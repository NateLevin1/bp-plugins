package net.bridgepractice.bridgepracticeclub;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.NotNull;
import net.bridgepractice.RavenAntiCheat.RavenAntiCheat;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.*;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class Bridge extends JavaPlugin implements Listener, PluginMessageListener {
    public static Bridge instance;
    private final HashMap<UUID, PlayerInfo> playerInfos = new HashMap<>();
    private final HashMap<UUID, Long> playerNpcTimes = new HashMap<>();
    public HashMap<UUID, ArrowRegenerate> playerArrowRegenerations = new HashMap<>();
    public World world;
    public ScoreboardManager sm;
    public final ArrayList<EntityPlayer> npcs = new ArrayList<>();
    private final HashMap<Integer, String> entityInteractChat = new HashMap<>();
    private final HashMap<UUID, Long> playerLastMove = new HashMap<>();
    private final HashMap<UUID, InvItem[]> cachedInventories = new HashMap<>();
    public static MinecraftServer nmsServer;
    public static WorldServer nmsWorld;
    public static HashMap<String, Boolean> disabledGames = new HashMap<>();

    public static final boolean debug = false;

    // db related things
    String host = "localhost";
    String port = "3306";
    String database = "bridge";
    String username = "mc";
    String password = "mcserver";
    static Connection connection;

    @Override
    public void onEnable() {
        instance = this;

        try {
            openConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "bp:messages", this);
        world = this.getServer().getWorld("wing");

        (new BukkitRunnable() {
            @Override
            public void run() {
                for(int i = 0; i < NPC.names.length; i++) {
                    NPC.skins[i] = Bridge.instance.getSkin(NPC.names[i]);
                }
                Bridge.instance.getLogger().info("Finished getting NPC skins");
            }
        }).runTaskAsynchronously(this);

        // packet stuff
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                int entityId = packet.getIntegers().read(0);
                EnumWrappers.EntityUseAction type = packet.getEntityUseActions().read(0);
                String chat = entityInteractChat.get(entityId);
                if(chat != null && player.getItemInHand().getType() != Material.BED) {
                    long lastPressTime = playerNpcTimes.getOrDefault(player.getUniqueId(), 0L);
                    playerNpcTimes.put(player.getUniqueId(), System.currentTimeMillis());
                    if(System.currentTimeMillis() - lastPressTime > 500) {
                        BukkitRunnable send = new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.chat(chat);
                            }
                        };
                        send.runTask(Bridge.instance);
                    } // otherwise we just ignore it
                } else if(type == EnumWrappers.EntityUseAction.ATTACK) { // left click
                    PlayerInfo info = getPlayer(player.getUniqueId());
                    if(entityId == info.locSettings.npcId) {
                        (new BukkitRunnable() {
                            @Override
                            public void run() {
                                info.locSettings.onNpcHit.call(info);
                            }
                        }).runTask(Bridge.instance);
                    }
                }
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.ENTITY_ACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                // sprinting
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                EnumWrappers.PlayerAction action = packet.getPlayerActions().read(0);
                PlayerInfo info = getPlayer(player.getUniqueId());
                if(info == null) return;
                if(action == EnumWrappers.PlayerAction.START_SPRINTING) {
                    info.locSettings.isSprintingHit = true;
                } else if(action == EnumWrappers.PlayerAction.STOP_SPRINTING) {
                    info.locSettings.isSprintingHit = false;
                }
            }
        });

        this.getCommand("wing").setExecutor(new CommandWing());
        this.getCommand("whereami").setExecutor(new CommandWhereami());
        this.getCommand("prebow").setExecutor(new CommandPrebow());
        this.getCommand("bypass").setExecutor(new CommandBypass());
        this.getCommand("bot").setExecutor(new CommandBridgeBot());
        this.getCommand("whereis").setExecutor(new CommandWhereIs());
        this.getCommand("rainbow").setExecutor(new RainbowCommand());
        this.getCommand("clutch").setExecutor(new CommandClutch());

        CommandSpawn spawnCommand = new CommandSpawn();
        this.getCommand("spawn").setExecutor(spawnCommand);
        this.getCommand("l").setExecutor(spawnCommand);
        this.getCommand("lobby").setExecutor(spawnCommand);
        this.getCommand("leave").setExecutor(spawnCommand);
        this.getCommand("hub").setExecutor(spawnCommand);

        this.getCommand("rin").setExecutor((sender, command, label, args) -> {
            if(sender.hasPermission("bridgepractice.broadcast")) {
                String minutes = "5";
                if(args.length != 0) {
                    minutes = args[0];
                }
                getServer().broadcastMessage("\n§6§lHey! §aThe server will restart to update in §a§l" + minutes + " minute" + (minutes.equals("1") ? "" : "s") + "§a.");
                getServer().broadcastMessage("");
                for(Player player : getServer().getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1, 1);
                    player.playSound(player.getLocation(), Sound.BLAZE_BREATH, 1, 1f);
                    sendTitle(player, "§aServer restart in §a§l" + minutes + " minute" + (minutes.equals("1") ? "" : "s") + "§a.", "Check the Discord for update information", 5, 10 * 20, 5);
                }
            } else {
                sender.sendMessage("§cYou do not have permission to use this command");
            }
            return true;
        });

        this.getCommand("dq").setExecutor((sender, command, label, args) -> {
            if(sender.hasPermission("bridgepractice.broadcast")) {
                // disable queue
                if(args.length == 0) {
                    sender.sendMessage("§cUsage: /dq game");
                    return true;
                }
                disabledGames.put(args[0], true);
            } else {
                sender.sendMessage("§cYou do not have permission to use this command");
            }
            return true;
        });

        this.getCommand("eq").setExecutor((sender, command, label, args) -> {
            if(sender.hasPermission("bridgepractice.broadcast")) {
                // enable queue
                if(args.length == 0) {
                    sender.sendMessage("§cUsage: /eq game");
                    return true;
                }
                boolean found = disabledGames.remove(args[0]);
                if(!found) {
                    sender.sendMessage("§cUnknown game '" + args[0] + "'");
                }
            } else {
                sender.sendMessage("§cYou do not have permission to use this command");
            }
            return true;
        });

        CommandLeavequeue commandLeavequeue = new CommandLeavequeue();
        this.getCommand("leavequeue").setExecutor(commandLeavequeue);
        this.getCommand("leaveq").setExecutor(commandLeavequeue);
        this.getCommand("lq").setExecutor(commandLeavequeue);

        for(Player player : this.getServer().getOnlinePlayers()) {
            player.sendMessage("§l§6Plugin restarted while you were online! Sending you to spawn...");
            player.chat("/spawn");
        }

        WeatherFix wf = new WeatherFix();
        wf.runTaskTimer(this, 0, 20);

        sm = this.getServer().getScoreboardManager();

        // check for afk players
        BukkitRunnable checkForAfkPlayers = new BukkitRunnable() {
            @Override
            public void run() {
                for(Map.Entry<UUID, Long> lastMove : playerLastMove.entrySet()) {
                    long timeSince = System.currentTimeMillis() - lastMove.getValue();
                    if(timeSince > 100 * 1000) {
                        if(getPlayer(lastMove.getKey()).location != PlayerLocation.Spawn) {
                            Player player = getServer().getPlayer(lastMove.getKey());
                            if(player == null) continue;
                            if(timeSince > 120 * 1000) {
                                player.chat("/spawn"); // rip them
                            } else {
                                player.sendMessage("§c§lWARNING: You will be AFK kicked to spawn if you don't move in the next few seconds!");
                                (new BukkitRunnable() {
                                    int runs = 0;
                                    @Override
                                    public void run() {
                                        runs++;
                                        if(runs > 9) this.cancel();
                                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 1);
                                    }
                                }).runTaskTimer(instance, 1, 2);
                            }
                        }
                    }

                }
            }
        };
        checkForAfkPlayers.runTaskTimer(this, 0, 10 * 20);

        nmsServer = ((CraftServer) getServer()).getServer();
        nmsWorld = ((CraftWorld) this.world).getHandle();
        createNPCForAll(new Location(this.world, -7.5, 99.5, 7.5, -90, 0), "Prebow Practice", "cruh", "/prebow");
        createNPCForAll(new Location(this.world, -8.5, 100, 4.5, -90, 0), "Bypass Practice", "cheetahh", "/bypass");
        createNPCForAll(new Location(this.world, -9.5, 100, 0.5, -90, 0), "Clutch Practice", "BuckyBarrTV", "/clutch");
        createNPCForAll(new Location(this.world, -8.5, 100, -3.5, -90, 0), "Bot Practice", "parihs", "/bot");
        createNPCForAll(new Location(this.world, -7.5, 99.5, -6.5, -90, 0), "Wing Practice", "fozzie1000", "/wing");

        // load content arrays
        CommandWing.islandContentDefault = getBlocks(new Location(Bridge.instance.world, 1000, 97, -5), 4, 3, 4);
        CommandWing.islandContentMagma = getBlocks(new Location(Bridge.instance.world, 1000, 107, -5), 4, 3, 4);
        CommandWing.islandContentPalace = getBlocks(new Location(Bridge.instance.world, 1000, 117, -5), 4, 3, 4);
        CommandWing.islandContentModern = getBlocks(new Location(Bridge.instance.world, 1000, 127, -5), 4, 3, 4);
        CommandWing.islandContentAquatic = getBlocks(new Location(Bridge.instance.world, 1000, 137, -5), 4, 3, 4);
        CommandWing.islandContentNightLight = getBlocks(new Location(Bridge.instance.world, 1000, 147, -5), 4, 3, 4);
        CommandWing.islandContentSeptic = getBlocks(new Location(Bridge.instance.world, 1000, 157, -5), 4, 3, 4);

        CommandWing.landingContentDefault = getBlocks(new Location(Bridge.instance.world, 989, 94, -11), 7, 6, 10);
        CommandWing.landingContentMagma = getBlocks(new Location(Bridge.instance.world, 989, 104, -11), 7, 6, 10);
        CommandWing.landingContentPalace = getBlocks(new Location(Bridge.instance.world, 989, 114, -11), 7, 6, 10);
        CommandWing.landingContentAquatic = getBlocks(new Location(Bridge.instance.world, 989, 124, -11), 7, 6, 10);
        CommandWing.landingContentModern = getBlocks(new Location(Bridge.instance.world, 989, 134, -11), 7, 6, 10);
        CommandWing.landingContentNightLight = getBlocks(new Location(Bridge.instance.world, 989, 144, -11), 7, 6, 10);
        CommandWing.landingContentSeptic = getBlocks(new Location(Bridge.instance.world, 989, 154, -11), 7, 6, 10);

        CommandPrebow.targetContent = getBlocks(new Location(Bridge.instance.world, 1012, 93, -4), 9, 6, 7);
        CommandPrebow.mushroomContent = getBlocks(new Location(Bridge.instance.world, 1012, 93, -24), 9, 6, 7);
        CommandPrebow.flowerContent = getBlocks(new Location(Bridge.instance.world, 1036, 91, -24), 9, 8, 7);
        CommandPrebow.sailboatContent = getBlocks(new Location(Bridge.instance.world, 1055, 94, -24), 9, 6, 7);
        CommandBypass.earlyContents = new BlockState[][][][] {getBlocks(new Location(Bridge.instance.world, 1082, 87, -24), 4, 10, 37), getBlocks(new Location(Bridge.instance.world, 1082, 87, 23), 4, 10, 37)};
        CommandBypass.middleContents = new BlockState[][][][] {getBlocks(new Location(Bridge.instance.world, 1092, 84, -24), 7, 16, 40), getBlocks(new Location(Bridge.instance.world, 1092, 84, 22), 7, 16, 40)};
        CommandBypass.lateContents = new BlockState[][][][] {getBlocks(new Location(Bridge.instance.world, 1110, 84, -24), 16, 16, 40), getBlocks(new Location(Bridge.instance.world, 1110, 84, 22), 16, 16, 40)};
        CommandBridgeBot.bridgeContent = getBlocks(new Location(Bridge.instance.world, 1138, 84, -23), 1, 9, 41);
        CommandBridgeBot.cageContent = getBlocks(new Location(Bridge.instance.world, 1012, 93, -24), 9, 6, 7);
        CommandBridgeBot.npcCageContent = getBlocks(new Location(Bridge.instance.world, 1012, 102, -24), 9, 6, 7);
        CommandClutch.spawnContent = getBlocks(new Location(Bridge.instance.world, 963, 95, -17), 9, 8, 14);
        CommandClutch.spawnContent2 = getBlocks(new Location(Bridge.instance.world, 963, 105, -17), 9, 8, 14);
        CommandClutch.bridgeContent = getBlocks(new Location(Bridge.instance.world, 967, 102, 0), 1, 7, 42);
        CommandClutch.bridgeDevelopedContent = getBlocks(new Location(Bridge.instance.world, 967-10, 102, 0), 1, 7, 42);
        CommandClutch.bridgeBypassContent = getBlocks(new Location(Bridge.instance.world, 967-21, 102, 0), 3, 7, 42);

        PlayerInfo.queues.put(PlayerLocation.Bypass, new ArrayList<>());
        PlayerInfo.queues.put(PlayerLocation.BridgeBot, new ArrayList<>());
        PlayerInfo.queues.put(PlayerLocation.Prebow, new ArrayList<>());
        PlayerInfo.queues.put(PlayerLocation.Wing, new ArrayList<>());
    }
    @Override
    public void onDisable() {
        // Fired when the server stops and disables all plugins
        for(Player player : this.getServer().getOnlinePlayers()) {
            PlayerInfo info = getPlayer(player.getUniqueId());
            if(info.onLocationChange != null) {
                info.onLocationChange.call(info);
            }
        }

        instance = null;
        world = null;
        sm = null;

        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public PlayerInfo getPlayer(UUID id) {
        return playerInfos.get(id);
    }
    public Collection<PlayerInfo> getAllPlayerInfos() {
        return playerInfos.values();
    }
    public void setPlayer(UUID id, PlayerInfo info) {
        PlayerInfo oldInfo = getPlayer(id);
        if(oldInfo != null && oldInfo.onLocationChange != null) {
            oldInfo.onLocationChange.call(oldInfo);
        }
        playerInfos.put(id, info);
        if(oldInfo != null) {
            PlayerInfo.nextInQueue(oldInfo.location, getCommandFromPlayerLocation(oldInfo.location));
        }
    }
    public ArrayList<EntityPlayer> getAllNpcs() {
        return this.npcs;
    }
    public void createNPCForAll(Location loc, String name, String skinUsername, String chatOnInteract) {
        // see https://www.spigotmc.org/threads/how-to-create-and-modify-npcs.400753/
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name); // max 16 characters
        EntityPlayer npc = new EntityPlayer(nmsServer, nmsWorld, gameProfile, new PlayerInteractManager(nmsWorld)); // This will be the EntityPlayer (NPC) we send with the sendNPCPacket method.
        npc.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw() - 46, loc.getPitch());
        entityInteractChat.put(npc.getId(), chatOnInteract);
        npcs.add(npc);

        // add skin (should we do this more often than startup?)
        gameProfile.getProperties().put("textures", getSkin(skinUsername));
    }
    Property getSkin(String skinUsername) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format("https://api.ashcon.app/mojang/v2/user/%s", skinUsername)).openConnection();
            if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                ArrayList<String> lines = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                reader.lines().forEach(lines::add);

                String reply = String.join(" ", lines);
                int indexOfValue = reply.indexOf("\"value\": \"");
                int indexOfSignature = reply.indexOf("\"signature\": \"");
                String skin = reply.substring(indexOfValue + 10, reply.indexOf("\"", indexOfValue + 10));
                String signature = reply.substring(indexOfSignature + 14, reply.indexOf("\"", indexOfSignature + 14));

                return new Property("textures", skin, signature);
            } else {
                getLogger().severe("Error getting skin for "+skinUsername+":");
                getLogger().info("Connection could not be opened when fetching player skin (Response code " + connection.getResponseCode() + ", " + connection.getResponseMessage() + ")");
            }
        } catch (Exception e) {
            getLogger().severe("Error getting skin for "+skinUsername+":");
            e.printStackTrace();
        }
        return null;
    }

    WrappedSignedProperty getWrappedSkin(String skinUsername) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format("https://api.ashcon.app/mojang/v2/user/%s", skinUsername)).openConnection();
        if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            ArrayList<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            reader.lines().forEach(lines::add);

            String reply = String.join(" ", lines);
            int indexOfValue = reply.indexOf("\"value\": \"");
            int indexOfSignature = reply.indexOf("\"signature\": \"");
            String skin = reply.substring(indexOfValue + 10, reply.indexOf("\"", indexOfValue + 10));
            String signature = reply.substring(indexOfSignature + 14, reply.indexOf("\"", indexOfSignature + 14));

            return new WrappedSignedProperty("textures", skin, signature);
        } else {
            getLogger().info("Connection could not be opened when fetching player skin (Response code " + connection.getResponseCode() + ", " + connection.getResponseMessage() + ")");
        }
        return null;
    }

    public static void setScoreboard(Player player, Scoreboard scoreboard) {
        player.setScoreboard(scoreboard);
        net.bridgepractice.bpshowranksinnametag.Main.addAllPlayers(player);
    }

    public static String ordinal(int i) {
        int mod100 = i % 100;
        int mod10 = i % 10;
        if(mod10 == 1 && mod100 != 11) {
            return i + "st";
        } else if(mod10 == 2 && mod100 != 12) {
            return i + "nd";
        } else if(mod10 == 3 && mod100 != 13) {
            return i + "rd";
        } else {
            return i + "th";
        }
    }
    public static Scoreboard createScoreboard(String displayName, String[] scores) {
        Scoreboard board = instance.sm.getNewScoreboard();
        Objective objective = board.registerNewObjective("scoreboard", "dummy");
        objective.setDisplayName(displayName);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int numSpaces = 0;
        int numResets = 1;
        for(int i = 0; i < scores.length; i++) {
            if(scores[i].equals("")) {
                objective.getScore(String.join("", Collections.nCopies(numSpaces, " "))).setScore(scores.length - i);
                numSpaces++;
            } else if(scores[i].startsWith("%")) {
                int percent = scores[i].substring(1).indexOf('%') + 1;
                String teamName = scores[i].substring(1, percent);
                Team team = board.registerNewTeam(teamName);
                String entry = String.join("", Collections.nCopies(numResets, "§r"));
                team.addEntry(entry);
                String content = scores[i].substring(percent + 1);
                int split = content.indexOf("%");
                if(split == -1) {
                    team.setPrefix(content);
                } else {
                    team.setPrefix(content.substring(0, split));
                    team.setSuffix(content.substring(split + 1));
                }
                objective.getScore(entry).setScore(scores.length - i);
                numResets++;
            } else {
                objective.getScore(scores[i]).setScore(scores.length - i);
            }
        }

        return board;
    }
    public static void sendTitle(Player player, String titleText, String subtitleText, int fadeIn, int fadeOut, int duration) {
        IChatBaseComponent chatTitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + titleText + "\"}");
        IChatBaseComponent chatSubtitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitleText + "\"}");

        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, chatTitle);
        PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, chatSubtitle);
        PacketPlayOutTitle length = new PacketPlayOutTitle(fadeIn, duration, fadeOut);

        // For some reason if we don't send this length packet then the first time we try to send a title to a player
        // after they have disconnected the title does not show up.
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);

        // send the actual title
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(title);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(subtitle);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);
    }
    public static void sendTitle(Player player, String titleText, String subtitleText) {
        Bridge.sendTitle(player, titleText, subtitleText, 5, 5, 40);
    }
    public static String prettifyNumber(float num) {
        // this is a *horrible* solution but it works
        String s = String.valueOf(Math.ceil(num / 1000f * 8f) / 8f);
        return padWithZeroes(s);
    }
    public static String padWithZeroes(String s) {
        String[] dec = s.split("\\.");
        if(dec.length == 1) {
            return s + ".000";
        } else if(dec[1].length() == 1) {
            return s + "00";
        } else if(dec[1].length() == 2) {
            return s + "0";
        } else {
            return s;
        }
    }
    public static void setBridgeInventory(Player player, boolean settingsInsteadOfGlyph) { // provide settings as null for just a glyph
        InvItem[] hotbar = instance.cachedInventories.get(player.getUniqueId());

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                InvItem[] hotbar = instance.cachedInventories.get(player.getUniqueId());
                if(hotbar == null) {
                    player.sendMessage("§c§lUh oh!§r§c There was an error re-fetching your bridge hotbar! Please try going into this mode again, or open a ticket on the discord if the issue persists.");
                    return;
                }
                ItemStack sword = getUnbreakable(new ItemStack(Material.IRON_SWORD, 1));

                ItemStack blocks = new ItemStack(Material.STAINED_CLAY, 64, (byte) instance.getPlayer(player.getUniqueId()).locSettings.clayColor);

                List<String> gapple_lore = new ArrayList<>();
                gapple_lore.add("§7Instantly heals you to full");
                gapple_lore.add("§7health and grants §aAbsorption");
                gapple_lore.add("§aI§7.");
                ItemStack gapple = new ItemStack(Material.GOLDEN_APPLE, 8);
                ItemMeta gim = gapple.getItemMeta();
                gim.setLore(gapple_lore);
                gapple.setItemMeta(gim);

                ItemStack bow = getUnbreakable(new ItemStack(Material.BOW, 1));
                List<String> bow_lore = new ArrayList<>();
                bow_lore.add("§7Arrows regenerate every");
                bow_lore.add("§a3.5s§7. You can have a maximum");
                bow_lore.add("§7of §a1§7 arrow at a time.");
                bow_lore.add("");
                ItemMeta bim = bow.getItemMeta();
                bim.setDisplayName("§aBow");
                bim.setLore(bow_lore);
                bim.spigot().setUnbreakable(true);
                bow.setItemMeta(bim);

                ItemStack pick = getUnbreakable(new ItemStack(Material.DIAMOND_PICKAXE, 1));
                pick.addEnchantment(Enchantment.DIG_SPEED, 2);
                ItemMeta pim = pick.getItemMeta();
                pim.spigot().setUnbreakable(true);
                pick.setItemMeta(pim);

                ItemStack glyph;
                List<String> glyph_lore = new ArrayList<>();
                if(!settingsInsteadOfGlyph) {
                    glyph_lore.add("§7Best item in the game!");
                    glyph = new ItemStack(Material.DIAMOND, 1);
                    ItemMeta glyphim = glyph.getItemMeta();
                    glyphim.setLore(glyph_lore);
                    glyphim.setDisplayName("§6Glyph Menu");
                    glyph.setItemMeta(glyphim);
                } else {
                    glyph_lore.add("§7Right click to customize");
                    glyph_lore.add("§7the settings of this mode.");
                    glyph_lore.add("");
                    glyph = new ItemStack(Material.EMERALD, 1);
                    ItemMeta glyphim = glyph.getItemMeta();
                    glyphim.setLore(glyph_lore);
                    glyphim.setDisplayName("§6Settings Menu");
                    glyph.setItemMeta(glyphim);
                }

                for(InvItem item : hotbar) {
                    int i = item.index;
                    String name = item.item;
                    switch(name) {
                        case "iron_sword":
                            player.getInventory().setItem(i, sword);
                            break;
                        case "stained_clay_1":
                        case "stained_clay_2":
                            player.getInventory().setItem(i, blocks);
                            break;
                        case "golden_apple":
                            player.getInventory().setItem(i, gapple);
                            break;
                        case "diamond_pickaxe":
                            player.getInventory().setItem(i, pick);
                            break;
                        case "bow":
                            player.getInventory().setItem(i, bow);
                            break;
                        case "glyph_menu":
                            if(i > 8 && settingsInsteadOfGlyph) {
                                player.getInventory().setItem(getOpenSpot(hotbar), glyph);
                            } else {
                                player.getInventory().setItem(i, glyph);
                            }
                            break;
                        case "arrow":
                            instance.getPlayer(player.getUniqueId()).arrowLoc = i;
                            break;
                    }
                }
                giveArrow(player);
            }
        };

        if(hotbar == null) {
            // try again in 5 ticks
            runnable.runTaskLater(instance, 4);
        } else {
            runnable.run();
        }
    }
    private static int getOpenSpot(InvItem[] hotbar) {
        for(int i = 8; i >= 0; i--) {
            int finalI = i;
            if(Arrays.stream(hotbar).allMatch(invItem -> invItem.index != finalI)) {
                return i;
            }
        }
        return 7;
    }
    public static void sendActionBar(Player player, String text, int times) {
        IChatBaseComponent comp = IChatBaseComponent.ChatSerializer
                .a("{\"text\":\"" + text + " \"}");
        PacketPlayOutChat packet = new PacketPlayOutChat(comp, (byte) 2);
        for(int i = 0; i < times; i++) {
            (new BukkitRunnable() {
                @Override
                public void run() {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
            }).runTaskLater(instance, 20L * (i));
        }
    }
    public static void sendActionBar(Player player, String text) {
        sendActionBar(player, text, 2);
    }
    private static ItemStack getUnbreakable(ItemStack stack) {
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound nmsCompound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
        nmsCompound.set("Unbreakable", new NBTTagByte((byte) 1));
        nmsStack.setTag(nmsCompound);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }
    public static ItemStack getEnchanted(ItemStack stack) {
        // see https://www.spigotmc.org/threads/adding-the-enchant-glow-to-block.50892/
        stack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.set("HideFlags", new NBTTagInt(1));
        nmsStack.setTag(tag);
        return CraftItemStack.asCraftMirror(nmsStack);
    }
    public static void giveArrow(Player player) {
        List<String> arrow_lore = new ArrayList<String>();
        arrow_lore.add("§7Regenerates every §a3.5s§7!");
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        ItemMeta aim = arrow.getItemMeta();
        aim.setDisplayName("§aArrow");
        aim.setLore(arrow_lore);
        arrow.setItemMeta(aim);
        if(instance.getPlayer(player.getUniqueId()).arrowLoc != -1) {
            player.getInventory().setItem(instance.getPlayer(player.getUniqueId()).arrowLoc, arrow);
        } else {
            player.sendMessage("§c§lUh oh!§r§c Could not figure out the slot your arrow should be in! Please open a ticket on the discord.");
        }
    }
    public static void givePlayerXP(Player player, int xpAmount) {
        try(PreparedStatement statement = connection.prepareStatement("UPDATE players SET xp = xp + ? WHERE uuid=?;")) {
            statement.setInt(1, xpAmount); // xp amount
            statement.setString(2, player.getUniqueId().toString()); // uuid
            statement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§c§lUh oh!§r§c Something went wrong pushing your information to our database. Please open a ticket on the discord!");
        }
    }
    public static int getPlayerXP(Player player) {
        try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT xp FROM players WHERE uuid=?;")) {
            statement.setString(1, player.getUniqueId().toString()); // uuid
            ResultSet res = statement.executeQuery();
            if(!res.next()) {
                throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
            }
            return res.getInt(1); // 1 indexing!
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your xp from our database. Please open a ticket on the discord!");
        }
        return -1;
    }
    public static ItemStack makeItem(Material material, int amount, String itemName, String[] lore, int blockModifier) {
        List<String> loreList = new ArrayList<>();
        for(String line : lore) {
            loreList.add("§7" + line);
        }
        ItemStack item;
        if(blockModifier == -1) {
            item = new ItemStack(material, amount);
        } else {
            item = new ItemStack(material, amount, (byte) blockModifier);
        }
        ItemMeta itemItemMeta = item.getItemMeta();
        if(itemName != null) {
            itemItemMeta.setDisplayName("§r" + itemName);
        }
        itemItemMeta.setLore(loreList);
        item.setItemMeta(itemItemMeta);
        return item;
    }
    public static BlockState[][][] getBlocks(Location start, int width, int height, int length) {
        BlockState[][][] res = new org.bukkit.block.BlockState[height][length][width];
        for(int y = 0; y < height; y++) {
            for(int z = 0; z < length; z++) {
                for(int x = 0; x < width; x++) {
                    res[y][z][x] = start.clone().add(x, y, z).getBlock().getState();
                }
            }
        }
        return res;
    }
    public void sendTablist(Player player, @NotNull String Title, @NotNull String subTitle) {
        // see https://www.spigotmc.org/threads/tablist-header-in-1-8-8.296009/
        IChatBaseComponent tabTitle = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + Title + "\"}");
        IChatBaseComponent tabSubTitle = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + subTitle + "\"}");

        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(tabTitle);

        try {
            Field field = packet.getClass().getDeclaredField("b");
            field.setAccessible(true);
            field.set(packet, tabSubTitle);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void showPlayerNPCs(Player player) {
        // see https://www.spigotmc.org/threads/how-to-create-and-modify-npcs.400753/
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
        for(EntityPlayer npc : npcs) {
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
            connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc)); // Spawns the NPC for the player client.
            connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) ((npc.yaw + 45) * 256 / 360))); // Correct head rotation when spawned in player look direction.

            // apply skins
            DataWatcher watcher = npc.getDataWatcher();
            watcher.watch(10, (byte) 0xFF);
            PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(npc.getId(), watcher, true);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);

            BukkitRunnable removeFromTab = new BukkitRunnable() {
                @Override
                public void run() {
                    connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc));
                }
            };
            removeFromTab.runTaskLater(this, 60);
        }
    }
    public static BlockState[][] deepClone2D(BlockState[][] input) {
        BlockState[][] result = new BlockState[input.length][];
        for(int r = 0; r < input.length; r++) {
            result[r] = input[r].clone();
        }
        return result;
    }
    public static BlockState[][][] deepClone(BlockState[][][] input) {
        BlockState[][][] result = new BlockState[input.length][][];
        for(int r = 0; r < input.length; r++) {
            result[r] = deepClone2D(input[r]);
        }
        return result;
    }
    // returns if they died
    public static boolean damagePlayer(Player player, double amount) {
        double playerHealth = player.getHealth();
        if(player.isBlocking())
            amount *= 0.5;

        RavenAntiCheat.emulatePlayerTakeKnockback(player);

        if(playerHealth - amount > 0) {
            player.damage(amount);
            return false;
        } else {
            PlayerInfo info = instance.getPlayer(player.getUniqueId());
            info.onDeath.call(info);
            return true;
        }
    }
    public static String hearts(int num) {
        return (new String(new char[num]).replace("\0", "❤"));
    }
    public static void showActionBarDamage(Player player, int newHealth, int oldHealth, String name) {
        if(newHealth > oldHealth) {
            // dead
            Bridge.sendActionBar(player, "§c" + name + " §0" + Bridge.hearts(10), 1);
        } else {
            int damageDealt = oldHealth - newHealth;
            Bridge.sendActionBar(player, "§c" + name + " §4" + Bridge.hearts(newHealth) + "§c" + Bridge.hearts(damageDealt) + "§0" + Bridge.hearts(10 - newHealth - damageDealt), 1);
        }
    }
    public static String getPlayerReadableLocation(PlayerLocation pLoc) {
        switch(pLoc) {
            case Wing:
                return "wing practice";
            case Spawn:
                return "spawn";
            case Bypass:
                return "bypass practice";
            case BridgeBot:
                return "bot 1v1 practice";
            case Prebow:
                return "prebow practice";
            case Clutch:
                return "clutch practice";
        }
        return "[[[ If you see this, open a ticket on the Discord! ]]]";
    }
    public static String getCommandFromPlayerLocation(PlayerLocation pLoc) {
        switch(pLoc) {
            case Wing:
                return "wing";
            case Spawn:
                return "spawn";
            case Bypass:
                return "bypass";
            case BridgeBot:
                return "bot";
            case Prebow:
                return "prebow";
        }
        return "[[[ If you see this, open a ticket on the Discord! ]]]";
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

    // Event Handlers
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().setHeldItemSlot(0);

        // get their bridge hotbar if we haven't already
        // we can run this async because it doesn't rely on bukkit APIs
        BukkitRunnable updatePlayerHotbar = new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = connection.prepareStatement("SELECT hotbarSword, hotbarBow, hotbarPickaxe, hotbarBlocksOne, hotbarBlocksTwo, hotbarGoldenApple, hotbarArrow, hotbarGlyph FROM players WHERE uuid=?;")) {
                    statement.setString(1, player.getUniqueId().toString()); // uuid
                    ResultSet res = statement.executeQuery();
                    if(!res.next()) {
                        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                    }
                    InvItem[] items = new InvItem[8];
                    items[0] = new InvItem("iron_sword", res.getInt("hotbarSword"));
                    items[1] = new InvItem("bow", res.getInt("hotbarBow"));
                    items[2] = new InvItem("diamond_pickaxe", res.getInt("hotbarPickaxe"));
                    items[3] = new InvItem("stained_clay_1", res.getInt("hotbarBlocksOne"));
                    items[4] = new InvItem("stained_clay_2", res.getInt("hotbarBlocksTwo"));
                    items[5] = new InvItem("golden_apple", res.getInt("hotbarGoldenApple"));
                    items[6] = new InvItem("arrow", res.getInt("hotbarArrow"));
                    items[7] = new InvItem("glyph_menu", res.getInt("hotbarGlyph"));
                    cachedInventories.put(player.getUniqueId(), items);
                } catch (SQLException e) {
                    e.printStackTrace();
                    BukkitRunnable run = new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.kickPlayer("§c§lUh oh!§r§c There was an error getting your bridge hotbar!\nPlease check the Discord, and open a ticket if the issue persists.");
                        }
                    };
                    run.runTaskLater(Bridge.instance, 0);
                }
            }
        };
        updatePlayerHotbar.runTaskAsynchronously(Bridge.instance);

        sendTablist(player, "§e§lbridgepractice.net", "\n§aJoin the Discord!\n§b§nbridgepractice.net/discord");

        // send them to spawn
        player.chat("/spawn");


        if(getServer().getOnlinePlayers().size() <= 15) {
            event.setJoinMessage("§7[§a+§7] " + event.getPlayer().getDisplayName() + "§7 joined the server!");
        } else {
            event.setJoinMessage("");
        }
    }
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        playerLastMove.remove(uuid);
        cachedInventories.remove(uuid);
        playerNpcTimes.remove(uuid);
        playerArrowRegenerations.remove(uuid);

        PlayerInfo info = getPlayer(player.getUniqueId());
        if(info == null) {
            event.setQuitMessage("");
            return;
        }
        if(info.onLocationChange != null) {
            // as a last resort, if your onlocationchange is bugged out you can just relog
            try {
                info.onLocationChange.call(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        playerInfos.remove(player.getUniqueId());
        PlayerInfo.removeFromQueue(player, false);
        PlayerInfo.nextInQueue(info.location, getCommandFromPlayerLocation(info.location));

        event.setQuitMessage("§7[§c-§7] " + event.getPlayer().getDisplayName() + "§7 left the server.");
        if(getServer().getOnlinePlayers().size() > 15) {
            event.setQuitMessage("");
        }
    }
    @EventHandler
    public void onAnyDamage(EntityDamageEvent event) {
        // disable fall damage, water, suffocation
        if(event.getEntity() instanceof Player && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if(System.currentTimeMillis() - playerLastMove.getOrDefault(playerUuid, 0L) > 60 * 1000)
            playerLastMove.put(playerUuid, System.currentTimeMillis());
        PlayerInfo info = getPlayer(playerUuid);
        Location loc = player.getLocation();
        if((info.allowedBox == null && loc.getY() < 80) || (info.allowedBox != null && (
                loc.getZ() <= (info.allowedBox.relXZ[1] + info.allowedBox.zStart) ||
                        loc.getZ() >= (info.allowedBox.relXZ[1] + info.allowedBox.zEnd) ||
                        loc.getX() <= (info.allowedBox.relXZ[0] + info.allowedBox.xStart) ||
                        loc.getX() >= (info.allowedBox.relXZ[0] + info.allowedBox.xEnd) ||
                        loc.getY() <= (info.allowedBox.yStart) ||
                        loc.getY() >= (info.allowedBox.yEnd)))) {
            // call the on death if it exists
            if(info.onDeath != null) {
                info.onDeath.call(info);
            } else {
                player.teleport(info.respawnLocation);
            }
        } else if(info.winBox != null &&
                info.onWin != null &&
                loc.getZ() >= (info.winBox.relXZ[1] + info.winBox.zStart) &&
                loc.getZ() <= (info.winBox.relXZ[1] + info.winBox.zEnd) &&
                loc.getX() >= (info.winBox.relXZ[0] + info.winBox.xStart) &&
                loc.getX() <= (info.winBox.relXZ[0] + info.winBox.xEnd) &&
                loc.getY() >= (info.winBox.yStart) &&
                loc.getY() <= (info.winBox.yEnd) &&
                player.getGameMode() != GameMode.ADVENTURE) { // ensure they have not already won
            info.onWin.call(info);
        }

        if(info.onMove != null) {
            info.onMove.call(info);
        }
    }
    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        // disable hunger
        event.setCancelled(true);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getCurrentItem() == null) return;

        if(e.getWhoClicked() instanceof Player && e.getClickedInventory() != null) {
            SettingsMenu menu = getPlayer(e.getWhoClicked().getUniqueId()).settingsMenu;
            if(menu != null && e.getClickedInventory().getTitle().equals(menu.title)) {
                // if this is the player's settings menu, then send that click to the menu's onclick
                if(menu.items.get(e.getSlot()) == null) {
                    e.setCancelled(true);
                    return;
                }
                String groupName = menu.items.get(e.getSlot()).group;
                if(groupName != null) {
                    ArrayList<SettingsMenu.Entry> groupList = menu.groups.get(groupName);
                    if(groupList != null) {
                        for(SettingsMenu.Entry entry : groupList) {
                            if(entry.index == e.getSlot()) {
                                menu.inventory.setItem(entry.index, entry.enchantedItem);
                                entry.selected = true;
                            } else if(entry.selected) {
                                menu.inventory.setItem(entry.index, entry.item);
                                entry.selected = false;
                            }
                        }
                    }
                }

                menu.onClick.call(e.getCurrentItem(), groupName);
            }
            if(!debug)
                e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        // disable placement of blocks above y=99 and below y=83
        Player player = event.getPlayer();
        if(cannotPlaceBlocks(event, event.getPlayer())) {
            player.sendMessage("§cYou can't place blocks there!");
            // -------  ANTI-CHEAT: Lagback if the player attempts to jump on ghost blocks
            Vector oldVel = player.getVelocity();
            oldVel.setY(0);
            if(player.getLocation().getBlock().getLocation().equals(event.getBlock().getLocation().add(0, 1, 0))) {
                player.teleport(player.getLocation().subtract(0, 0.4, 0));
                player.setVelocity(oldVel);
            }
            // -------
            event.setCancelled(true);
            return;
        }
        // track it in the PlayerInfo
        PlayerInfo playerInfo = getPlayer(player.getUniqueId());
        playerInfo.changedBlocks.add(event.getBlockReplacedState().getLocation());

        if(playerInfo.onBlockChange != null) {
            playerInfo.onBlockChange.call(playerInfo);
        }
    }
    private boolean cannotPlaceBlocks(BlockPlaceEvent event, Player player) {
        Location loc = event.getBlock().getLocation();
        if(debug) return false;
        if(loc.getY() > 99 || loc.getY() < 84) return true;

        AllowedLocation[] allowedPlacing = getPlayer(player.getUniqueId()).allowedPlacing;
        for(AllowedLocation allowed : allowedPlacing) {
            int relX = allowed.relXZ[0];
            int relZ = allowed.relXZ[1];
            int x = (int) loc.getX();
            int z = (int) loc.getZ();
            boolean isInX = x >= (relX + allowed.xStart) && x <= (relX + allowed.xEnd);
            boolean isInZ = z >= (relZ + allowed.zStart) && z <= (relZ + allowed.zEnd);
            if(isInX && isInZ) {
                return false;
            }
        }
        return true;
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // disallow breaking of random blocks
        Player player = event.getPlayer();
        PlayerInfo info = getPlayer(player.getUniqueId());
        Block block = event.getBlock();
        if(!debug)
            event.setCancelled(true);
        if(info.allowedBreaking != null) {
            for(AllBreak ab : info.allowedBreaking) {
                if(ab.mat == block.getType() && ((byte) ab.data) == block.getData()) {
                    event.setCancelled(false);
                }
            }
        } else {
            if(info.changedBlocks != null && info.changedBlocks.contains(block.getLocation())) {
                event.setCancelled(false);
            }
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if(stack == null)
            return;
        String displayName = stack.getItemMeta().getDisplayName();
        if(displayName == null)
            return;
        if(displayName.equals("§6Settings Menu") && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            player.openInventory(getPlayer(player.getUniqueId()).settingsMenu.inventory);
            return;
        }
        if(displayName.equals("§6Glyph Menu") || displayName.equals("§6Settings Menu")) {
            // play sound effect
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
        }
        if(stack.getType() == Material.BED) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("lobby");
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        }

        PlayerInfo info = getPlayer(player.getUniqueId());
        if(info != null) {
            if(info.location == PlayerLocation.Clutch) {
                org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                if(stack.getType() == Material.STAINED_CLAY) {
                    switch(displayName) {
                        case "§r§7Difficulty: §aEasy ♟ §7(Right Click)":
                            info.locSettings.difficulty = -1;
                            player.sendMessage("§a✔ §7Selected \"§aEasy§7\" difficulty.");
                            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.3f, 1.3f);
                            CommandClutch.selectDifficulty(player, -1);
                            break;
                        case "§r§7Difficulty: §eNormal ♜ §7(Right Click)":
                            info.locSettings.difficulty = 0;
                            player.sendMessage("§a✔ §7Selected \"§eNormal§7\" difficulty.");
                            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.3f, 1);
                            CommandClutch.selectDifficulty(player, 0);
                            break;
                        case "§r§7Difficulty: §c§lHard ♚ §7(Right Click)":
                            info.locSettings.difficulty = 1;
                            player.sendMessage("§a✔ §7Selected \"§c§lHard§7\" difficulty.");
                            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.3f, 0.7f);
                            CommandClutch.selectDifficulty(player, 1);
                            break;
                        default:
                            break;
                    }
                } else if(stack.getType() == Material.STICK) {
                    info.locSettings.doubleHit = true;
                    inv.setItem(4, CommandClutch.doubleHitItem);
                    player.sendMessage("§a✔ §7You will now be hit §atwice§7.");
                    player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.3f, 1.5f);
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.3f, 1.5f);
                        }
                    }).runTaskLater(this, 3);
                } else if(stack.getType() == Material.BLAZE_ROD) {
                    info.locSettings.doubleHit = false;
                    inv.setItem(4, CommandClutch.singleHitItem);
                    player.sendMessage("§a✔ §7You will now be hit §aonce§7.");
                    player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.3f, 1.5f);
                }
            } else if(info.location == PlayerLocation.Spawn) {
                if(stack.getType() == Material.NETHER_STAR) {
                    PlayerInfo.removeFromQueue(player, true);
                }
            }
        }
    }
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if(!(event.getEntity() instanceof Arrow))
            return;

        // delete arrows when they hit anything
        event.getEntity().remove();
        if(!(event.getEntity().getShooter() instanceof Player))
            return;
        Player player = (Player) event.getEntity().getShooter();
        PlayerInfo info = getPlayer(player.getUniqueId());
        if(info == null) return;
        if(info.locSettings.arrowTask != null) {
            info.locSettings.arrowTask.cancel();
        }

        if(info.location == PlayerLocation.Prebow) {
            // if in prebow then make them win on hitting gold block
            // see https://bukkit.org/threads/getting-block-hit-by-projectile-arrow.49071/ #15
            Arrow arrow = (Arrow) event.getEntity();
            World world = arrow.getWorld();
            BlockIterator bi = new BlockIterator(world, arrow.getLocation().toVector(), arrow.getVelocity().normalize(), 0, 4);
            Block hit = null;

            while(bi.hasNext()) {
                hit = bi.next();
                if(hit.getType() != Material.AIR) {
                    break;
                }
            }
            if(hit != null && hit.getType() == Material.GOLD_BLOCK) {
                info.locSettings.bowsInARow++;
                info.onWin.call(info);
            } else {
                info.locSettings.bowsInARow = 0;
            }
        }
    }
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // cancel if entity damaged itself (i.e. shooting bow at yourself)
        if(event.getEntity() instanceof Player) { // don't allow players to hurt other players
            List<MetadataValue> noDamage = event.getDamager().getMetadata("NO_DAMAGE");
            List<MetadataValue> intendedFor = event.getDamager().getMetadata("INTENDED_FOR");
            if(!intendedFor.isEmpty() && !intendedFor.get(0).value().equals(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            if(!noDamage.isEmpty() && noDamage.get(0).asBoolean()) {
                // this will run for things when we want knockback but not damage
                event.setDamage(0);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if(event.getDamager() instanceof Projectile) {
            Projectile arrow = (Projectile) event.getDamager();
            if(event.getEntity().equals(arrow.getShooter())) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        LivingEntity entity = event.getEntity();
        Entity proj = event.getProjectile();
        if(entity instanceof Player && proj instanceof Arrow) {
            Player player = (Player) entity;
            // we are doing this every time; should we?
            ArrowRegenerate ar = new ArrowRegenerate(player);
            ar.runTaskTimer(this, 0, 2);

            PlayerInfo info = getPlayer(player.getUniqueId());
            if(info.onBowShoot != null) {
                info.onBowShoot.call(info, (Arrow) proj, event.getBow());
            }
        } // otherwise we can ignore it
    }
    @EventHandler
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if(item.getType() == Material.GOLDEN_APPLE) {
            // do the custom gapple logic for bridge
            if(item.getAmount() == 1) {
                player.getInventory().remove(item);
            } else {
                player.getItemInHand().setAmount(item.getAmount() - 1);
            }
            player.setHealth(20);
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60 * 20, 0, false, true));
            event.setCancelled(true); // don't give them regen, cancel the event
        }
    }
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    static class InvItem {
        String item;
        int index;
        InvItem(String item, int index) {
            this.item = item;
            this.index = index;
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if(channel.equals("BungeeCord")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if(subchannel.equals("QueueGame")) {
                short len = in.readShort();
                byte[] msgbytes = new byte[len];
                in.readFully(msgbytes);

                DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
                String gameName = "";
                String pName = "";
                try {
                    pName = msgin.readUTF();
                    gameName = msgin.readUTF();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String finalGameName = gameName;
                String finalPName = pName;
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player p = getServer().getPlayerExact(finalPName);
                        p.chat("/"+finalGameName);
                    }
                }).runTaskLater(this, 10);

            }
        } else if(channel.equals("bp:messages")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if(subchannel.equals("Commands")) {
                String cmd = in.readUTF();
                Player p = getServer().getPlayer(UUID.fromString(in.readUTF()));
                if(cmd.equals("lobby")) {
                    if(getPlayer(p.getUniqueId()).location != PlayerLocation.Spawn) {
                        p.chat("/spawn");
                    } else {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF("lobby");

                        p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
                    }
                }
            } else if(subchannel.equals("ReturnPlayerPlayingTime")) {
                String playingTime = in.readUTF();
                Player p = getServer().getPlayer(UUID.fromString(in.readUTF()));
                if(p == null) return;
                Team playingTimeTeam = p.getScoreboard().getTeam("playing_time");
                if(playingTimeTeam == null) return;
                if(playingTime.length() < 14) {
                    playingTimeTeam.setSuffix("§a"+playingTime);
                } else {
                    playingTimeTeam.setPrefix("  §a"+playingTime.substring(0, 12));
                    playingTimeTeam.setSuffix("§a"+playingTime.substring(12));
                }
            }
        }

    }
}
