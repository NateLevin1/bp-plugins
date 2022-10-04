package net.bridgepractice.bridgepracticeclub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CommandWing implements CommandExecutor {
    HashMap<UUID, Team> leaderTeams = new HashMap<>();
    HashMap<String, Float> playerTimes = new HashMap<>();
    public static BlockState[][][] islandContentDefault;
    public static BlockState[][][] islandContentMagma;
    public static BlockState[][][] islandContentModern;
    public static BlockState[][][] islandContentAquatic;
    public static BlockState[][][] islandContentNightLight;
    public static BlockState[][][] islandContentPalace;
    public static BlockState[][][] islandContentSeptic;

    public static BlockState[][][] landingContentDefault;
    public static BlockState[][][] landingContentMagma;
    public static BlockState[][][] landingContentModern;
    public static BlockState[][][] landingContentAquatic;
    public static BlockState[][][] landingContentNightLight;
    public static BlockState[][][] landingContentPalace;
    public static BlockState[][][] landingContentSeptic;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo oldInfo = Bridge.instance.getPlayer(player.getUniqueId());
            if(oldInfo != null && oldInfo.location == PlayerLocation.Wing) {
                player.sendMessage("§cYou are already practicing winging!");
                return true;
            }

            if(Bridge.disabledGames.getOrDefault("wing", false)) {
                player.sendMessage("§cQueueing for that game has been temporarily disabled");
                return true;
            }

            boolean wasQueueNeeded = PlayerInfo.addToQueueIfNeeded(player, PlayerLocation.Wing);
            if(wasQueueNeeded) {
                return true;
            }

            PlayerInfo.askToLeaveQueue(player);

            final long[] time = {0}; // this is a hack but it works

            Scoreboard board = Bridge.createScoreboard("    §b§eWing Practice     ", new String[] {
                    "",
                    " §l§bTime",
                    "%time%§e 0",
                    "",
                    " §l§aBlocks / Downstack",
                    "%blocks%§e 0 / 0",
                    "",
                    " §l§dSession Leader",
                    "%leader%§e None",
                    "",
                    " §l§9Personal Best  ",
                    "%pb%§e None",
                    "",
                    "   §7bridgepractice.net  "
            });
            Bridge.setScoreboard(player, board);
            updateSessionLeader();
            leaderTeams.put(player.getUniqueId(), board.getTeam("leader"));

            final boolean[] isHeightValidForScoreToCount = {true};
            Structure spawn = new Structure(Bridge.deepClone(islandContentDefault)); // FIXME: we deep clone every time to prevent .flipX changing the content array, but this is not ideal for performance
            Structure landing = new Structure(Bridge.deepClone(landingContentDefault));

            final ArrowShoot[] shootArrow = {null};

            ArrayList<SettingsMenu.Entry> entries = new ArrayList<>(Arrays.asList(new SettingsMenu.Entry(1, 1, Bridge.makeItem(Material.STAINED_CLAY, 1, "Use Blue Clay",  new String[]{"Sets the color of clay to bridge","with to §9blue§7."}, 11), "block_color", true), // see DyeColor.class
                    new SettingsMenu.Entry(2, 1, Bridge.makeItem(Material.STAINED_CLAY, 1, "Use White Clay", new String[]{"Sets the color of clay to bridge","with to §fwhite§7."}, 0), "block_color", false),
                    new SettingsMenu.Entry(3, 1, Bridge.makeItem(Material.STAINED_CLAY, 1, "Use Red Clay",   new String[]{"Sets the color of clay to bridge","with to §cred§7."}, 14),  "block_color", false),

                    new SettingsMenu.Entry(1, 7, Bridge.makeItem(Material.STAINED_GLASS_PANE, 1, "Don't shoot arrows", new String[]{"Will §cnot§7 shoot arrows at you as","you bridge."}, 14),             "shoot_arrows", true),
                    new SettingsMenu.Entry(2, 7, Bridge.makeItem(Material.ARROW, 1, "Do shoot arrows",    new String[]{"Will shoot arrows at you as","you bridge to practice","under bow pressure."}, -1), "shoot_arrows", false),

                    new SettingsMenu.Entry(1, 3, Bridge.makeItem(Material.GOLD_PLATE, 1, "0",  new String[]{"Start at height limit"}, -1),             "height", true),
                    new SettingsMenu.Entry(2, 3, Bridge.makeItem(Material.IRON_PLATE, 1, "-1", new String[]{"Start 1 block below height limit","(does §cnot§7 count towards","session leader or PB)"},-1),    "height", false),
                    new SettingsMenu.Entry(3, 3, Bridge.makeItem(Material.STONE_PLATE, 1, "-5", new String[]{"Start 5 blocks below height limit","(does §cnot§7 count towards","session leader or PB)"}, -1), "height", false),

                    new SettingsMenu.Entry(1, 5, Bridge.makeItem(Material.REDSTONE_COMPARATOR, 1, "Bridge to the left",  new String[]{"Set your bridging direction","to the left."}, -1), "dir", true),
                    new SettingsMenu.Entry(2, 5, Bridge.makeItem(Material.DIODE, 1, "Bridge to the right", new String[]{"Set your bridging direction","to the right"}, -1), "dir", false)));
            if (player.hasPermission("group.legend")){
                entries.add(new SettingsMenu.Entry(5, 1, Bridge.makeItem(Material.IRON_BLOCK, 1, "Default Island",   new String[]{"Select the §edefault§7","wing island."}, -1),  "islands", true));
                entries.add(new SettingsMenu.Entry(5, 2, Bridge.makeItem(Material.WOOL, 1, "Magma Island",   new String[]{"Select the §cMagma§7","wing island."}, 1),  "islands", false));
                entries.add(new SettingsMenu.Entry(5, 3, Bridge.makeItem(Material.PRISMARINE, 1, "Aquatic Island",   new String[]{"Select the §9Aquatic§7","wing island."}, -1),  "islands", false));
                entries.add(new SettingsMenu.Entry(5, 4, Bridge.makeItem(Material.QUARTZ, 1, "Modern Island",   new String[]{"Select the §fModern§7","wing island."}, -1),  "islands", false));
                entries.add(new SettingsMenu.Entry(5, 5, Bridge.makeItem(Material.REDSTONE_LAMP_OFF, 1, "Night Light Island",   new String[]{"Select the §6Night Light§7","wing island."}, -1),  "islands", false));
                entries.add(new SettingsMenu.Entry(5, 6, Bridge.makeItem(Material.SLIME_BLOCK, 1, "Septic Island",   new String[]{"Select the §aSeptic§7","wing island."}, -1),  "islands", false));
                entries.add(new SettingsMenu.Entry(5, 7, Bridge.makeItem(Material.DIAMOND_BLOCK, 1, "Palace Island",   new String[]{"Select the §bPalace§7","wing island."}, -1),  "islands", false));
            }
            SettingsMenu menu = new SettingsMenu(
                    entries.toArray(new SettingsMenu.Entry[0]),
                    6,
                    "Wing Settings",
                    (itemClicked, groupName)->{
                        PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
                        if(groupName != null) {
                            switch (groupName) {
                                case "block_color":
                                    int dyeColor = itemClicked.getDurability();
                                    if (info.locSettings.clayColor != dyeColor) {
                                        info.locSettings.clayColor = dyeColor;
                                        updateMap(info, spawn, landing);
                                    }
                                    info.onDeath.call(info);
                                    break;
                                case "height":
                                    if (itemClicked.getType() == Material.GOLD_PLATE && info.locSettings.height != 99) {
                                        info.locSettings.height = 99;
                                        isHeightValidForScoreToCount[0] = true;
                                    } else if (itemClicked.getType() == Material.IRON_PLATE && info.locSettings.height != 98) {
                                        info.locSettings.height = 98;
                                        isHeightValidForScoreToCount[0] = false;
                                    } else if (itemClicked.getType() == Material.STONE_PLATE && info.locSettings.height != 94) {
                                        info.locSettings.height = 94;
                                        isHeightValidForScoreToCount[0] = false;
                                    }
                                    if (!isHeightValidForScoreToCount[0]) {
                                        Bridge.sendActionBar(player, "§cYour attempt times will §lnot be counted§r§c because you are using an altered height.");
                                    }
                                    updateMap(info, spawn, landing);
                                    info.respawnLocation.setY(info.locSettings.height + 1);
                                    info.onDeath.call(info);
                                    break;
                                case "dir":
                                    if (itemClicked.getType() == Material.REDSTONE_COMPARATOR && !info.locSettings.isBridgingLeft) {
                                        // left
                                        info.locSettings.isBridgingLeft = true;
                                        info.respawnLocation.setX(info.winBox.relXZ[0] + 0.5);
                                        info.winBox.xStart = 4;
                                        info.winBox.xEnd = 11;
                                        info.allowedPlacing = new AllowedLocation[]{ new AllowedLocation(info.allowedPlacing[0].relXZ, 0, 9, 0, 5), new AllowedLocation(info.allowedPlacing[0].relXZ, 5, 11, 4, 23) };
                                    } else if (itemClicked.getType() == Material.DIODE && info.locSettings.isBridgingLeft) {
                                        // right
                                        info.locSettings.isBridgingLeft = false;
                                        info.respawnLocation.setX(info.winBox.relXZ[0] + 8.5);
                                        info.winBox.xStart = -2;
                                        info.winBox.xEnd = 5;
                                        info.allowedPlacing = new AllowedLocation[]{ new AllowedLocation(info.allowedPlacing[0].relXZ, -1, 8, 0, 5), new AllowedLocation(info.allowedPlacing[0].relXZ, -1, 3, 4, 23) };
                                    }
                                    updateMap(info, spawn, landing);

                                    info.onDeath.call(info);
                                    break;
                                case "shoot_arrows":
                                    if(itemClicked.getType() == Material.STAINED_GLASS_PANE && shootArrow[0] != null) {
                                        shootArrow[0].cancel();
                                        shootArrow[0] = null;
                                    } else if(itemClicked.getType() == Material.ARROW) {
                                        if(shootArrow[0] != null) {
                                            shootArrow[0].cancel();
                                        }

                                        shootArrow[0] = new ArrowShoot(time, player);
                                        shootArrow[0].runTaskTimer(Bridge.instance, 10, 20+(ThreadLocalRandom.current().nextInt(0, 40)));
                                    }
                                    info.onDeath.call(info);
                                    break;
                                case "islands":
                                    switch (itemClicked.getType()){
                                        case IRON_BLOCK:
                                            setWingIsland(islandContentDefault, spawn, landingContentDefault, landing, info);
                                            break;
                                        case WOOL:
                                            setWingIsland(islandContentMagma, spawn, landingContentMagma, landing, info);
                                            break;
                                        case PRISMARINE:
                                            setWingIsland(islandContentAquatic, spawn, landingContentModern, landing, info);
                                            break;
                                        case QUARTZ:
                                            setWingIsland(islandContentModern, spawn, landingContentAquatic, landing, info);
                                            break;
                                        case REDSTONE_LAMP_OFF:
                                            setWingIsland(islandContentNightLight, spawn, landingContentNightLight, landing, info);
                                            break;
                                        case SLIME_BLOCK:
                                            setWingIsland(islandContentSeptic, spawn, landingContentSeptic, landing, info);
                                            break;
                                        case DIAMOND_BLOCK:
                                            setWingIsland(islandContentPalace, spawn, landingContentPalace, landing, info);
                                            break;

                                    }
                                    break;
                            }
                        }
                    }
            );

            // get PB from the DB and show it to the player
            float pb = 0;
            try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT wingPB FROM players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString()); // uuid
                ResultSet res = statement.executeQuery();
                if(!res.next()) {
                    throw new SQLException("Did not get a row from the database. Player name: "+player.getName()+" Player UUID: "+player.getUniqueId());
                }
                // display it if it exists, otherwise display "None"
                pb = res.getFloat(1); // 1 indexing!
                if(!res.wasNull()) {
                    board.getTeam("pb").setPrefix("§e "+Bridge.prettifyNumber(pb));
                    float finalPb1 = pb;
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            board.getTeam("pb").setSuffix(" §b#"+Leaderboard.getPlace("wingPB", finalPb1, Leaderboard.Direction.Ascending));
                        }
                    }).runTaskAsynchronously(Bridge.instance);
                } else {
                    // intro message
                    BukkitRunnable intro = new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("\n§6"+new String(new char[54]).replace("\0", "-"));
                            player.sendMessage("\nLooks like it's your first time playing §aWing Practice§f!");
                            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                            BukkitRunnable a = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("\nThe goal is to bridge to the other side and jump down onto the urban side.");
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                }
                            };
                            BukkitRunnable b = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("\nTo do this, you should bridge diagonally at least 5 times (10 total blocks),");
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                }
                            };
                            BukkitRunnable c = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("\nThen bridge normally to the end. Have fun!");
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                    player.sendMessage("\n§6"+new String(new char[54]).replace("\0", "-"));
                                }
                            };
                            a.runTaskLater(Bridge.instance, 3*20);
                            b.runTaskLater(Bridge.instance, 6*20);
                            c.runTaskLater(Bridge.instance, 9*20);
                        }
                    };

                    intro.runTaskLater(Bridge.instance, 10);

                    board.getTeam("pb").setPrefix("§e None");
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
            }

            final TimeUpdater[] tu = {null};
            final float[] finalPb = {pb};
            Bridge.instance.setPlayer(player.getUniqueId(), new PlayerInfo(PlayerLocation.Wing, menu, (info) -> {
                // on death
                if (tu[0] != null) {
                    tu[0].cancel();
                }
                new ResetBridgePlayer(player, info, true, true).run();
                time[0] = 0;
            }, (info) -> {
                // on block change
                if(time[0] == 0) {
                    time[0] = System.currentTimeMillis();
                    tu[0] = new TimeUpdater(time, board, player);
                    tu[0].runTaskTimer(Bridge.instance, 0, 4);
                }

                int downstack = 99;
                for(Location loc : info.changedBlocks) {
                    if(loc.getY() == downstack-1) // prevent just placing blocks lower without connecting
                        downstack = (int)loc.getY();
                }
                if(isHeightValidForScoreToCount[0]) {
                    board.getTeam("blocks").setPrefix("§e "+info.changedBlocks.size()+" / "+(99-downstack));
                } else {
                    board.getTeam("blocks").setPrefix("§e "+info.changedBlocks.size()+" / §cN/A");
                }
            }, (info) -> {
                // on location change
                // delete changed blocks, remove from scoreboard list & cancel time updater
                if (tu[0] != null) {
                    tu[0].cancel();
                }
                leaderTeams.remove(player.getUniqueId());
                playerTimes.remove(player.getName());
                updateSessionLeader();
                info.locSettings = new PlayerInfo.LocSettings(); // reset to defaults
                updateMap(info, spawn, landing);
                setWingIsland(islandContentDefault, spawn, landingContentDefault, landing, info);
                if(shootArrow[0] != null) {
                    shootArrow[0].cancel();
                }
                for(Location loc : info.changedBlocks) {
                    loc.getBlock().setType(Material.AIR);
                }
            }, (info) -> {
                // on win
                float timeTakenNum = (System.currentTimeMillis()-time[0]);
                String timeTaken = Bridge.prettifyNumber(timeTakenNum);

                tu[0].cancel();

                board.getTeam("time").setPrefix("§e "+timeTaken);

                if(isHeightValidForScoreToCount[0]) {
                    Float oldTime = playerTimes.get(player.getName());
                    if(oldTime == null || timeTakenNum < oldTime) {
                        playerTimes.put(player.getName(), timeTakenNum);
                    }
                    updateSessionLeader();
                }

                if(((CraftPlayer)player).getHandle().ping < 250) {
                    if(timeTakenNum < 3700) {
                        info.onDeath.call(info);
                        // they are cheating. lets ban them!
                        // FIXME: this should probably use plugin messaging channels. Oh well!
                        try(PreparedStatement statement = Bridge.connection.prepareStatement("INSERT INTO commandQueue (target, type, content) VALUES ('proxy', 'excmd', ?);")) {
                            statement.setString(1, "ban "+player.getName()+" 27 IMPOSSIBLE WING TIME");
                            statement.executeUpdate();
                            return;
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                }

                player.setGameMode(GameMode.ADVENTURE);

                // publish to db if pb
                if(isHeightValidForScoreToCount[0] && (finalPb[0] == 0 || finalPb[0] > timeTakenNum)) {
                    try(PreparedStatement statement = Bridge.connection.prepareStatement("UPDATE players SET wingPB = ? WHERE uuid=?;")) {
                        statement.setFloat(1, timeTakenNum); // wingPB, set to the new PB
                        statement.setString(2, player.getUniqueId().toString()); // uuid, set to player uuid
                        statement.executeUpdate();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your PB to our database. Please open a ticket on the discord and screenshot your time!");
                    }
                    // display in #multiplayer-logs
                    sendNewPBWebhook(player, timeTaken);
                    // show to player (we don't need to go through the db at this point)
                    board.getTeam("pb").setPrefix("§e "+timeTaken);
                    Bridge.sendTitle(player, "§bNew PB! §e"+timeTaken, "§a+15§r xp");
                    finalPb[0] = timeTakenNum;
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            board.getTeam("pb").setSuffix(" §b#"+Leaderboard.getPlace("wingPB", timeTakenNum, Leaderboard.Direction.Ascending));
                        }
                    }).runTaskAsynchronously(Bridge.instance);
                } else {
                    Bridge.sendTitle(player, "§bTime: §e"+timeTaken, "§a+15§r xp");
                }

                ResetBridgePlayer rbp = new ResetBridgePlayer(player, info, true, true);
                rbp.runTaskLater(Bridge.instance, 3*20);
                time[0] = 0;

                Bridge.givePlayerXP(player, 15);
            }, null, null));

            PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
            int relX = info.winBox.relXZ[0];
            int relZ = info.winBox.relXZ[1];

            spawn.previousLocation = new Location(Bridge.instance.world, relX-3, 97, relZ-3);
            landing.previousLocation = new Location(Bridge.instance.world, relX+4, 87, relZ+23);

            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(info.respawnLocation);
            Bridge.setBridgeInventory(player, true);
            return true;
        } else {
            sender.sendMessage("You must be a player!");
        }
        return false;
    }

    private void updateSessionLeader() {
        final float[] lowestTime = {200*1000};
        final String[] lowestName = {"None"};
        playerTimes.forEach((playerName, playerTime) -> {
            if(playerTime < lowestTime[0]) {
                lowestTime[0] = playerTime;
                lowestName[0] = playerName;
            }
        });
        for(Team team : leaderTeams.values()) {
            team.setPrefix("§e "+lowestName[0].substring(0, Math.min(13, lowestName[0].length())));
            team.setSuffix((lowestTime[0] == 200*1000 ? "" : "§7: §a"+Bridge.prettifyNumber(lowestTime[0])));
        }
    }

    private void updateMap(PlayerInfo info, Structure spawn, Structure landing) {
        // rel = 97, -2

        int[] rel = info.winBox.relXZ;

        spawn.remove();
        landing.remove();
        boolean isL = info.locSettings.isBridgingLeft;
        if(isL) {
            if(spawn.isFlipped)
                spawn.flipX();
            spawn.place(new Location(Bridge.instance.world, rel[0]-3, info.locSettings.height-2, rel[1]-3));
            landing.place(new Location(Bridge.instance.world, rel[0]+4, 87, rel[1]+23));
        } else {
            // right
            if(!spawn.isFlipped)
                spawn.flipX();
            spawn.place(new Location(Bridge.instance.world, rel[0]+8, info.locSettings.height-2, rel[1]-3));
            landing.place(new Location(Bridge.instance.world, rel[0]-2, 87, rel[1]+23));
        }

        for(int y = 88; y < 90; y++) {
            for(int x = isL ? rel[0]+4 : rel[0]-2; x < (isL ? rel[0]+11 : rel[0]+5); x++) {
                for(int z = rel[1]+23; z < rel[1]+29; z++) {
                    Block block = new Location(Bridge.instance.world, x, y, z).getBlock();
                    block.setType(Material.STAINED_CLAY);
                    switch(info.locSettings.clayColor) {
                        case 11: // blue
                            block.setData((byte) 6); // pink
                            break;
                        case 14: // red
                            block.setData((byte) 3); // light blue
                            break;
                        case 0: // white
                            block.setData((byte) 15); // black
                            break;
                    }
                }
            }
        }
    }

    private void sendNewPBWebhook(Player player, String time) {
        (new BukkitRunnable() {
            @Override
            public void run() {
                JsonObject webhook = new JsonObject();
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                JsonObject author = new JsonObject();

                webhook.add("embeds", embeds);
                embeds.add(embed);

                embed.addProperty("color", 0x39c2ff);

                embed.add("author", author);
                author.addProperty("name", "New PB (Mode: Wing)");

                String playerName = player.getName();

                embed.addProperty("title", playerName + ": " + (time));

                embed.addProperty("description", playerName + " got new PB            |           on Wing Map\n"
                        + "```pascal\n"
                        + " New Time: " + time + " \n"
                        + "```");

                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", ("https://minotar.net/armor/bust/" + playerName + "/64"));
                embed.add("thumbnail", thumbnail);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", playerName + " got a new PB");
                embed.add("footer", footer);

                embed.addProperty("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

                Utils.sendWebhookSync(webhook);
            }
        }).runTaskAsynchronously(Bridge.instance);
    }

        private void setWingIsland(BlockState[][][] blockStatesIslandUncloned, Structure spawn, BlockState[][][] blockStatesLandingUncloned, Structure landing, PlayerInfo info){
            BlockState[][][] islandBlocks = Bridge.deepClone(blockStatesIslandUncloned);
            BlockState[][][] landingBlocks = Bridge.deepClone(blockStatesLandingUncloned);
            boolean isIslandFlipped = spawn.isFlipped;

            spawn.switchContent(islandBlocks);
            landing.switchContent(landingBlocks);

            if (isIslandFlipped) {
                spawn.flipX();
            }

            int[] rel = info.winBox.relXZ;
            boolean isL = info.locSettings.isBridgingLeft;
            if(isL) {
                spawn.place(new Location(Bridge.instance.world, rel[0]-3, info.locSettings.height-2, rel[1]-3));
                landing.place(new Location(Bridge.instance.world, rel[0]+4, 87, rel[1]+23));
            } else {
                // right
                spawn.place(new Location(Bridge.instance.world, rel[0]+8, info.locSettings.height-2, rel[1]-3));
                landing.place(new Location(Bridge.instance.world, rel[0]-2, 87, rel[1]+23));
            }
            info.onDeath.call(info);
        }
    }