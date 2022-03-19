package net.bridgepractice.bpbridge;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.lunarclient.bukkitapi.LunarClientAPI;
import net.bridgepractice.bpbridge.bridgemodifiers.BridgeModifier;
import net.bridgepractice.bpbridge.bridgemodifiers.PvpModifier;
import net.bridgepractice.bpbridge.bridgemodifiers.UnrankedModifier;
import net.bridgepractice.bpbridge.games.BridgeBase;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BPBridge extends JavaPlugin implements Listener, PluginMessageListener {
    public static BPBridge instance;
    SlimePlugin plugin;
    SlimeLoader slimeLoader;
    SlimePropertyMap slimeProperties;
    HashMap<String, JoiningPlayer> joiningPlayers = new HashMap<>();
    HashMap<String, Game> gamesByWorld = new HashMap<>();
    HashMap<UUID, Long> lastRequests = new HashMap<>();
    public static String discordWebhook = "https://discord.com/api/webhooks/879108049489514506/tpuJCqR_TbUn1tzUyFGTU7OBdUFl4oYqyQ4AYcL__X7MsMhke5dr0xwCPOF1nNxx-Z5u";
    LunarClientAPI lunarClientAPI;
    public static LuckPerms luckPerms;

    // db related things
    String host = "localhost";
    String port = "3306";
    String database = "bridge";
    String username = "mc";
    String password = "mcserver";
    // should this be public? need to access it in `.games` and we can't with any other access modifier.
    public static Connection connection;
    @Override
    public void onEnable() {
        instance = this;

        luckPerms = LuckPermsProvider.get();

        try {
            openConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "bp:messages", this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        plugin = (SlimePlugin) getServer().getPluginManager().getPlugin("SlimeWorldManager");
        slimeLoader = plugin.getLoader("file");
        slimeProperties = new SlimePropertyMap();
        slimeProperties.setBoolean(SlimeProperties.ALLOW_ANIMALS, false);
        slimeProperties.setBoolean(SlimeProperties.ALLOW_MONSTERS, false);
        slimeProperties.setBoolean(SlimeProperties.PVP, true);
        slimeProperties.setString(SlimeProperties.DIFFICULTY, "peaceful");

        getCommand("map").setExecutor(new MapCommand());

        Utils.loadCages();

        // Right now, we don't do much with the LC API. In the future, we should
        // add a level head implementation for ranked players!
        lunarClientAPI = LunarClientAPI.getInstance();
    }
    @Override
    public void onDisable() {
    }

    // DB Related Methods
    public void openConnection() throws SQLException {
        if(connection != null && !connection.isClosed()) {
            return;
        }
        // NOTE: If something around this are fails, something is different between my host machine and the machine
        //       this is running on. Getting rid of the `characterEncoding` query parameter may help, but other
        //       solutions are likely.
        connection = DriverManager.getConnection("jdbc:mysql://"
                        + this.host + ":" + this.port + "/" + this.database + "?characterEncoding=latin1&autoReconnect=true",
                this.username, this.password);
    }

    interface WorldCallback {
        void run(World world);
    }
    public void loadWorld(String worldName, WorldCallback callback) {

        (new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    SlimeWorld world = plugin.loadWorld(slimeLoader, worldName, true, slimeProperties);

                    /*
                        This is some certified reflection magic:
                        1. We get a CraftSlimeWorld from the slime world by just casting (CraftSlimeWorld implements SlimeWorld)
                        2. We make the `name` field accessible
                        3. We set the `name` field to whatever we want

                        The point of this is to prevent naming conflicts.
                    */
                    String fullWorldName = world.getName() + "-" + RandomStringUtils.randomAlphanumeric(8);
                    Class<?> craftSlimeWorld = world.getClass();
                    Field name = craftSlimeWorld.getDeclaredField("name");
                    name.setAccessible(true);
                    name.set(world, fullWorldName);

                    worldCallbackByWorldName.put(fullWorldName, callback);
                    // this function is run asynchronously and cannot take a callback as an argument.
                    // so we have to listen to the WorldLoadEvent. To accomplish the nice API of accepting
                    // a callback to this function, we store that callback in a hashmap and retrieve it
                    // on load. This will cause a memory leak if loading the world fails, so lets hope
                    // it doesn't fail :^)
                    plugin.generateWorld(world);
                } catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException | WorldInUseException | NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    Utils.sendDebugErrorWebhook("Error loading world:\n"+e);
                }
            }
        }).runTaskAsynchronously(this);
    }
    private final HashMap<String, WorldCallback> worldCallbackByWorldName = new HashMap<>();
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        WorldCallback callback = worldCallbackByWorldName.get(world.getName());
        if(callback != null) {
            worldCallbackByWorldName.remove(world.getName());
            callback.run(getServer().getWorld(world.getName()));
        }
    }
    public void unloadWorld(String worldName) {
        gamesByWorld.remove(worldName);
        (new BukkitRunnable() {
            @Override
            public void run() {
                boolean unloaded = getServer().unloadWorld(worldName, false);
                if(!unloaded) {
                    World world = getServer().getWorld(worldName);
                    Utils.sendDebugErrorWebhook("Could not unload world `" + worldName + "`!" +
                            (world == null
                                    ? "\nNo world with that name exists!"
                                    : "\ncurplayers="+world.getPlayers()) +
                            Utils.getGameDebugInfo(worldName));
                }
            }
        }).runTaskLater(this, 3*20);
    }

    private void handlePlayerJoiningGame(Player player, JoiningPlayer joiningPlayer) {
        World joiningPlayerWorld = joiningPlayer.getWorld();
        if(joiningPlayerWorld == null) {
            // force remove
            removeFromQueueable(joiningPlayer.getWorldName(), "unranked");
            removeFromQueueable(joiningPlayer.getWorldName(), "pvp");
            (new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage("§c§lUh oh! §cSomething went wrong sending you to the server (Attempted to queue nonexistent world). If this continues, please open a ticket on the Discord!");
                    connectPlayerToLobby(player);
                }
            }).runTaskLater(this, 5);
            return;
        }
        Game game = gamesByWorld.get(joiningPlayerWorld.getName());
        if(game == null) {
            player.sendMessage("§c§lUh oh! §cSomething went wrong sending you to the server. If this continues, please open a ticket on the Discord!");
            (new BukkitRunnable() {
                @Override
                public void run() {
                    connectPlayerToLobby(player);
                }
            }).runTaskLater(this, 5);
        } else {
            game.onPlayerJoin(player);
        }
    }
    private void handlePlayerJoiningPrivateGame(Player player, JoiningPlayer joiningPlayer) {
        World joiningPlayerWorld = joiningPlayer.getWorld();
        if(joiningPlayerWorld == null) {
            (new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage("§c§lUh oh! §cSomething went wrong sending you to the server (Attempted to queue nonexistent world). If this continues, please open a ticket on the Discord!");
                    connectPlayerToLobby(player);
                }
            }).runTaskLater(this, 5);
            return;
        }
        Game game = gamesByWorld.get(joiningPlayerWorld.getName());
        if(game == null) {
            // create the game
            // TODO: We only ever create a BridgeBase but we should probably do more than that
            gamesByWorld.put(joiningPlayer.getWorldName(), new BridgeBase(getServer().getWorld(joiningPlayer.getWorldName()), joiningPlayer.getMapName(), false, player, getModifier(joiningPlayer.getGameType())));
        } else {
            game.onPlayerJoin(player);
        }
    }

    public static void connectPlayerToLobby(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF("lobby");
        player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
    }

    public Game gameOfPlayer(Player player) {
        Game game = gamesByWorld.get(player.getWorld().getName());
        if(game != null && game.isPlayerInGame(player)) {
            return game;
        }
        return null;
    }

    public static void lagbackIfOnTop(Player player, BlockEvent event) {
        // -------  ANTI-CHEAT: Lagback if the player attempts to jump on ghost blocks
        Vector oldVel = player.getVelocity();
        oldVel.setY(0);
        if(player.getLocation().getBlock().getLocation().equals(event.getBlock().getLocation().add(0, 1, 0))) {
            player.teleport(player.getLocation().subtract(0, 0.4, 0));
            player.setVelocity(oldVel);
        }
        // -------
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("");
        Player player = event.getPlayer();
        if(player.hasPermission("group.admin")) {
            lunarClientAPI.giveAllStaffModules(player);
        }

        // reset to new player
        player.getInventory().setHeldItemSlot(0);
        player.getInventory().clear();
        player.getInventory().setBoots(new ItemStack(Material.AIR));
        player.getInventory().setLeggings(new ItemStack(Material.AIR));
        player.getInventory().setChestplate(new ItemStack(Material.AIR));
        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        player.setExp(0);
        player.setLevel(0);
        player.setHealth(20);
        player.setAllowFlight(false);
        // clear potion effects
        for(PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGameMode(GameMode.ADVENTURE);

        // try to send the plugin messages that we weren't able to send because nobody was online
        // we can't do it immediately so we put it in this runnable
        (new BukkitRunnable() {
            @Override
            public void run() {
                // it is possible that they logged off since joining, lets make sure they haven't
                if(player.isOnline()) {
                    while(!pluginMessagesToSend.isEmpty()) {
                        player.sendPluginMessage(instance, "BungeeCord", pluginMessagesToSend.remove());
                    }
                }
            }
        }).runTaskLater(this, 10);

        JoiningPlayer joiningPlayer = joiningPlayers.get(player.getName());
        if(joiningPlayer != null) {
            if(joiningPlayer.isJoiningPrivateGame()) {
                handlePlayerJoiningPrivateGame(player, joiningPlayer);
            } else if(joiningPlayer.isJoiningGame()) {
                handlePlayerJoiningGame(player, joiningPlayer);
            } else {
                player.teleport(Maps.getRedSpawnLoc(joiningPlayer.getMapName(), joiningPlayer.getWorld()));
                createQueue(player, joiningPlayer.getMapName(), joiningPlayer.getGameType());
            }
            joiningPlayers.remove(player.getName());
        } else {
            // loading
            player.teleport(new Location(getServer().getWorld("world2"), 0.5, 99, 0.5));
            player.sendMessage("§cLoading your game...");

            // check if something is wrong
            (new BukkitRunnable() {
                @Override
                public void run() {
                    if(player.getWorld().getName().equals("world2")) {
                        player.sendMessage("§c§lUh oh! §cSomething went wrong figuring out what game for you to join. If this continues, please open a ticket on the Discord!");
                        connectPlayerToLobby(player);
                    }
                }
            }).runTaskLater(this, 3 * 20);
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Game game = gameOfPlayer(player);
        if(game != null) {
            game.onPlayerLeave(player);
        }
        event.setQuitMessage("");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Game game = gameOfPlayer(player);
        if(game != null) {
            game.onPlayerMove(event, player);
        }
    }

    @EventHandler
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if(item.getType() == Material.GOLDEN_APPLE) {
            // do the custom gapple logic for bridge
            player.setHealth(20);
            (new BukkitRunnable() {
                @Override
                public void run() {
                    player.removePotionEffect(PotionEffectType.ABSORPTION);
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60 * 20, 0, false, true));
                    gameOfPlayer(player).onPlayerHealthChange(player);
                }
            }).runTask(this);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }
    @EventHandler
    public void onPlayerDamagedBySelf(EntityDamageEvent event) {
        // disable drown, fall, etc damage
        EntityDamageEvent.DamageCause cause = event.getCause();
        if(event.getEntity() instanceof Player) {
            switch(cause) {
                case SUFFOCATION:
                case FALL:
                case DROWNING:
                    event.setCancelled(true);
                    break;
                case VOID:
                    Player player = (Player) event.getEntity();
                    if(player.getHealth() - event.getFinalDamage() < 0) {
                        // just in case
                        event.setCancelled(true);
                        Utils.sendDebugErrorWebhook("Player "+player.getName()+" died to the void while playing a game!"+Utils.getGameDebugInfo(player.getWorld().getName()));
                        player.sendMessage("§c§lUh oh! §cSomething went wrong in that server. If this continues, please open a ticket on the Discord!");
                        connectPlayerToLobby(player);
                    }
                    break;
            }
        }
    }
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(event.toWeatherState());
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if(event.getMaterial() == Material.DIAMOND) {
            Game game = gameOfPlayer(player);
            assert game != null;
            game.onPlayerGlyph(player);
        } else if(event.getMaterial() == Material.BED) {
            if(System.currentTimeMillis() - lastRequests.getOrDefault(player.getUniqueId(), 0L) > 500) {
                lastRequests.put(player.getUniqueId(), System.currentTimeMillis());
                player.sendMessage("§cSending you to the lobby...");
                connectPlayerToLobby(player);
            }
        } else if(event.getMaterial() == Material.PAPER) {
            if(System.currentTimeMillis() - lastRequests.getOrDefault(player.getUniqueId(), 0L) > 500) {
                lastRequests.put(player.getUniqueId(), System.currentTimeMillis());
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("RequestGame");
                Game game = gameOfPlayer(player);
                if(game == null) {
                    player.sendMessage("§cSomething went wrong queueing another game!");
                    return;
                }
                out.writeUTF(game.getGameType());
                player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
            }
        } else if(event.getMaterial() == Material.STAINED_CLAY && event.getAction() == Action.RIGHT_CLICK_BLOCK && player.getGameMode() == GameMode.ADVENTURE) {
            // emulate hypixel behavior (this behavior is quite odd, but it was suggested)
            player.sendMessage("§cYou can't place blocks there!");
            player.playSound(event.getClickedBlock().getLocation(), Sound.DIG_STONE, 1, 0.8f);
        } else if(event.getMaterial() == Material.BOW && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            Game game = gameOfPlayer(player);
            game.onPlayerBowCharge(event, player);
        }
    }
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if(!(event.getEntity() instanceof Arrow))
            return;

        // delete arrows when they hit anything
        event.getEntity().remove();
    }
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if(!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        Game game = gameOfPlayer(player);
        if(game == null) {
            return;
        }
        game.rechargeArrow(player);
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage("");
        Player player = event.getEntity();
        Utils.sendDebugErrorWebhook("Player "+player.getName()+" died while playing a game!"+Utils.getGameDebugInfo(player.getWorld().getName()));
        player.sendMessage("§cError: You died while in a multiplayer game!\n§eThis issue has been automatically reported, and you can ignore this message.");
        connectPlayerToLobby(player);
    }
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player player = (Player) event.getEntity();
        Game game = gameOfPlayer(player);
        if(game == null) return;
        if(!game.isPlaying()) {
            event.setCancelled(true);
            return;
        }
        if(game.canPlayerTakeDamage(player)) {
            event.setCancelled(true);
            return;
        }

        Player damager;
        if(event.getDamager() instanceof Projectile) {
            damager = ((Player) ((Projectile) event.getDamager()).getShooter());
        } else {
            damager = ((Player) event.getDamager());
        }

        if(player.equals(damager)) {
            event.setCancelled(true);
            return;
        }

        if(event.getDamager() instanceof Projectile) {
            // play successful hit sfx
            damager.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 1f, 0.5f);
        }

        game.onPlayerHitByPlayer(player, damager, event.getFinalDamage());
        if(player.getHealth() - event.getFinalDamage() < 0) {
            event.setCancelled(true);
            player.setVelocity(new Vector());
            game.onPlayerDeath(player);
            Utils.sendActionBar(damager, player.getCustomName() + " §0" + Utils.hearts(10), 1);
        } else {
            int damageDealt = (int) Math.round(event.getFinalDamage()) / 2;
            int heartsLeftNum = ((int) ((player.getHealth() - event.getFinalDamage()) / 2));
            String heartsLeft = Utils.hearts(heartsLeftNum);
            String dealt = Utils.hearts(damageDealt);
            Utils.sendActionBar(damager, player.getCustomName() + " §4" + heartsLeft + "§c" + dealt + "§0" + Utils.hearts(10 - (heartsLeftNum + damageDealt)), 1);
        }


        (new BukkitRunnable() {
            @Override
            public void run() {
                game.onPlayerHealthChange(player);
            }
        }).runTask(this);
    }
    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        // disable natural regen
        if(event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        // disable hunger
        event.setCancelled(true);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == null) return;
        // no getting rid of armor!
        if(event.getSlot() >= event.getClickedInventory().getSize()) {
            event.setCancelled(true);
        }
        // no hotkeying paper or bed!
        if(event.getCurrentItem().getType() == Material.BED || event.getCurrentItem().getType() == Material.PAPER) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Game game = gameOfPlayer(player);
        if(game == null) return;
        event.setCancelled(true);
        if(game.isQueueing()) {
            player.sendMessage("§cYou cannot chat before the game has started!");
            return;
        }

        game.onPlayerChat(player, event.getMessage());
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = gameOfPlayer(player);
        if(game != null) {
            if(game.cannotPlaceBlocks(event.getBlockPlaced().getLocation(), player)) {
                player.sendMessage("§cYou can't place blocks there!");
                lagbackIfOnTop(player, event);
                event.setCancelled(true);
                return;
            }
            game.onPlayerBlockPlace(event, player);
        }

    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = gameOfPlayer(player);
        if(game == null) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        Location loc = block.getLocation();

        if(game.cannotBreakBlock(block, loc, player)) {
            player.sendMessage("§cYou can't break that block!");
            event.setCancelled(true);
        }
    }

    private final String[] mapNames = {"aquatica", "ashgate", "atlantis", "boo", "cheesy", "chronon", "condo", "dojo", "flora", "fortress2", "galaxy", "hyperfrost", "licorice", "lighthouse2", "outpost", "palaestra", "sorcery", "stumped", "sunstone", "treehouse", "tundra2", "twilight", "urban"};
    private final String[] pvpMapNames = {"developedatlantis", "developedgalaxy", "developedsorcery", "developedstumped"};
    private String getRandomUnrankedMap() {
        return mapNames[ThreadLocalRandom.current().nextInt(mapNames.length)];
    }
    private String getRandomPvpMap() {
        return pvpMapNames[ThreadLocalRandom.current().nextInt(pvpMapNames.length)];
    }

    public void createQueue(Player player, String map, String gameType) {
        (new BukkitRunnable() {
            @Override
            public void run() {
                World world = player.getWorld();
                sendCreateQueuePluginMessage(player, gameType);
                // TODO: We only ever create a BridgeBase but we should be creating other Game types
                gamesByWorld.put(world.getName(), new BridgeBase(world, map, true, player, getModifier(gameType)));
            }
        }).runTaskLater(this, 3); // we need to delay since the player has just joined
    }
    private BridgeModifier getModifier(String gameType) {
        BridgeModifier modifier = null;
        if(gameType.equals("unranked")) {
            modifier = new UnrankedModifier();
        } else if(gameType.equals("pvp")) {
            modifier = new PvpModifier();
        }
        assert modifier != null;
        return modifier;
    }
    public void sendCreateQueuePluginMessage(Player player, String gameType) {
        World world = player.getWorld();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("SetGameQueueing");
        out.writeUTF(gameType);
        out.writeUTF(world.getName());
        player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
    }
    Queue<byte[]> pluginMessagesToSend = new PriorityQueue<>();
    public void removeFromQueueable(String worldName, String gameMode) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RemoveGameQueueing");
        out.writeUTF(gameMode);
        out.writeUTF(worldName);
        byte[] bytes = out.toByteArray();
        // if at least 1 player is online
        if(getServer().getOnlinePlayers().size() >= 1) {
            // we can send the plugin message now
            Player sender = Objects.requireNonNull(Iterables.getFirst(getServer().getOnlinePlayers(), null));
            sender.sendPluginMessage(this, "BungeeCord", bytes);
        } else {
            // otherwise we have to add to queue, and we will send when any player logs on
            pluginMessagesToSend.add(bytes);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player someOnlinePlayer, byte[] message) {
        if(channel.equals("bp:messages")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            switch(subchannel) {
                case "StartGame": {
                    String gameType = in.readUTF();
                    String playerName = in.readUTF();
                    String map;
                    if(gameType.equals("unranked")) {
                        map = getRandomUnrankedMap();
                    } else if(gameType.equals("pvp")) {
                        map = getRandomPvpMap();
                    } else {
                        map = "urban";
                    }
                    loadWorld(map, (world) -> {
                        Player onlinePlayer = getServer().getPlayerExact(playerName);
                        Location loc = Maps.getRedSpawnLoc(map, world);
                        if(onlinePlayer != null) {
                            // check if they are currently in a game - if so, make them leave (since the leaving is
                            // usually only called when the player actually leaves the server and they dont if queueing
                            // a new game while playing)
                            Game game = gameOfPlayer(onlinePlayer);
                            if(game != null) {
                                game.onPlayerLeave(onlinePlayer);
                            }
                            onlinePlayer.teleport(loc);
                            createQueue(onlinePlayer, map, gameType);
                        } else {
                            // this is the case where there are players online so we can warp them in after the game
                            // was created
                            ByteArrayDataOutput out = ByteStreams.newDataOutput();
                            out.writeUTF("ConnectOther");
                            out.writeUTF(playerName);
                            out.writeUTF("multiplayer_1");
                            someOnlinePlayer.sendPluginMessage(this, "BungeeCord", out.toByteArray());

                            joiningPlayers.put(playerName, new JoiningPlayer(map, world, gameType));
                            (new BukkitRunnable() {
                                @Override
                                public void run() {
                                    joiningPlayers.remove(playerName);
                                }
                            }).runTaskLaterAsynchronously(this, 5 * 20);
                        }
                    });
                    break;
                }
                case "IntentToJoinGame": {
                    String worldName = in.readUTF();
                    String playerName = in.readUTF();

                    Player onlinePlayer = getServer().getPlayerExact(playerName);
                    if(onlinePlayer != null) {
                        // check if they are currently in a game - if so, make them leave (since the leaving is
                        // usually only called when the player actually leaves the server and they dont if queueing
                        // a new game while playing)
                        Game game = gameOfPlayer(onlinePlayer);
                        if(game != null) {
                            game.onPlayerLeave(onlinePlayer);
                        }

                        handlePlayerJoiningGame(onlinePlayer, new JoiningPlayer(worldName));
                    } else {
                        joiningPlayers.put(playerName, new JoiningPlayer(worldName));
                    }
                    break;
                }
                case "StartPrivateGame": {
                    String gameType = in.readUTF();
                    String serverMap = in.readUTF();
                    int players = in.readInt();
                    String[] playerNames = new String[players];
                    for(int i = 0; i < playerNames.length; i++) {
                        playerNames[i] = in.readUTF();
                    }
                    String map;
                    if(serverMap.length() > 0) {
                        map = serverMap;
                    } else if(gameType.equals("unranked")) {
                        map = getRandomUnrankedMap();
                    } else if(gameType.equals("pvp")) {
                        map = getRandomPvpMap();
                    } else {
                        map = "urban";
                    }
                    loadWorld(map, (world) -> {
                        Location loc = Maps.getRedSpawnLoc(map, world);
                        boolean hasCreatedGame = false;
                        for(String playerName : playerNames) {
                            Player onlinePlayer = getServer().getPlayerExact(playerName);
                            if(onlinePlayer != null) {
                                // check if they are currently in a game - if so, make them leave (since the leaving is
                                // usually only called when the player actually leaves the server and they dont if queueing
                                // a new game while playing)
                                Game game = gameOfPlayer(onlinePlayer);
                                if(game != null) {
                                    game.onPlayerLeave(onlinePlayer);
                                }
                                onlinePlayer.teleport(loc);
                                if(!hasCreatedGame) {
                                    hasCreatedGame = true;
                                    // TODO: we are creating a BridgeBase but we really should be making other Games too
                                    gamesByWorld.put(world.getName(), new BridgeBase(world, map, false, onlinePlayer, getModifier(gameType)));
                                } else {
                                    gamesByWorld.get(world.getName()).onPlayerJoin(onlinePlayer);
                                }
                            } else {
                                // this is the case where there are players online so we can warp them in after the game
                                // was created
                                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                out.writeUTF("ConnectOther");
                                out.writeUTF(playerName);
                                out.writeUTF("multiplayer_1");
                                someOnlinePlayer.sendPluginMessage(this, "BungeeCord", out.toByteArray());

                                joiningPlayers.put(playerName, JoiningPlayer.newWithPrivate(map, world, gameType));
                                (new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        joiningPlayers.remove(playerName);
                                    }
                                }).runTaskLaterAsynchronously(this, 5 * 20);
                            }
                        }

                    });
                    break;
                }
            }
        }
    }
}
