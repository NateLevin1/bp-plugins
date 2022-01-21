package net.bridgepractice.bridgepracticeclub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.bridgepractice.RavenAntiCheat.RavenAntiCheat;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
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
import org.bukkit.util.Vector;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class CommandBypass implements CommandExecutor {
    public static BlockState[][][] earlyContent;
    public static BlockState[][][] middleContent;
    public static BlockState[][][] lateContent;
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo oldInfo = Bridge.instance.getPlayer(player.getUniqueId());
            if(oldInfo != null && oldInfo.location == PlayerLocation.Bypass) {
                player.sendMessage("§cYou are already practicing bypassing!");
                return true;
            }

            if(Bridge.disabledGames.getOrDefault("bypass", false)) {
                player.sendMessage("§cQueueing for that game has been temporarily disabled");
                return true;
            }

            boolean wasQueueNeeded = PlayerInfo.addToQueueIfNeeded(player, PlayerLocation.Bypass);
            if(wasQueueNeeded) {
                return true;
            }

            PlayerInfo.askToLeaveQueue(player);

            final long[] time = {0};

            Scoreboard board = Bridge.createScoreboard("    §b§eBypass Practice     ", new String[]{
                    "",
                    " §l§bTime",
                    "%time%§e 0",
                    "",
                    " §l§aBlocks",
                    "%blocks%§e 0",
                    "",
                    "%mode% §9Personal Best",
                    "%pb%§e None",
                    "",
                    " §l§bGoals All Time ",
                    "%goals%§e 0",
                    "",
                    "   §7bridgepractice.net  "
            });
            Bridge.setScoreboard(player, board);

            final TimeUpdater[] tu = {null};
            final Structure[] overlay = {null};

            // get PB from the DB and show it to the player
            final HashMap<String, Float> pbs = new HashMap<>();
            final int[] goals = {0};
            boolean wasStartNull = false;
            Leaderboard[] leaderboard = {null};
            try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT bypassGoals, bypassStartPB, bypassEarlyPB, bypassMiddlePB, bypassLatePB FROM players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString()); // uuid
                ResultSet res = statement.executeQuery();
                if(!res.next()) {
                    throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                }
                // display it if it exists, otherwise display "None"
                goals[0] = res.getInt(1); // 1 indexing!
                pbs.put("bypassStartPB", res.getFloat(2));
                wasStartNull = res.wasNull();
                pbs.put("bypassEarlyPB", res.getFloat(3));
                pbs.put("bypassMiddlePB", res.getFloat(4));
                pbs.put("bypassLatePB", res.getFloat(5));
                if(!wasStartNull) {
                    board.getTeam("pb").setPrefix("§e " + Bridge.prettifyNumber(pbs.get("bypassStartPB")));
                    board.getTeam("goals").setPrefix("§e " + goals[0]);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
            }

            SettingsMenu menu = new SettingsMenu(
                    new SettingsMenu.Entry[]{
                            new SettingsMenu.Entry(1, 1, Bridge.makeItem(Material.STAINED_GLASS_PANE, 1, "Disable Hitting", new String[]{"Will §cnot§7 hit you after you start", "so that you can practice getting", "hit into a bypass."}, 14), "hit", true),
                            new SettingsMenu.Entry(1, 2, Bridge.makeItem(Material.STAINED_GLASS_PANE, 1, "Enable Hitting", new String[]{"§aWill§7 hit you after you start", "so that you can practice getting", "hit into a bypass."}, 13), "hit", false),

                            new SettingsMenu.Entry(1, 4, Bridge.makeItem(Material.STAINED_CLAY, 1, "Start Game Map", new String[]{"Clears the map of all blocks", "so the map is like the very", "beginning of a game."}, 0), "map", true),
                            new SettingsMenu.Entry(1, 5, Bridge.makeItem(Material.STAINED_CLAY, 1, "Early Game Map", new String[]{"Sets the map's blocks so the", "map is like the early stages", "of a game."}, 4), "map", false),
                            new SettingsMenu.Entry(1, 6, Bridge.makeItem(Material.STAINED_CLAY, 1, "Middle Game Map", new String[]{"Sets the map's blocks so the", "map is like the middle stages", "of a game."}, 5), "map", false),
                            new SettingsMenu.Entry(1, 7, Bridge.makeItem(Material.STAINED_CLAY, 1, "Late Game Map", new String[]{"Sets the map's blocks so the", "map is like the late stages", "of a game."}, 11), "map", false),
                    },
                    3,
                    "Bypass Settings",
                    (itemClicked, groupName) -> {
                        PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
                        if(groupName.equals("hit")) {
                            info.locSettings.shouldHit = !info.locSettings.shouldHit;
                        } else if(groupName.equals("map")) {
                            if(overlay[0] != null) {
                                overlay[0].removeIfContentNotAir();
                            }
                            // otherwise these will be cleared after the death which could remove some blocks
                            for(Location loc : info.changedBlocks) {
                                Block block = loc.getBlock();
                                if(block.getType() == Material.STAINED_CLAY) // don't destroy blocks part of Structures
                                    block.setType(Material.AIR);
                            }
                            info.changedBlocks.clear();
                            switch(itemClicked.getDurability()) {
                                case 0:
                                    overlay[0] = null;
                                    info.locSettings.mode = "bypassStartPB";
                                    board.getTeam("mode").setSuffix(" ");
                                    leaderboard[0].setTitle("§bLeaderboard");
                                    break;
                                case 4:
                                    overlay[0] = new Structure(earlyContent);
                                    overlay[0].placeIfContentNotAir(new Location(Bridge.instance.world, info.winBox.relXZ[0] - 2, 87, info.winBox.relXZ[1] + 3));
                                    info.locSettings.mode = "bypassEarlyPB";
                                    board.getTeam("mode").setSuffix(" §9(Early Game)");
                                    leaderboard[0].setTitle("§bEarly Leaderboard");
                                    break;
                                case 5:
                                    overlay[0] = new Structure(middleContent);
                                    overlay[0].placeIfContentNotAir(new Location(Bridge.instance.world, info.winBox.relXZ[0] - 2, 84, info.winBox.relXZ[1] + 3));
                                    info.locSettings.mode = "bypassMiddlePB";
                                    board.getTeam("mode").setSuffix(" §9(Mid Game)");
                                    leaderboard[0].setTitle("§bMiddle Leaderboard");
                                    break;
                                case 11:
                                    overlay[0] = new Structure(lateContent);
                                    overlay[0].placeIfContentNotAir(new Location(Bridge.instance.world, info.winBox.relXZ[0] - 7, 84, info.winBox.relXZ[1] + 3));
                                    info.locSettings.mode = "bypassLatePB";
                                    board.getTeam("mode").setSuffix(" §9(Late Game)");
                                    leaderboard[0].setTitle("§bLate Leaderboard");
                                    break;
                            }
                            leaderboard[0].loadColumn(info.locSettings.mode);
                            if(pbs.get(info.locSettings.mode) != 0) {
                                board.getTeam("pb").setPrefix("§e " + Bridge.prettifyNumber(pbs.get(info.locSettings.mode)));
                            } else {
                                board.getTeam("pb").setPrefix("§e None");
                            }
                        }
                        info.onDeath.call(info);
                    }
            );


            Bridge.instance.setPlayer(player.getUniqueId(), new PlayerInfo(PlayerLocation.Bypass, menu, (info) -> {
                // on death
                if(tu[0] != null) {
                    tu[0].cancel();
                }
                new ResetBridgePlayer(player, info, true).run();
                info.locSettings.warned = null;
                time[0] = 0;
            }, (info) -> {
                // on block change
                board.getTeam("blocks").setPrefix("§e " + info.changedBlocks.size());
            }, (info) -> {
                // on location change
                for(Location loc : info.changedBlocks) {
                    loc.getBlock().setType(Material.AIR);
                }

                if(overlay[0] != null) {
                    overlay[0].removeIfContentNotAir();
                }

                if(leaderboard[0] != null) {
                    leaderboard[0].remove();
                }

                try(PreparedStatement statement = Bridge.connection.prepareStatement("UPDATE players SET bypassGoals = ? WHERE uuid=?;")) {
                    statement.setInt(1, goals[0]); // bypassGoals, set to the new amount
                    statement.setString(2, player.getUniqueId().toString()); // uuid, set to player uuid
                    statement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your time!");
                }

                time[0] = 0;
                if(tu[0] != null) {
                    tu[0].cancel();
                }
            }, (info) -> {
                // on win
                float timeTakenNum = (System.currentTimeMillis() - time[0]);
                String timeTaken = Bridge.prettifyNumber(timeTakenNum);

                tu[0].cancel();

                board.getTeam("time").setPrefix("§e " + timeTaken);
                new ResetBridgePlayer(player, info, true).run();
                player.sendMessage("§6You scored in " + timeTaken + " seconds! §a+3xp");
                Bridge.givePlayerXP(player, 3);
                BukkitRunnable winSound = new BukkitRunnable() {
                    int runs = 0;
                    @Override
                    public void run() {
                        runs++;
                        if(runs > 3) this.cancel();
                        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1.2f);
                    }
                };
                winSound.runTaskTimer(Bridge.instance, 1, 2);

                if(pbs.get(info.locSettings.mode) == 0 || pbs.get(info.locSettings.mode) > timeTakenNum) {
                    // PB
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            try(PreparedStatement statement = Bridge.connection.prepareStatement("UPDATE players SET " + info.locSettings.mode + " = ? WHERE uuid=?;")) {
                                statement.setFloat(1, timeTakenNum); // set to the new PB
                                statement.setString(2, player.getUniqueId().toString()); // uuid, set to player uuid
                                statement.executeUpdate();
                                leaderboard[0].loadColumn(info.locSettings.mode); // update leaderboard
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                                player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your time!");
                            }
                        }
                    }).runTaskAsynchronously(Bridge.instance);

                    // display in #multiplayer-logs
                    sendNewPBWebhook(player, timeTaken);

                    // show to player (we don't need to go through the db at this point)
                    board.getTeam("pb").setPrefix("§e " + timeTaken);
                    Bridge.sendTitle(player, "§bNew PB! §e" + timeTaken, "");
                    pbs.put(info.locSettings.mode, timeTakenNum);
                }

                goals[0]++;
                board.getTeam("goals").setPrefix("§e " + goals[0]);

                time[0] = 0;
                info.locSettings.warned = null;
            }, null, (info) -> {
                // on player move
                int z = player.getLocation().getBlockZ();
                int x = player.getLocation().getBlockX();
                int y = player.getLocation().getBlockY();
                if(z > info.winBox.relXZ[1] + 2) {
                    if(time[0] == 0) {
                        // start
                        time[0] = System.currentTimeMillis();
                        tu[0] = new TimeUpdater(time, board, player);
                        tu[0].runTaskTimer(Bridge.instance, 0, 4);
                        if(info.locSettings.shouldHit) {
                            BukkitRunnable hit = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // set velo
                                    player.setVelocity(new Vector((Math.random() < 0.5 ? -1 : 1) * 0.25, 0.3, 0));
                                    player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);
                                    RavenAntiCheat.emulatePlayerTakeKnockback(player);
                                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(((CraftPlayer) player).getHandle(), 1)); // make it look like player took damage
                                }
                            };
                            hit.runTaskLater(Bridge.instance, (int) ((Math.random() * 15) + 5));
                        }
                    }

                    if((x == info.winBox.relXZ[0] && z >= -127 && z <= -106) || y >= 100) {
                        // warn or cause death
                        if(info.locSettings.warned == null) {
                            Bridge.sendActionBar(player, "§c§lWarning:§r§e You will restart if you stay on top of the bridge longer", 1);
                            info.locSettings.warned = System.currentTimeMillis();
                        } else {
                            if(System.currentTimeMillis() - info.locSettings.warned > 2 * 1000) {
                                // if two seconds have passed since warning, then kill them
                                info.onDeath.call(info);
                            }
                        }
                    }
                }
            }));

            PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(info.respawnLocation);
            Bridge.setBridgeInventory(player, true);

            // create the leaderboard
            leaderboard[0] = new Leaderboard("§bLeaderboard", player, info.winBox.relXZ[0] + 4.5, 93.6, info.winBox.relXZ[1] + 1.25, Leaderboard.Direction.Ascending, Leaderboard.ColumnType.Float);
            leaderboard[0].loadColumn("bypassStartPB");

            if(wasStartNull) {
                // intro message
                BukkitRunnable intro = new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage("\n§6" + new String(new char[54]).replace("\0", "-"));
                        player.sendMessage("\nLooks like it's your first time playing §aBypass Practice§f!");
                        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                        BukkitRunnable a = new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendMessage("\nThe goal is to drop low on one side and wallrun all the way to score on the other.");
                                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                            }
                        };
                        BukkitRunnable b = new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendMessage("\nIf you stay in the middle of the bridge for too long you will be reset.");
                                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                            }
                        };
                        BukkitRunnable c = new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendMessage("\nYou can opt to be hit before bypassing or change the bridge to be start, early, middle, or late game using the settings menu (the emerald). Have fun!");
                                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                player.sendMessage("\n§6" + new String(new char[54]).replace("\0", "-"));
                            }
                        };
                        a.runTaskLater(Bridge.instance, 3 * 20);
                        b.runTaskLater(Bridge.instance, 6 * 20);
                        c.runTaskLater(Bridge.instance, 9 * 20);
                    }
                };

                intro.runTaskLater(Bridge.instance, 10);

                board.getTeam("pb").setPrefix("§e None");
            }
            return true;
        } else {
            sender.sendMessage("You must be a player!");
        }
        return false;
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
                author.addProperty("name", "New PB (Mode: Bypass)");

                String playerName = player.getName();

                embed.addProperty("title", playerName + ": " + (time));

                embed.addProperty("description", playerName + " got new PB            |           on Bypass Map\n"
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
}