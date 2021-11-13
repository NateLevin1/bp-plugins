package net.bridgepractice.BridgePracticeLobby;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.*;
import java.math.RoundingMode;
import java.sql.*;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class BridgePracticeLobby extends JavaPlugin implements Listener, PluginMessageListener {
    public static BridgePracticeLobby instance;
    private final ArrayList<Player> playersHidingPlayers = new ArrayList<>();
    private final HashMap<UUID, Long> lastPlayerVisibilityChanges = new HashMap<>();
    private final HashMap<UUID, Long> lastPlayerHotbarEdits = new HashMap<>();
    private final HashMap<UUID, Long> lastPlayerGetFromHypixel = new HashMap<>();
    private final HashMap<UUID, Long> lastShop = new HashMap<>();
    private final HashMap<UUID, Long> lastSpade = new HashMap<>();
    private final HashMap<UUID, Long> lastTele = new HashMap<>();

    private final HashMap<UUID, Location> respawnLocation = new HashMap<>();
    private final HashMap<UUID, Leaderboard[]> clickableLeaderboards = new HashMap<>();
    public static final String hypixelKey = "b3b3895d-1604-4ef7-b0bb-bd14f169be95";
    private final ItemStack hotbarLayoutItem = Utils.getEnchanted(Utils.makeItem(Material.BOOK, "§aEdit Hotbar Layout §7(Right Click)", "§7Customize your hotbar", "§7layout for all modes", "", "§eRight click to edit"));
    private final ItemStack duelPlayerItemIron = Utils.getUnbreakable(Utils.makeItem(Material.IRON_SWORD, "§aDuel Player §7(Hit Players to Duel)", "§7Left click on players", "§7to duel them"));
    private final ItemStack duelPlayerItemGold = Utils.getUnbreakable(Utils.makeItem(Material.GOLD_SWORD, "§aDuel Player §7(Hit Players to Duel)", "§7Left click on players", "§7to duel them"));
    private final ItemStack duelPlayerItemDiamond = Utils.getUnbreakable(Utils.makeItem(Material.DIAMOND_SWORD, "§aDuel Player §7(Hit Players to Duel)", "§7Left click on players", "§7to duel them"));
    private final HashMap<UUID, Long> playerNpcTimes = new HashMap<>();
    public final ArrayList<EntityPlayer> npcs = new ArrayList<>();
    Menu gameMenu;
    Menu multiplayerGamesMenu;

    // db related things
    String host = "localhost";
    String port = "3306";
    String database = "bridge";
    String username = "mc";
    String password = "mcserver";
    static Connection connection;
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        try {
            openConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // fix time
        List<World> worlds = getServer().getWorlds();
        (new BukkitRunnable() {
            @Override
            public void run() {
                for(World world : worlds) {
                    world.setTime(1000L);
                }
            }
        }).runTaskTimer(this, 0, 10 * 20);

        instance = this;

        ItemStack singleplayer = Utils.makeDyed(Material.STAINED_CLAY, DyeColor.BLUE, "§aSingleplayer", "§7Go to the singleplayer lobby.", "", "§eClick to Go");
        ItemStack multiplayer = Utils.makeDyed(Material.STAINED_CLAY, DyeColor.RED, "§aMultiplayer", "§7View the multiplayer game modes.", "", "§eClick to View");

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        // allow menu to be garbage collected if it is single use
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.CLOSE_WINDOW) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                InventoryView inv = player.getOpenInventory();
                if(inv == null) return;
                if(inv.getCursor().getType() != Material.AIR) {
                    inv.setCursor(new ItemStack(Material.AIR));
                }
                Menu menu = Menu.menus.get(inv.getTitle());
                if(menu == null) return;
                menu.allowForGarbageCollection();
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                int entityId = packet.getIntegers().read(0);
                GameType gameType = entityGameTypes.get(entityId);
                Leaderboard[] clickedLb = clickableLeaderboards.get(player.getUniqueId());
                if(gameType != null) {
                    long lastPressTime = playerNpcTimes.getOrDefault(player.getUniqueId(), 0L);
                    if(System.currentTimeMillis() - lastPressTime > 800) {
                        playerNpcTimes.put(player.getUniqueId(), System.currentTimeMillis());
                        BukkitRunnable send = new BukkitRunnable() {
                            @Override
                            public void run() {
                                switch(gameType) {
                                    case Singleplayer: {
                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                        out.writeUTF("Connect");
                                        out.writeUTF("singleplayer");

                                        player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
                                        break;
                                    }
                                    case Multiplayer:
                                        player.openInventory(multiplayerGamesMenu.getInventory());
                                        break;
                                    case Unranked: {
                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                        out.writeUTF("RequestGame");
                                        out.writeUTF("unranked");
                                        player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
                                        break;
                                    }
                                    case PvP: {
                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                        out.writeUTF("RequestGame");
                                        out.writeUTF("pvp");
                                        player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
                                        break;
                                    }
                                    default:
                                        break;
                                }
                            }
                        };
                        send.runTask(instance);
                    }
                }

                for(Leaderboard leaderboard : clickedLb) {
                    if(leaderboard.isIdFromThisLeaderboard(entityId)) {
                        leaderboard.onClickableClick(player);
                    }
                }
            }
        });

        ItemStack unranked = Utils.makeItem(Material.IRON_SWORD, "§aBridge Duel", "§8Multiplayer", "", "§7Defeat your opponent in a", "§7classic game of The Bridge.", "", "§eClick to Play!");
        ItemStack pvp = Utils.makeItem(Material.IRON_BOOTS, "§aBridge PvP 1v1", "§8Multiplayer", "", "§7Kill your opponent 5 times", "§7on a developed bridge", "", "§eClick to Play!");

        gameMenu = new Menu("Game Menu", 5, false,
                new MenuItem(1, 1, Utils.makeItem(Material.BOOKSHELF, "§aMain Lobby", "§7Return to the Main Lobby.", "", "§eClick to Go"), (p, m) -> sendPlayerToServer(p, "lobby")),
                new MenuItem(2, 1, Utils.makeCustomPlayerHead("http://textures.minecraft.net/texture/e25a2f9cb91863bedcfcc40ec31992368fb4ea8f34c532bc3a58c0ac63977be5", "§aMy Stats", "§7View your statistics", "§7across gamemodes.", "", "§eClick to View"), (p, m) -> showStats(p)),
                new MenuItem(3, 1, Utils.makeItem(Material.EMERALD, "§aBridgePractice Store", "§cComing soon!", "", "§7Purchase a rank to help", "§7support the server!", "", "§eClick to Visit"), (p, m) -> {
                    m.allowForGarbageCollection();
                    p.closeInventory();
                    sendToStore(p);
                }),

                new MenuItem(1, 3, singleplayer, (p, m) -> sendPlayerToServer(p, "singleplayer")),
                new MenuItem(1, 4, singleplayer, (p, m) -> sendPlayerToServer(p, "singleplayer")),
                new MenuItem(2, 3, Utils.makeItem(Material.STONE_SWORD, "§aBot 1v1", "§8Singleplayer", "", "§7Try out new strategies", "§7and get high winstreaks", "§7against an AI!", "", "§eClick to Play!"), (p, m) -> playSingleplayerGame(p, "bot")),
                new MenuItem(2, 4, Utils.makeItem(Material.SUGAR, "§aBypass Practice", "§8Singleplayer", "", "§7Practice bypassing at", "§7different game times", "", "§eClick to Play!"), (p, m) -> playSingleplayerGame(p, "bypass")),
                new MenuItem(3, 3, Utils.makeItem(Material.CLAY_BRICK, "§aWing Practice", "§8Singleplayer", "", "§7Sharpen your bridging", "§7skills!", "", "§eClick to Play!"), (p, m) -> playSingleplayerGame(p, "wing")),
                new MenuItem(3, 4, Utils.makeItem(Material.ARROW, "§aPrebow Practice", "§8Singleplayer", "", "§7Practice your prebows", "§7to hit them every time.", "", "§eClick to Play!"), (p, m) -> playSingleplayerGame(p, "prebow")),

                new MenuItem(1, 6, multiplayer, (p, m) -> showMultiplayerGames(p)),
                new MenuItem(1, 7, multiplayer, (p, m) -> showMultiplayerGames(p)),
                new MenuItem(2, 6, unranked, (p, m) -> requestGame(p, "unranked")),
                new MenuItem(2, 7, pvp, (p, m) -> requestGame(p, "pvp")),
                new MenuItem(3, 6, multiplayer, (p, m) -> showMultiplayerGames(p)),
                new MenuItem(3, 7, multiplayer, (p, m) -> showMultiplayerGames(p))
        );

        multiplayerGamesMenu = new Menu("Multiplayer Games", 3, false,
                new MenuItem(1, 3, unranked, (p, m) -> requestGame(p, "unranked")),
                new MenuItem(1, 5, pvp, (p, m) -> requestGame(p, "pvp"))
        );

        World world = getServer().getWorld("world");
        Block rightUrbanFlawless = world.getBlockAt(new Location(world, 30, 98, 11));
        rightUrbanFlawless.setMetadata("parkour", new FixedMetadataValue(this, "Urban Flawless Right"));
        rightUrbanFlawless.setMetadata("parkour_respawn_location", new FixedMetadataValue(this, new Location(world, 30.5, 98, 11.5, -90, 0)));

        Block leftUrbanFlawless = world.getBlockAt(new Location(world, 30, 98, -11));
        leftUrbanFlawless.setMetadata("parkour", new FixedMetadataValue(this, "Urban Flawless Left"));
        leftUrbanFlawless.setMetadata("parkour_respawn_location", new FixedMetadataValue(this, new Location(world, 30.5, 98, -10.5, -90, 0)));

        createNPCForAll(new Location(world, 13.5, 99, -2.5, 90, 0), "Singleplayer", "ewogICJ0aW1lc3RhbXAiIDogMTYyOTMyNDA1NTY0OCwKICAicHJvZmlsZUlkIiA6ICJmMjc0YzRkNjI1MDQ0ZTQxOGVmYmYwNmM3NWIyMDIxMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJIeXBpZ3NlbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yODk5ZDcyODdjMGEwNjlhNWM3ZDk3MzUyZjgwMWFkNTRmMDg1MmEwZjgzNDAwZTdkMmQ3ZmQ0NDMwNTNiYzUxIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "jvcOCHH33SUSpYsDC1pxBukWZXNI1sREuRQFdoYUYR6G0WGDCLy9GQhz1CbAr85syoT9GXvmiAl6i6Mi+l0FCfnNopfA8eEsqSSeN2rzp4AZL97S7t4umGT4VcBInUY8JY5FC8L6tpQ8pG23pr1jw8FhuFiTV4PRmyKQsHmcEimbVy/E+41A24KTRHFTYpHRZNmUQTS7erF3uWS97edEg1lA1+cLTGgMpN78nlrhCMMauBb2M/w3j0BCONIHA82g4cxWnQqv6T+xbPuiZ/qwy7KazFqwH33ibTezZUaq+sBhg1EJdX1cgQQI3l7MfvdoGf+/DJujbp5M2wCq1MqKdJvMVk9pJ31UCY617+lv7uw8aUU4oTW24iDIyhVd0E2bC+b+GB4EDK0FI1TpPHypEur2v7WsyqIo7/eDLEjRB71qMiOJJCnlNAn2UE/a/22zpBqFY11z6JDmYI8SapZTNXuU8x0XRMCP/NABRPA0f0bcKfz6oLDheXciDVmReMGHOUV54fBKwrkhdIYAIvMU1jk1maYZuxTVNlSiVXlNPbzbPA55M/k9kKNVy2oB3IwkgLuh6LqzI6AVGNVLTQBElH43GBuG2GZAsoxn8NRlZ13M3J2oqO1qNC9lMPhbdXIiPU5kjYZSR9ahCceifGfO029m0EJAhJtMdRsjFjjQ6M4=", GameType.Singleplayer); // http://textures.minecraft.net/texture/2899d7287c0a069a5c7d97352f801ad54f0852a0f83400e7d2d7fd443053bc51
        createNPCForAll(new Location(world, 15.5, 99, 0.5, 90, 0), "Featured", "ewogICJ0aW1lc3RhbXAiIDogMTYyODk3ODAwMjg1OCwKICAicHJvZmlsZUlkIiA6ICI5MGQ1NDY0OGEzNWE0YmExYTI2Yjg1YTg4NTU4OGJlOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJFdW4wbWlhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzM0Mzg2MDIwODM1MzA3MTZhNGNlNzlmN2RhMTM5NzkxM2QxMWVkMmJjYTc4MGMyYmYwMDVmMDdmMWE5ZGI2MWUiCiAgICB9CiAgfQp9", "dKecB/zYUr+oblBy7GeG3YXKo2bMFXAr90nIsXYf88LP7PZBpTVq72zx3/bFmH+94/crBx0hf0A9fd2ssDqV2Bv4DxNDJg1Fgy2fvusPlqjix8ffJvPmyQHVhz8aTNFrKdnNpVzubCll++KzPamEfCvNEKQWESe4EmbdchwHcZEtA34V9Da8Arj3NMXt5uatkr854f4FWzsRn0XmxddIlEnRxUxqdl36Yl9pHZGVMi5S03mv6hkcRdKAVgO9uG4zv2NCAdvrxqepKe5onxvnOHLiEPhhVd0BwfUOOZWpRcmxLH9XyhPwlK5KVB4tGpH3iNbmuSBzZ8WScaE8w3Oxj02jw9Hfae96QHCEUn2cHnC3e6GvhfHHSu5a9kptJ3wW9yNMSajLlIHvAlCi2rDdGivOU2sBlZdi4vLmc2eGdHF8dVBG8X43F8xOqhiPO30wj+VMJpwPm8/g0eb3cblItptCrjCItr3Jnu+DoGgDv6Q7OKpdeA84PQw0pnq3ud59DnfjMnkKYiBOkJUxCGHDcYUXax2g4IWBjYWMZWpYOoFrnAz/CKILBh0BwHLDoahYLxY3qsST1H+fVEvekpWQFQijzJmRoZRgIKAJUMoR02YIkj9fDdw96vnS8eiIVr8OnKmbegqjqFjUdzaNVi0DzXB3bHWU/hXJWHdpyYnIiQM=", GameType.PvP); // http://textures.minecraft.net/texture/3438602083530716a4ce79f7da1397913d11ed2bca780c2bf005f07f1a9db61e
        createNPCForAll(new Location(world, 13.5, 99, 3.5, 90, 0), "Multiplayer", "ewogICJ0aW1lc3RhbXAiIDogMTYyOTMyNDI5NzkxNywKICAicHJvZmlsZUlkIiA6ICIzNDkxZjJiOTdjMDE0MWE2OTM2YjFjMjJhMmEwMGZiNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJKZXNzc3N1aGgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTM2NWM2ZDhkMjA3MmE1ZDMxZWM0YzRjM2RkMmQzYzQ0OGM2YzgxNTZkZDQyMTFiYmQyNDRjZmRkMzI4ODhjOSIKICAgIH0KICB9Cn0=", "lm5N3j6BtkHYU7H20QnZ/0t04ptOEY0apxVvaTDWow4dvREZwQLY8+1+jZlpZCDDsWjPACY5kVlf+PG2mv4o+/0aNmQTMf0Cp3KXpdtROZeAmRyRPH/7OWoVGyzFwVzFROyBuUJNhp4M5fW2qaWzRxK+hjmGjmrXtwsfCMqTkzlkPfORS15UWTcir3eN6TvleqbftBydW5YIskoNi5AzTMBLgwMIaBnJoQ2WvMowomS3fTrLE090LPqrE30t8rmaiJBkUnp1iBkw8pEAzPLgTnligk+kG9vY3YrPm704fYy8M5gmMrbZarmLbBEPw0MSpotJCc6Nx3DcoogOMsXzGM/MpwJFH/LeIcsD/FNN1TJgRfEZ9bE6TjGsseXmJuVDI3iSQe6Q87ib86yNNzri70rAooKcyRL4WlK70C3stL4Ul7IZdtVryUcs1q/3FmerSNSmh7ztWPNADbM/CbMbcW25S17XxROemsuL2nWSH4f83dTScJjch6mUpxmg37aOrrKQWwRrDII8SQHWVPob8oOMO8PhYFkQ5UIeN71FuVVtq1NdZmzC0GdShJixv47hqza7ngINkfujQK1pYmIenv78b/pRfhdvlD4lUfu878NxjUWbeVO82i6aiWAp889vqhRjjGAWo4ChbjKN8gqUSS8koR6XoWb+bDwYGW36CW0=", GameType.Multiplayer); // http://textures.minecraft.net/texture/1365c6d8d2072a5d31ec4c4c3dd2d3c448c6c8156dd4211bbd244cfdd32888c9

        getCommand("rainbow").setExecutor(new RainbowCommand());
        getCommand("fw").setExecutor(new FireworkCommand());
        getCommand("cookie").setExecutor(new CookieCommand());
        getCommand("telestick").setExecutor(new TelestickCommand());
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
    private static class HotbarItem {
        ItemStack item;
        int index;
        HotbarItem(ItemStack item, int index) {
            this.item = item;
            this.index = index;
        }
    }
    public void requestGame(Player player, String gameType) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RequestGame");
        out.writeUTF(gameType);
        player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
    }
    public void sendPlayerToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }
    public void playSingleplayerGame(Player player, String gameName) {
        sendPlayerToServer(player, "singleplayer");
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward"); // So BungeeCord knows to forward it
        out.writeUTF("singleplayer");
        out.writeUTF("QueueGame"); // The channel name to check if this your data

        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        try {
            msgout.writeUTF(player.getName());
            msgout.writeUTF(gameName);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());
        player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
    }
    private HotbarItem[] getHotbarSync(Player player) {
        try(PreparedStatement statement = connection.prepareStatement("SELECT hotbarSword, hotbarBow, hotbarPickaxe, hotbarBlocksOne, hotbarBlocksTwo, hotbarGoldenApple, hotbarArrow, hotbarGlyph FROM players WHERE uuid=?;")) {
            statement.setString(1, player.getUniqueId().toString()); // uuid
            ResultSet res = statement.executeQuery();
            if(!res.next()) {
                throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
            }
            HotbarItem[] items = new HotbarItem[8];
            items[0] = new HotbarItem(Utils.getSword(), res.getInt("hotbarSword"));
            items[1] = new HotbarItem(Utils.getBow(), res.getInt("hotbarBow"));
            items[2] = new HotbarItem(Utils.getPickaxe(), res.getInt("hotbarPickaxe"));
            items[3] = new HotbarItem(Utils.getBlocks(), res.getInt("hotbarBlocksOne"));
            items[4] = new HotbarItem(Utils.getBlocks(), res.getInt("hotbarBlocksTwo"));
            items[5] = new HotbarItem(Utils.getGapple(), res.getInt("hotbarGoldenApple"));
            items[6] = new HotbarItem(Utils.getArrow(), res.getInt("hotbarArrow"));
            items[7] = new HotbarItem(Utils.getGlyph(), res.getInt("hotbarGlyph"));
            return items;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
        }
        return new HotbarItem[0];
    }
    private HotbarItem[] getHotbarFromHypixelSync(Player player) throws IOException {
        JsonObject apiResponse;
        try {
            apiResponse = Utils.getJSON("https://api.hypixel.net/player?key=" + hypixelKey + "&uuid=" + player.getUniqueId()).get("player").getAsJsonObject().get("stats").getAsJsonObject().get("Duels").getAsJsonObject().get("layout_bridge_duel_layout").getAsJsonObject();
        } catch (NullPointerException e) {
            throw new IOException("Player is not available in the Hypixel API");
        }
        HotbarItem[] items = new HotbarItem[8];
        for(Map.Entry<String, JsonElement> item : apiResponse.entrySet()) {
            int i = Integer.parseInt(item.getKey());
            String name = item.getValue().getAsString();
            switch(name) {
                case "iron_sword":
                    items[0] = new HotbarItem(Utils.getSword(), i);
                    break;
                case "stained_clay_1":
                    items[1] = new HotbarItem(Utils.getBlocks(), i);
                    break;
                case "stained_clay_2":
                    items[2] = new HotbarItem(Utils.getBlocks(), i);
                    break;
                case "golden_apple":
                    items[3] = new HotbarItem(Utils.getGapple(), i);
                    break;
                case "diamond_pickaxe":
                    items[4] = new HotbarItem(Utils.getPickaxe(), i);
                    break;
                case "bow":
                    items[5] = new HotbarItem(Utils.getBow(), i);
                    break;
                case "glyph_menu":
                    items[6] = new HotbarItem(Utils.getGlyph(), i);
                    break;
                case "arrow":
                    items[7] = new HotbarItem(Utils.getArrow(), i);
                    break;
            }
        }
        return items;
    }
    private void getFromHypixelIfNotCustomized(Player player) {
        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement barCustomStatement = connection.prepareStatement("SELECT hotbarCustomized FROM players WHERE uuid=?;")) {
                    barCustomStatement.setString(1, player.getUniqueId().toString()); // uuid
                    ResultSet res = barCustomStatement.executeQuery();
                    if(!res.next()) {
                        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                    }
                    boolean isCustomized = res.getBoolean(1);
                    if(!isCustomized) {
                        HotbarItem[] items = getHotbarFromHypixelSync(player);
                        try(PreparedStatement updateStatement = connection.prepareStatement("UPDATE players SET hotbarCustomized = TRUE, hotbarSword = ?, hotbarBow = ?, hotbarPickaxe = ?, hotbarBlocksOne = ?, hotbarBlocksTwo = ?, hotbarGoldenApple = ?, hotbarArrow = ?, hotbarGlyph = ? WHERE uuid=?;")) {
                            boolean hasSeenBlocks = false;
                            for(HotbarItem item : items) {
                                switch(item.item.getType()) {
                                    case IRON_SWORD:
                                        updateStatement.setInt(1, item.index);
                                        break;
                                    case BOW:
                                        updateStatement.setInt(2, item.index);
                                        break;
                                    case DIAMOND_PICKAXE:
                                        updateStatement.setInt(3, item.index);
                                        break;
                                    case STAINED_CLAY:
                                        if(!hasSeenBlocks) {
                                            hasSeenBlocks = true;
                                            updateStatement.setInt(4, item.index);
                                        } else {
                                            updateStatement.setInt(5, item.index);
                                        }
                                        break;
                                    case GOLDEN_APPLE:
                                        updateStatement.setInt(6, item.index);
                                        break;
                                    case ARROW:
                                        updateStatement.setInt(7, item.index);
                                        break;
                                    case DIAMOND:
                                        updateStatement.setInt(8, item.index);
                                        break;
                                }
                            }
                            updateStatement.setString(9, player.getUniqueId().toString()); // uuid, set to player uuid
                            updateStatement.executeUpdate();
                        }
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }

            }
        }).runTaskAsynchronously(this);
    }
    private HotbarItem[] defaultHotbar() {
        HotbarItem[] items = new HotbarItem[8];
        items[0] = new HotbarItem(Utils.getSword(), 0);
        items[1] = new HotbarItem(Utils.getBow(), 1);
        items[2] = new HotbarItem(Utils.getPickaxe(), 2);
        items[3] = new HotbarItem(Utils.getBlocks(), 3);
        items[4] = new HotbarItem(Utils.getBlocks(), 4);
        items[5] = new HotbarItem(Utils.getGapple(), 5);
        items[7] = new HotbarItem(Utils.getGlyph(), 7);
        items[6] = new HotbarItem(Utils.getArrow(), 8);
        return items;
    }
    private void showStats(Player player) {
        MenuItem wing = new MenuItem(2, 1, Utils.makeItem(Material.CLAY_BRICK, "§eWing Practice", "§8Singleplayer", ""), null);
        MenuItem bypass = new MenuItem(2, 2, Utils.makeItem(Material.SUGAR, "§eBypass Practice", "§8Singleplayer", ""), null);
        MenuItem prebow = new MenuItem(3, 1, Utils.makeItem(Material.ARROW, "§ePrebow Practice", "§8Singleplayer", ""), null);
        MenuItem bot = new MenuItem(3, 2, Utils.makeItem(Material.STONE_SWORD, "§eBot 1v1", "§8Singleplayer", ""), null);
        MenuItem unranked = new MenuItem(2, 6, Utils.makeItem(Material.IRON_SWORD, "§eBridge Duel", "§8Multiplayer", ""), null);
        MenuItem pvp = new MenuItem(2, 7, Utils.makeItem(Material.IRON_BOOTS, "§eBridge PvP 1v1", "§8Multiplayer", ""), null);

        MenuItem star = new MenuItem(3, 4, Utils.makeItem(Material.NETHER_STAR, "§eOther Stats", ""), null);

        Menu stats = new Menu("Your Stats", 5, true,
                new MenuItem(1, 4, Utils.makeItem(Material.BEACON, "§5Your Statistics", "§7View your statistics", "§7across the network"), null),

                wing,
                bypass,
                prebow,
                bot,

                unranked,
                pvp,

                star
        );
        player.openInventory(stats.getInventory());

        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid=?;")) {
                    statement.setString(1, player.getUniqueId().toString()); // uuid
                    ResultSet res = statement.executeQuery();
                    if(!res.next()) {
                        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                    }
                    Inventory inv = stats.getInventory();

                    DecimalFormat decimalFormatter = new DecimalFormat("#.###");
                    decimalFormatter.setRoundingMode(RoundingMode.CEILING);

                    double wingPB = res.getFloat("wingPB") * 0.001;
                    inv.setItem(wing.index, Utils.addLore(wing.item, "§7 - §fPersonal Best: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(wingPB)), ""));

                    double startGame = res.getFloat("bypassStartPB") * 0.001;
                    Utils.addLore(bypass.item, "§7 - §fStart Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(startGame)));
                    double earlyGame = res.getFloat("bypassEarlyPB") * 0.001;
                    Utils.addLore(bypass.item, "§7 - §fEarly Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(earlyGame)));
                    double middleGame = res.getFloat("bypassMiddlePB") * 0.001;
                    Utils.addLore(bypass.item, "§7 - §fMiddle Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(middleGame)));
                    double lateGame = res.getFloat("bypassLatePB") * 0.001;
                    inv.setItem(bypass.index, Utils.addLore(bypass.item, "§7 - §fLate Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(lateGame)), ""));

                    int prebowHits = res.getInt("prebowHits");
                    inv.setItem(prebow.index, Utils.addLore(prebow.item, "§7 - §fAll Time Hits: §a" + prebowHits, ""));

                    int botWinStreak = res.getInt("botWinStreak");
                    Utils.addLore(bot.item, "§7 - §fWin Streak: §a" + botWinStreak);
                    int botWins = res.getInt("botWins");
                    inv.setItem(bot.index, Utils.addLore(bot.item, "§7 - §fTotal Wins: §a" + botWins));

                    int xp = res.getInt("xp");
                    Utils.addLore(star.item, "§7 - §fXP: §a" + xp);
                    Date firstLogin = res.getDate("firstLogin");
                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                    inv.setItem(star.index, Utils.addLore(star.item, "§7 - §fFirst Login: §a" + formatter.format(firstLogin), ""));

                    {
                        int curWs = res.getInt("unrankedCurrentWinStreak");
                        int allTimeWs = res.getInt("unrankedAllTimeWinStreak");
                        int wins = res.getInt("unrankedWins");
                        int losses = res.getInt("unrankedLosses");
                        inv.setItem(unranked.index, Utils.addLore(unranked.item, "§7 - §fCurrent Winstreak: §a" + curWs,
                                "§7 - §fBest Winstreak: §a" + allTimeWs,
                                "§7 - §fTotal Wins: §a" + wins,
                                "§7 - §fTotal Losses: §a" + losses,
                                "§7 - §fWin/Loss Ratio: §a" + (losses != 0 ? decimalFormatter.format(wins / ((double) losses)) : (wins == 0 ? "§cN/A" : "Infinity!"))));
                    }

                    {
                        int curWs = res.getInt("pvpCurrentWinStreak");
                        int allTimeWs = res.getInt("pvpAllTimeWinStreak");
                        int wins = res.getInt("pvpWins");
                        int losses = res.getInt("pvpLosses");
                        inv.setItem(pvp.index, Utils.addLore(pvp.item, "§7 - §fCurrent Winstreak: §a" + curWs,
                                "§7 - §fBest Winstreak: §a" + allTimeWs,
                                "§7 - §fTotal Wins: §a" + wins,
                                "§7 - §fTotal Losses: §a" + losses,
                                "§7 - §fWin/Loss Ratio: §a" + (losses != 0 ? decimalFormatter.format(wins / ((double) losses)) : (wins == 0 ? "§cN/A" : "Infinity!")),
                                ""));
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
                }
            }
        }).runTaskAsynchronously(this);
    }
    public static String prettifyTime(double time) {
        return String.format("%.3f", time);
    }
    private void sendToStore(Player player) {
        player.sendMessage("\n§cThe store hasn't opened yet! However, you can join the Discord to be notified when it does!");
        player.spigot().sendMessage(new ComponentBuilder("Click to join: §nhttp://bridgepractice.net/discord").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://bridgepractice.net/discord")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("§bClick to get the invite to the discord!")})).create());
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1);
    }
    public void showPlayerNPCs(Player player) {
        // see https://www.spigotmc.org/threads/how-to-create-and-modify-npcs.400753/
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
        for(EntityPlayer npc : npcs) {
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
            connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc)); // Spawns the NPC for the player client.
            connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) ((npc.yaw - 45) * 256 / 360))); // Correct head rotation when spawned in player look direction.

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
    enum GameType {
        Singleplayer,
        Multiplayer,
        Unranked,
        PvP
    }

    private final HashMap<Integer, GameType> entityGameTypes = new HashMap<>();
    public void createNPCForAll(Location loc, String name, String skinBase64, String signature, GameType gameType) {
        // see https://www.spigotmc.org/threads/how-to-create-and-modify-npcs.400753/
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name); // max 16 characters
        EntityPlayer npc = new EntityPlayer(((CraftServer) this.getServer()).getServer(), ((CraftWorld) loc.getWorld()).getHandle(), gameProfile, new PlayerInteractManager(((CraftWorld) loc.getWorld()).getHandle())); // This will be the EntityPlayer (NPC) we send with the sendNPCPacket method.
        npc.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw() + 46, loc.getPitch());
        entityGameTypes.put(npc.getId(), gameType);
        npcs.add(npc);

        // add skin (should we do this more often than startup?)
        (new BukkitRunnable() {
            @Override
            public void run() {
                // faster startup, still works
                gameProfile.getProperties().put("textures", new Property("textures", skinBase64, signature));
            }
        }).runTaskAsynchronously(this);
    }
    private void showMultiplayerGames(Player player) {
        player.openInventory(multiplayerGamesMenu.getInventory());
    }
    private void setCage(Player player, Menu menu, Cage cage) {
        menu.allowForGarbageCollection();
        player.closeInventory();
        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = connection.prepareStatement("UPDATE players SET cage = ? WHERE uuid=?;")) {
                    statement.setInt(1, cage.ordinal());
                    statement.setString(2, player.getUniqueId().toString()); // uuid, set to player uuid
                    statement.executeUpdate();

                    player.sendMessage("§a✔ Successfully set your cage to " + cage + ".");
                } catch (SQLException throwables) {
                    player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your time!");
                    throwables.printStackTrace();
                }
            }
        }).runTaskAsynchronously(this);
    }
    public void removeGadgetEffects(Player player){
        Gadget gadget = getGadget(player);
        if (gadget == null) return;
        switch (gadget.fifthSlotItem.getType()){
            case COOKIE:
                player.setFoodLevel(20);
                break;
            case BLAZE_ROD:
                player.getInventory().setBoots(null);
                break;
        }

    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().setHeldItemSlot(0);
        Inventory inv = player.getInventory();
        inv.clear();

        ItemStack[] armorSet = new ItemStack[player.getInventory().getArmorContents().length];
        for (int i = 0; i < armorSet.length; i++) {armorSet[i] = new ItemStack(Material.AIR);}
        player.getInventory().setArmorContents(armorSet);

        player.setFoodLevel(20);
        player.teleport(new Location(player.getWorld(), 2.5, 99, 0.5, -90, 0));

        clickableLeaderboards.remove(player.getUniqueId());

        for(Player p : playersHidingPlayers) {
            p.hidePlayer(player);
        }

        // if they don't have a db row then create it
        try(PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO players(uuid, firstLogin) VALUES (?, ?);"); /* insert if doesn't exist */) {
            statement.setString(1, player.getUniqueId().toString()); // uuid, set to player uuid
            statement.setDate(2, new Date(new java.util.Date().getTime())); // firstLogin, set to today's date
            statement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        getFromHypixelIfNotCustomized(player);

        inv.setItem(0, Utils.makeItem(Material.COMPASS, "§aGame Menu §7(Right Click)", "§7Right click to use the Game Menu!"));
        inv.setItem(1, Utils.makePlayerHead(player.getName(), "§aMy Stats §7(Right Click)", "§7View your statistics", "§7across gamemodes.", "", "§eRight Click to View"));
        inv.setItem(2, Utils.makeItem(Material.EMERALD, "§aShop §7(Right Click)", "", "§7View options to", "§7customize your game"));

        giveGadgets(player, inv);

        inv.setItem(7, Utils.makeDyed(Material.INK_SACK, DyeColor.PURPLE, "§fPlayers: §aShown §7(Right Click)", "§7Right click to toggle player visibility!"));
        inv.setItem(8, Utils.getUnbreakable(Utils.makeItem(Material.GOLD_SPADE, "§6Leaderboards §7(Right Click)", "§7View the leaderboards")));

        Utils.sendTablist(player, "§e§lbridgepractice.net", "\n§aJoin the Discord!\n§b§nbridgepractice.net/discord");

        // send welcome message
        player.sendMessage("§3§m----------------------------------§6" +
                        "\n  Welcome to §eBridge Practice§6!" +
                        "\n  §3§lDISCORD: §bbridgepractice.net/discord" +
                        "\n  §c§lNEW! §a§lSTORE: §bstore.bridgepractice.net" +
                        "\n§3§m----------------------------------");
        if(!player.hasPermission("group.legend")) {
            player.spigot().sendMessage(new ComponentBuilder("Please support this custom-coded\nserver by buying a rank! (click)").color(ChatColor.GREEN).bold(false).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to visit our store!").color(ChatColor.AQUA).create())).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://store.bridgepractice.net")).create());
        }

        Scoreboard board = Utils.createScoreboard("   §b§lBridge §c§lPractice   ", new String[]{
                "",
                "%xp%  XP: ",
//                "%level%  Your Level: ", // once we add levels :)
//                "%percentage%    ",
                "",
                "  Players Online:",
                "%total%  §7Total: ",
                "%game%  §7In-Game: ",
                "",
                "   §ebridgepractice.net  "
        });
        player.setScoreboard(board);
        (new BukkitRunnable() {
            @Override
            public void run() {

                ByteArrayDataOutput allCount = ByteStreams.newDataOutput();
                allCount.writeUTF("PlayerCount");
                allCount.writeUTF("ALL");
                player.sendPluginMessage(instance, "BungeeCord", allCount.toByteArray());

                ByteArrayDataOutput lobbyCount = ByteStreams.newDataOutput();
                lobbyCount.writeUTF("PlayerCount");
                lobbyCount.writeUTF("lobby");
                player.sendPluginMessage(instance, "BungeeCord", lobbyCount.toByteArray());

                board.getTeam("xp").setSuffix("§a" + Utils.getPlayerXPSync(player) + "⫯");
//                board.getTeam("level").setSuffix("§e2◉");
//                board.getTeam("percentage").setSuffix("§7(58/100)");
            }
        }).runTaskAsynchronously(instance);

        showPlayerNPCs(player);

        Team npcTeam = board.registerNewTeam("npcs");
        npcTeam.setPrefix("§8[NPC] ");
        for(EntityPlayer npc : npcs) {
            npcTeam.addEntry(npc.getName());
        }
        npcTeam.setNameTagVisibility(NameTagVisibility.NEVER);

        // show leaderboards
        (new BukkitRunnable() {
            @Override
            public void run() {
                Leaderboard otherLb = new Leaderboard("Other Leaderboards", player, 57, 105, -2.5, Leaderboard.Direction.Ascending, Leaderboard.ColumnType.Float);
                otherLb.addClickable(new String[]{"Wing", "XP", "Play Time"}, new String[]{"wingPB", "xp", "playingTime"}, new Leaderboard.Direction[]{Leaderboard.Direction.Ascending, Leaderboard.Direction.Descending, Leaderboard.Direction.Descending}, new Leaderboard.ColumnType[]{Leaderboard.ColumnType.Float, Leaderboard.ColumnType.Xp, Leaderboard.ColumnType.MinutesToHours});

                Leaderboard unrankedLb = new Leaderboard("1v1 Duel Winstreak", player, 57, 105, 0.5, Leaderboard.Direction.Descending, Leaderboard.ColumnType.Integer);
                Leaderboard pvpLb = new Leaderboard("PvP Winstreak", player, 57, 105, 3.5, Leaderboard.Direction.Descending, Leaderboard.ColumnType.Integer);
                unrankedLb.addClickable(new String[]{"All Time", "Current"}, new String[]{"unrankedAllTimeWinstreak", "unrankedCurrentWinStreak"}, new Leaderboard.Direction[]{Leaderboard.Direction.Descending, Leaderboard.Direction.Descending, Leaderboard.Direction.Descending}, new Leaderboard.ColumnType[]{Leaderboard.ColumnType.Integer, Leaderboard.ColumnType.Integer, Leaderboard.ColumnType.Integer});
                pvpLb.addClickable(new String[]{"All Time", "Current"}, new String[]{"pvpAllTimeWinstreak", "pvpCurrentWinStreak"}, new Leaderboard.Direction[]{Leaderboard.Direction.Descending, Leaderboard.Direction.Descending, Leaderboard.Direction.Descending}, new Leaderboard.ColumnType[]{Leaderboard.ColumnType.Integer, Leaderboard.ColumnType.Integer, Leaderboard.ColumnType.Integer});
                clickableLeaderboards.put(player.getUniqueId(), new Leaderboard[]{otherLb, unrankedLb, pvpLb});
            }
        }).runTaskLater(this, 10);

        if(player.hasPermission("bridgepractice.lobby.effect.fly")) {
            player.setAllowFlight(true);
            player.setFlySpeed(0.07f);
        }

        // spigot is *so dumb*:
        // join messages just ARENT SENT when using bungeecord
        // WTF???
        // like...... WTF????????????
        // so now we have to use this awful hack.
        String joinMessage = null;
        if(player.hasPermission("bridgepractice.lobby.announce")) {
            if(player.hasPermission("bridgepractice.lobby.announce.loud")) {
                joinMessage = " §b>§c>§a> "+player.getCustomName()+" §6joined the lobby! §a<§c<§b<";
            } else {
                joinMessage = player.getCustomName()+" §6joined the lobby!";
            }
        }
        if(joinMessage != null) {
            player.sendMessage(joinMessage);
            for(Player p : player.getWorld().getPlayers()) {
                p.sendMessage(joinMessage);
            }
        }

    }
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if(!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if(subchannel.equals("PlayerCount")) {
            String server = in.readUTF(); // Name of server, as given in the arguments
            int playerCount = in.readInt();
            if(server.equals("ALL")) {
                player.getScoreboard().getTeam("total").setSuffix("§a" + playerCount);
            } else if(server.equals("lobby")) {
                player.getScoreboard().getTeam("game").setSuffix("§a" + (Integer.parseInt(player.getScoreboard().getTeam("total").getSuffix().substring(2)) - playerCount));
            }
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        playersHidingPlayers.remove(player);
        lastPlayerVisibilityChanges.remove(uuid);
        lastPlayerHotbarEdits.remove(uuid);
        lastPlayerGetFromHypixel.remove(uuid);
        respawnLocation.remove(uuid);
        playerNpcTimes.remove(uuid);
        lastShop.remove(uuid);
        lastSpade.remove(uuid);
        lastTele.remove(uuid);
        currentGadgets.remove(uuid);

        event.setQuitMessage("");
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        Inventory inv = player.getInventory();
        if(player.getLocation().getY() < 80 || player.getLocation().getY() > 150
        || player.getLocation().getX() < -40 || player.getLocation().getX() > 80
        || player.getLocation().getZ() < -40 || player.getLocation().getZ() > 40 ) {

                player.teleport(respawnLocation.getOrDefault(player.getUniqueId(), new Location(player.getWorld(), 2.5, 99, 0.5, -90, 0)));
        }

        //SWITCH SPADE NAME ON EXIT
        ItemStack leaderboardsSpade = Utils.getUnbreakable(Utils.makeItem(Material.GOLD_SPADE, "§6Teleport to Leaderboards §7(Right Click)", "§7View the leaderboards"));
        ItemStack spawnSpade = Utils.getUnbreakable(Utils.makeItem(Material.GOLD_SPADE, "§6Teleport to Spawn §7(Right Click)", "§7Go back to spawn"));

        if(player.getLocation().getX() > 43 && player.getLocation().getX() < 66
        && player.getLocation().getZ() > -10 && player.getLocation().getZ() < 9 ) {
            if (!inv.getItem(8).equals(spawnSpade)) {
                inv.setItem(8, spawnSpade);
            }
        }else {
            if (!inv.getItem(8).equals(leaderboardsSpade)) {
                inv.setItem(8, leaderboardsSpade);
            }
        }
    }
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }
    @EventHandler
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if(item.getType() == Material.COOKIE) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5*20, 2, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 5*20, 1, false, false));
            (new BukkitRunnable() {
                @Override
                public void run() {
                    player.setFoodLevel(19);
                    giveGadgets(player, player.getInventory());
                }
            }).runTaskLater(this, 1);
        }
    }
    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if(event.getAction() == Action.PHYSICAL) {
            Block plate = event.getClickedBlock();
            if(plate.getType() == Material.GOLD_PLATE) {
                player.sendMessage("§aStarted parkour '" + plate.getMetadata("parkour").get(0).asString() + "'");
                respawnLocation.put(player.getUniqueId(), (Location) plate.getMetadata("parkour_respawn_location").get(0).value());
                PlayerInventory inv = player.getInventory();
                inv.setItem(4, Utils.makeItem(Material.IRON_PLATE, "§aTeleport to Last Checkpoint"));
                inv.setItem(5, Utils.makeItem(Material.BED, "§cCancel Parkour"));
                inv.setHeldItemSlot(4);
            }
            return;
        }

        if(event.getItem() == null) return;

        switch(event.getMaterial()) {
            case COMPASS:
                player.openInventory(gameMenu.getInventory());
                break;
            case SKULL_ITEM:
                showStats(player);
                break;
            case EMERALD:
                if(System.currentTimeMillis() - lastShop.getOrDefault(player.getUniqueId(), 0L) > 1000) {
                    lastShop.put(player.getUniqueId(), System.currentTimeMillis());
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            Cage cage;
                            try(PreparedStatement statement = connection.prepareStatement("SELECT cage FROM players WHERE uuid=?;")) {
                                statement.setString(1, player.getUniqueId().toString()); // uuid
                                ResultSet res = statement.executeQuery();
                                if(!res.next()) {
                                    throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                                }
                                cage = Cage.values()[res.getInt("cage")];
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                                player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
                                return;
                            }
                            Menu shop = new Menu("BridgePractice Shop", 4, true,
                                    MenuItem.close(3, 4),

                                    new MenuItem(1, 3, Utils.makeItem(Material.EMERALD, "§aStore", "§cComing soon!", "", "§7Purchase a rank to help", "§7support the server!", "", "§eRight click to Visit"), (p, m) -> {
                                        m.allowForGarbageCollection();
                                        p.closeInventory();
                                        sendToStore(p);
                                    }),//menu item end
                                    new MenuItem(1, 5, Utils.makeItem(Material.MOB_SPAWNER, "§aCages", "§7Select a cage to spawn within", "§7before each round of The Bridge.", "", "§7Currently Selected:", "§a" + cage.toString(), "", "§eClick to View!"), (p, m) -> {
                                        m.allowForGarbageCollection();
                                        Menu cages = new Menu("§rChoose a cage", 4, true,
                                                MenuItem.close(3, 4),

                                                new MenuItem(1, 1, Utils.makeItem(Material.BARRIER, "§eDefault", "§7Selecting this option sets", "§7your cage to the default.", "", "§eClick to Select!"), (p2, m2) -> {
                                                    setCage(p2, m2, Cage.Default);
                                                }),
                                                new MenuItem(1, 2, Utils.makeItem(Material.BED, "§aBed", "§7Selects the Bed cage.", "", "§eClick to Select!"), (p2, m2) -> {
                                                    setCage(p2, m2, Cage.Bed);
                                                }),
                                                new MenuItem(1, 3, Utils.makeItem(Material.RED_ROSE, "§aFlower", "§7Selects the Flower cage.", "", "§eClick to Select!"), (p2, m2) -> {
                                                    setCage(p2, m2, Cage.Flower);
                                                }),
                                                new MenuItem(1, 4, Utils.makeItem(Material.EYE_OF_ENDER, "§aTemple", "§7Selects the Temple cage.", "", "§eClick to Select!"), (p2, m2) -> {
                                                    setCage(p2, m2, Cage.Temple);
                                                }),
                                                new MenuItem(1, 5, Utils.makeItem(Material.BOAT, "§aSailboat", "§7Selects the Sailboat cage.", "", "§eClick to Select!"), (p2, m2) -> {
                                                    setCage(p2, m2, Cage.Sailboat);
                                                }),
                                                new MenuItem(1, 6, Utils.makeItem(Material.RED_MUSHROOM, "§aMushroom", "§7Selects the Mushroom cage.", "", "§eClick to Select!"), (p2, m2) -> {
                                                    setCage(p2, m2, Cage.Mushroom);
                                                })
                                        );
                                        p.openInventory(cages.getInventory());
                                    })
                            );
                            player.openInventory(shop.getInventory());
                        }
                    }).runTaskAsynchronously(this);
                } else {
                    player.sendMessage("§cYou must wait §e1s§c between uses!");
                }
                break;
            case INK_SACK:
                if(!playersHidingPlayers.contains(player)) {
                    if(System.currentTimeMillis() - lastPlayerVisibilityChanges.getOrDefault(player.getUniqueId(), 0L) > 3000) {
                        // add to list, loop through all online and hide, say players are hidden
                        playersHidingPlayers.add(player);
                        for(Player p : getServer().getOnlinePlayers()) {
                            player.hidePlayer(p);
                        }
                        player.getInventory().setItem(7, Utils.makeDyed(Material.INK_SACK, DyeColor.SILVER, "§fPlayers: §cHidden §7(Right Click)", "§7Right click to toggle player visibility!"));
                        lastPlayerVisibilityChanges.put(player.getUniqueId(), System.currentTimeMillis());
                    } else {
                        player.sendMessage("§cYou must wait §e3s§c between uses!");
                    }
                } else {
                    if(System.currentTimeMillis() - lastPlayerVisibilityChanges.getOrDefault(player.getUniqueId(), 0L) > 3000) {
                        // remove from list, loop through all online and show, say players are shown
                        playersHidingPlayers.remove(player);
                        for(Player p : getServer().getOnlinePlayers()) {
                            player.showPlayer(p);
                        }
                        player.getInventory().setItem(7, Utils.makeDyed(Material.INK_SACK, DyeColor.PURPLE, "§fPlayers: §aShown §7(Right Click)", "§7Right click to toggle player visibility!"));
                        lastPlayerVisibilityChanges.put(player.getUniqueId(), System.currentTimeMillis());
                    } else {
                        player.sendMessage("§cYou must wait §e3s§c between uses!");
                    }
                }
                break;
            case GOLD_SPADE:
                double timeSinceSpade = System.currentTimeMillis() - lastSpade.getOrDefault(player.getUniqueId(), 0L);
                if (timeSinceSpade > 500) {
                    lastSpade.put(player.getUniqueId(), System.currentTimeMillis());
                    Inventory inven = player.getInventory();
                    if (player.getLocation().getX() > 43 && player.getLocation().getX() < 66
                            && player.getLocation().getZ() > -10 && player.getLocation().getZ() < 9) {
                        player.teleport(new Location(player.getWorld(), 2.5, 99, 0.5, -90, 0));
                        inven.setItem(8, Utils.getUnbreakable(Utils.makeItem(Material.GOLD_SPADE, "§6Leaderboards §7(Right Click)", "§7View the leaderboards")));
                    } else {
                        player.teleport(new Location(player.getWorld(), 50.5, 103, 0.5, -90, 0));
                        inven.setItem(8, Utils.getUnbreakable(Utils.makeItem(Material.GOLD_SPADE, "§6Spawn §7(Right Click)", "§7Go back to spawn")));
                    }
                } else if(timeSinceSpade > 10){
                    player.sendMessage("§cYou must wait §e0.5s§c between uses!");
                }
                break;
            case BOOK:
                if(System.currentTimeMillis() - lastPlayerHotbarEdits.getOrDefault(player.getUniqueId(), 0L) > 3000) {
                    lastPlayerHotbarEdits.put(player.getUniqueId(), System.currentTimeMillis());
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            Menu editor = new Menu("Hotbar Editor", 6, true,
                                    MenuItem.blocker(3, 0), MenuItem.blocker(3, 1), MenuItem.blocker(3, 2), MenuItem.blocker(3, 3),
                                    MenuItem.blocker(3, 4), MenuItem.blocker(3, 5), MenuItem.blocker(3, 6), MenuItem.blocker(3, 7), MenuItem.blocker(3, 8),

                                    MenuItem.close(5, 3),
                                    new MenuItem(5, 4, Utils.makeItem(Material.CHEST, "§aSave Layout", "§7Save your hotbar", "§7layout."), (p, m) -> {
                                        (new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                try(PreparedStatement statement = connection.prepareStatement("UPDATE players SET hotbarCustomized = TRUE, hotbarSword = ?, hotbarBow = ?, hotbarPickaxe = ?, hotbarBlocksOne = ?, hotbarBlocksTwo = ?, hotbarGoldenApple = ?, hotbarArrow = ?, hotbarGlyph = ? WHERE uuid=?;")) {
                                                    Inventory inv = m.getInventory();
                                                    boolean hasSeenBlocks = false;
                                                    int itemsFound = 0;
                                                    for(int i = 0; i < 9 * 5; i++) {
                                                        ItemStack item = inv.getItem(i);
                                                        if(item != null && item.getType() != Material.STAINED_GLASS_PANE) {
                                                            int rowIncludingSeparation = 4 - ((int) Math.floor(i / 9f));
                                                            int row = rowIncludingSeparation == 0 ? 0 : 4 - (rowIncludingSeparation - 1);
                                                            int index = row * 9 + (i % 9);
                                                            switch(item.getType()) {
                                                                case IRON_SWORD:
                                                                    statement.setInt(1, index);
                                                                    itemsFound++;
                                                                    break;
                                                                case BOW:
                                                                    statement.setInt(2, index);
                                                                    itemsFound++;
                                                                    break;
                                                                case DIAMOND_PICKAXE:
                                                                    statement.setInt(3, index);
                                                                    itemsFound++;
                                                                    break;
                                                                case STAINED_CLAY:
                                                                    if(!hasSeenBlocks) {
                                                                        hasSeenBlocks = true;
                                                                        statement.setInt(4, index);
                                                                    } else {
                                                                        statement.setInt(5, index);
                                                                    }
                                                                    itemsFound++;
                                                                    break;
                                                                case GOLDEN_APPLE:
                                                                    statement.setInt(6, index);
                                                                    itemsFound++;
                                                                    break;
                                                                case ARROW:
                                                                    statement.setInt(7, index);
                                                                    itemsFound++;
                                                                    break;
                                                                case DIAMOND:
                                                                    statement.setInt(8, index);
                                                                    itemsFound++;
                                                                    break;
                                                            }
                                                        }
                                                    }
                                                    if(itemsFound != 8) {
                                                        m.allowForGarbageCollection();
                                                        p.closeInventory();
                                                        p.sendMessage("§c✕ Your inventory didn't look right!");
                                                        return;
                                                    }
                                                    statement.setString(9, player.getUniqueId().toString()); // uuid, set to player uuid
                                                    statement.executeUpdate();
                                                    m.allowForGarbageCollection();
                                                    p.closeInventory();
                                                    p.sendMessage("§a✔ Successfully saved your hotbar layout.");
                                                } catch (SQLException throwables) {
                                                    throwables.printStackTrace();
                                                    player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your time!");
                                                }
                                            }
                                        }).runTaskAsynchronously(instance);
                                    }),
                                    new MenuItem(5, 5, Utils.makeItem(Material.EXPLOSIVE_MINECART, "§cReset Layout", "§7Reset your hotbar layout", "§7to the default"), (p, m) -> {
                                        updateHotbarEditor(m, defaultHotbar());
                                    }),
                                    new MenuItem(5, 7, Utils.makeItem(Material.EXP_BOTTLE, "§aGet From Hypixel", "§7Get your hotbar layout", "§7from Hypixel."), (p, m) -> {
                                        if(System.currentTimeMillis() - lastPlayerGetFromHypixel.getOrDefault(player.getUniqueId(), 0L) > 3000) {
                                            lastPlayerGetFromHypixel.put(player.getUniqueId(), System.currentTimeMillis());
                                            (new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        HotbarItem[] items = getHotbarFromHypixelSync(player);
                                                        updateHotbarEditor(m, items);
                                                        player.sendMessage("§aSuccessfully got your hotbar from Hypixel.");
                                                    } catch (IOException | NullPointerException e) {
                                                        p.sendMessage("§cThere was an error getting your hotbar from Hypixel! Are you banned or have never logged on?");
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }).runTaskAsynchronously(instance);
                                        } else {
                                            p.sendMessage("§cYou must wait §e3s§c between uses!");
                                        }
                                    })
                            );
                            HotbarItem[] hotbar = getHotbarSync(player);
                            updateHotbarEditor(editor, hotbar);
                            player.openInventory(editor.getInventory());
                        }
                    }).runTaskAsynchronously(this);
                } else {
                    player.sendMessage("§cYou must wait §e3s§c between uses!");
                }
                break;
            case IRON_PLATE:
                player.teleport(respawnLocation.get(player.getUniqueId()));
                break;
            case BED:
                respawnLocation.remove(player.getUniqueId());
                PlayerInventory inv = player.getInventory();
                giveGadgets(player, inv);
                player.sendMessage("§cParkour canceled.");
                break;

            case BLAZE_ROD:

                if (System.currentTimeMillis() - lastTele.getOrDefault(player.getUniqueId(), 0L) > 500) {
                    lastTele.put(player.getUniqueId(), System.currentTimeMillis());

                    double blockTeleDistance = 8d;
                    Vector playerLookDir = player.getEyeLocation().getDirection();
                    Location raycastPoint = null;
                    Location lastRaycastPoint = null;

                    for (double d = 0; d <= blockTeleDistance; d += 0.5) {
                        //get raycast ad a distance of D
                        raycastPoint = player.getEyeLocation().clone().add(playerLookDir.clone().multiply(d));

                        //packets for making a flame
                        Location particalLoc = raycastPoint.clone();
                        particalLoc.setY(particalLoc.getY() - 1.5d);
                        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.FLAME, true, (float) (particalLoc.getX()), (float) (particalLoc.getY()), (float) (particalLoc.getZ()), 0, 0, 0, 0, 1);
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            ((CraftPlayer) online).getHandle().playerConnection.sendPacket(packet);
                        }

                        //check block above and below
                        Location blockAbove = raycastPoint.clone();
                        blockAbove.setY(raycastPoint.getY() + 1);
                        Location blockBelow = raycastPoint.clone();
                        blockBelow.setY(raycastPoint.getY() - 1);

                        if (raycastPoint.getBlock().getType().equals(Material.AIR)) {
                            if (blockAbove.getBlock().getType().equals(Material.AIR)) {
                                lastRaycastPoint = raycastPoint;
                            } else if (blockBelow.getBlock().getType().equals(Material.AIR)) {
                                raycastPoint = blockBelow;
                                lastRaycastPoint = raycastPoint;
                            } else {//break out and go to last able location
                                raycastPoint = lastRaycastPoint;
                                break;
                            }
                        } else {
                            raycastPoint = lastRaycastPoint;
                            break;
                        }
                    }//block checking for loop

                    //loop broken or gone 5 blocks
                    raycastPoint.setY(raycastPoint.getBlock().getLocation().getY() + 0.5);
                    raycastPoint.setX(raycastPoint.getBlock().getLocation().getX() + 0.5);
                    raycastPoint.setZ(raycastPoint.getBlock().getLocation().getZ() + 0.5);
                    player.teleport(raycastPoint);

                } else {
                    player.sendMessage("§cYou're clicking to fast!");
                }
                break;
        }
    }
    private final HashMap<UUID, Gadget> currentGadgets = new HashMap<>();
    static class Gadget {
        ItemStack fourthSlotItem;
        ItemStack fifthSlotItem;
        public Gadget(ItemStack fourthSlotItem, ItemStack fifthSlotItem) {
            this.fourthSlotItem = fourthSlotItem;
            this.fifthSlotItem = fifthSlotItem;
        }
    }
    public Gadget getGadget(Player player) {
        return currentGadgets.get(player.getUniqueId());
    }
    public void setGadget(Player player, Gadget gadget) {
        removeGadgetEffects(player);
        currentGadgets.put(player.getUniqueId(), gadget);
    }
    public void giveGadgets(Player player, Inventory inv) {
        Gadget gadget = getGadget(player);
        if(gadget == null) {
            ItemStack fourthSlot = hotbarLayoutItem;
            ItemStack fifthSlot;
            if(player.hasPermission("bridgepractice.lobby.diamondduelitem")) {
                fifthSlot = duelPlayerItemDiamond;
            } else if(player.hasPermission("bridgepractice.lobby.goldduelitem")) {
                fifthSlot = duelPlayerItemGold;
            } else {
                fifthSlot = duelPlayerItemIron;
            }
            setGadget(player, new Gadget(fourthSlot, fifthSlot));

            inv.setItem(4, fourthSlot);
            inv.setItem(5, fifthSlot);
        } else {
            inv.setItem(4, gadget.fourthSlotItem);
            inv.setItem(5, gadget.fifthSlotItem);
        }
    }
    private void updateHotbarEditor(Menu m, HotbarItem[] items) {
        for(int i = 0; i < 9 * 3; i++) {
            m.removeItem(i);
        }
        for(int i = 0; i < 9; i++) {
            m.removeItem(36 + i);
        }
        for(HotbarItem hotbarItem : items) {
            int row = 4 - ((int) Math.floor(hotbarItem.index / 9f));
            m.addDraggableItem(new MenuItem(row == 4 ? row : 3 - row, hotbarItem.index % 9, hotbarItem.item, null));
        }
    }
    @EventHandler
    public void onPlayerDamagedBySelf(EntityDamageEvent event) {
        // disable drown, fall, etc damage
        if(event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }
    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player damaged = (Player) event.getEntity();
            Material type = damager.getItemInHand().getType();
            if(type == Material.IRON_SWORD || type == Material.GOLD_SWORD || type == Material.DIAMOND_SWORD) {
                // duel
                Menu duelMenu = new Menu("Duel " + damaged.getName(), 4, true,
                        new MenuItem(1, 3, Utils.makeDyed(Material.STAINED_CLAY, DyeColor.BLUE, "§aBridge 1v1", "§7Invite §a" + damaged.getName() + "§7 to", "§7a 1v1 duel.", "", "§eCLICK TO DUEL"), (p, m) -> {
                            m.allowForGarbageCollection();
                            p.closeInventory();
                            Utils.sendDuelRequest(p, damaged, "bridge");
                        }),
                        new MenuItem(1, 5, Utils.makeItem(Material.IRON_BOOTS, "§aBridge PvP Duel", "§7Invite §a" + damaged.getName() + "§7 to", "§7a Bridge PvP duel.", "", "§eCLICK TO DUEL"), (p, m) -> {
                            m.allowForGarbageCollection();
                            p.closeInventory();
                            Utils.sendDuelRequest(p, damaged, "pvp");
                        }),
                        MenuItem.close(3, 4)
                );
                damager.openInventory(duelMenu.getInventory());
            }
        }
        event.setCancelled(true);
    }
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(event.toWeatherState());
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        if(event.getWhoClicked().getGameMode() == GameMode.CREATIVE && event.getClickedInventory() != null && event.getWhoClicked().getInventory().equals(event.getClickedInventory())) return;
        event.setCancelled(true);
        try {
            if(inv != null) {
                Menu menuClicked = Menu.menus.get(inv.getTitle());
                if(menuClicked != null) {
                    if(menuClicked.draggables.contains(event.getCursor().getType()) && menuClicked.doesHaveOnClick(event.getSlot())) {
                        return;
                    }
                    if((menuClicked.draggables.contains(event.getCurrentItem().getType()) || menuClicked.draggables.contains(event.getCursor().getType())) && event.getHotbarButton() == -1 && !event.isShiftClick()) {
                        event.setCancelled(false);
                    }
                    menuClicked.runOnClick(event.getSlot(), (Player) event.getWhoClicked());
                }
            }
        } catch(Exception e) {
            // we do this so it is still canceled if something goes wrong
            e.printStackTrace();
        }

    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
