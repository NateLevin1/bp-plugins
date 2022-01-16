package net.bridgepractice.bridgepracticeclub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CommandBridgeBot implements CommandExecutor {
    public static BlockState[][][] bridgeContent;
    public static BlockState[][][] cageContent;
    public static BlockState[][][] npcCageContent;
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo oldInfo = Bridge.instance.getPlayer(player.getUniqueId());
            if(oldInfo != null && oldInfo.location == PlayerLocation.BridgeBot) {
                player.sendMessage("§cYou are already practicing bot 1v1s!");
                return true;
            }

            if(Bridge.disabledGames.getOrDefault("bot", false)) {
                player.sendMessage("§cQueueing for that game has been temporarily disabled");
                return true;
            }

            boolean wasQueueNeeded = PlayerInfo.addToQueueIfNeeded(player, PlayerLocation.BridgeBot);
            if(wasQueueNeeded) {
                return true;
            }

            PlayerInfo.askToLeaveQueue(player);

            Structure cage = new Structure(cageContent);
            Structure npcCage = new Structure(npcCageContent);

            ScoreHandler scoreHandler = (botWon, npc) -> {
                ResetBridgePlayer rbp = new ResetBridgePlayer(player, Bridge.instance.getPlayer(player.getUniqueId()), false);
                rbp.destroyBlocks = false;
                rbp.respawnLocation = new Location(Bridge.instance.world, rbp.respawnLocation.getX(), 102.3, -5.5);
                rbp.run();
                npc.reset();
            };
            final boolean[] wasWinHandled = {false};

            String date = new SimpleDateFormat("MM/dd/yy").format(new Date(System.currentTimeMillis()));

            for(Player p : Bridge.instance.getServer().getOnlinePlayers()) {
                // FIXME: This has implementation has an obvious bug: what if a player joins in the middle of another player's game?
                player.hidePlayer(p);
            }

            Scoreboard board = Bridge.createScoreboard("   §eBot 1v1   ", new String[]{
                    "§7" + date + " §8[BOT]",
                    "",
                    "%time%§fTime: %§a15:00",
                    "",
                    "%red%§c[R] %§7⬤⬤⬤⬤⬤",
                    "%blue%§9[B] %§7⬤⬤⬤⬤⬤",
                    "",
                    "%kills%§fKills: %§a0",
                    "%goals%§fGoals: %§a0",
                    "",
                    "§fMode: §aBot 1v1",
                    "%ws%§fWin Streak: %§a0",
                    "%wins%§fTotal Wins: %§a0",
                    "",
                    "   §ebridgepractice.net  "
            });
            Bridge.setScoreboard(player, board);

            long startTime = System.currentTimeMillis();

            BukkitRunnable timeUpdater = new BukkitRunnable() {
                @Override
                public void run() {
                    String formatted = new SimpleDateFormat("mm:ss").format(new Date(System.currentTimeMillis() - startTime));
                    board.getTeam("time").setSuffix("§a" + formatted);
                }
            };
            timeUpdater.runTaskTimer(Bridge.instance, 0, 20);

            String[] npcName = {""};
            int[] winstreak = {0};
            boolean[] didLocationChange = {false};
            String dashes = "§a§l" + new String(new char[47]).replace("\0", "-") + "§r";

            PutInCages putInCages = (scorer, npc) -> {
                npc.isInCage = true;
                npc.setLocation((float) npc.respawnLocation.getX(), 102.3f, 49.5f).teleportToSetLocation();
                player.setGameMode(GameMode.ADVENTURE);
                cage.place(new Location(Bridge.instance.world, (float) npc.respawnLocation.getX()-4.5, 99, -8.5));
                npcCage.place(new Location(Bridge.instance.world, (float) npc.respawnLocation.getX()-4.5, 99, 47));
                player.teleport(new Location(Bridge.instance.world, (float) npc.respawnLocation.getX(), 102.3, -5.5));
                BukkitRunnable removeFromCages = new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.setGameMode(GameMode.SURVIVAL);
                        npcCage.remove();
                        cage.remove();
                        npc.isInCage = false;
                    }
                };
                npc.schedules.add(removeFromCages.runTaskLater(Bridge.instance, 5 * 20));

                final int[] secsLeft = {6};
                String titleString = "";
                if(scorer == 0) {
                    titleString = "§9" + player.getName() + " scored!";
                } else if(scorer == 1) {
                    titleString = "§c" + npc.name + " scored!";
                }

                String finalTitleString = titleString;
                BukkitRunnable showTime = new BukkitRunnable() {
                    @Override
                    public void run() {
                        secsLeft[0]--;
                        if(secsLeft[0] < 1) {
                            Bridge.sendTitle(player, "", "§aFight!", 0, 5, 20);
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 1f);
                            this.cancel();
                            return;
                        }
                        Bridge.sendTitle(player, finalTitleString, "§7Cages open in §a" + secsLeft[0] + "s§7...", 0, 0, 25);
                        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1f, 1);
                    }
                };
                npc.schedules.add(showTime.runTaskTimer(Bridge.instance, 0, 20));
            };

            DidBotWinHandler sendWinLossMessage = (botWon) -> {
                player.sendMessage(dashes);
                player.sendMessage("\n                      §e§lBot 1v1§r§7 - §f§l" + new SimpleDateFormat("mm:ss").format(new Date(System.currentTimeMillis() - startTime)) + "§r");
                if(botWon) {
                    player.sendMessage("\n         §b" + npcName[0] + " §e§lWINNER!  §r§b" + player.getDisplayName());
                    player.sendMessage("\n                         §aYou §clost§7.");
                    if(winstreak[0] != 0) { // don't send the winstreak message if it won't have changed
                        player.sendMessage("\n                  §fWinstreak: §a"+winstreak[0]+" §7§m->§r §c§l0");
                    }
                } else {
                    player.sendMessage("\n         §b" + player.getDisplayName() + " §e§lWINNER!  §r§b" + npcName[0]);
                    player.sendMessage("\n                         §aYou §ewon§7!");
                    player.sendMessage("\n                  §fWinstreak: §c"+winstreak[0]+" §7§m->§r §a§l"+(winstreak[0]+1));
                }
                player.sendMessage("\n" + dashes);

                String sqlToRun;
                if(botWon) {
                    sqlToRun = "UPDATE players SET botWinStreak = 0 WHERE uuid=?;";
                } else {
                    sqlToRun = "UPDATE players SET botWinStreak = botWinStreak + 1, botWins = botWins + 1 WHERE uuid=?;";
                }
                // push to DB
                try(PreparedStatement winstreakUpdate = Bridge.connection.prepareStatement(sqlToRun)) {
                    winstreakUpdate.setString(1, player.getUniqueId().toString()); // uuid, set to player uuid
                    winstreakUpdate.executeUpdate();

                    // send log
                    sendWinLossWebhook(botWon, player, startTime, winstreak[0]);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your time!");
                }
            };
            ScoreHandler onWin = (botWon, npc) -> {
                wasWinHandled[0] = true;
                npc.clearSchedules();
                npc.remove();
                ResetBridgePlayer rbp = new ResetBridgePlayer(player, Bridge.instance.getPlayer(player.getUniqueId()), false);
                rbp.destroyBlocks = false;
                rbp.run();
                sendWinLossMessage.call(botWon);

                BukkitRunnable backToSpawn = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(!didLocationChange[0]) {
                            player.chat("/spawn");
                        }
                    }
                };
                backToSpawn.runTaskLater(Bridge.instance, 5 * 20);
            };

            NPC npc = new NPC(player, scoreHandler, onWin, putInCages);
            npcName[0] = npc.name;
            Leaderboard[] leaderboard = {null};

            Bridge.instance.setPlayer(player.getUniqueId(), new PlayerInfo(PlayerLocation.BridgeBot, null, (info) -> {
                // on death
                player.setVelocity(new Vector());
                ResetBridgePlayer rbp = new ResetBridgePlayer(player, info, false);
                rbp.destroyBlocks = false;
                rbp.run();
                npc.onPlayerKilled();
                if(info.locSettings.arrowTask != null) {
                    info.locSettings.arrowTask.cancel();
                }
            }, null, (info) -> {
                // on location change
                didLocationChange[0] = true;
                if(System.currentTimeMillis() - startTime > 2000) {
                    if(!wasWinHandled[0]) {
                        sendWinLossMessage.call(true);
                    }
                } else {
                    player.sendMessage("§c§lUh oh!§r§c That game ended too fast, so it was cancelled.");
                }

                cage.remove();
                npcCage.remove();
                if(leaderboard[0] != null) {
                    leaderboard[0].remove();
                }
                for(Location loc : info.changedBlocks) {
                    loc.getBlock().setType(Material.AIR);
                }
                player.setHealth(20);
                npc.clearSchedules();
                npc.removeChangedBlocks();
                npc.remove();
                for(Player p : Bridge.instance.getServer().getOnlinePlayers()) {
                    if(!player.canSee(p))
                        player.showPlayer(p);
                }
                timeUpdater.cancel();
                new Structure(bridgeContent).place(new Location(Bridge.instance.world, info.relXZ[0], 84, 2));

                // clear entities on ground
                for(Entity entity : Bridge.instance.world.getEntities()) {
                    if(entity instanceof Item) {
                        Location loc = entity.getLocation();
                        if(Math.abs(loc.getX() - info.relXZ[0]) < 5 && loc.getZ() > info.relXZ[1]-10 && loc.getZ() < info.relXZ[1]+70) {
                            entity.remove();
                        }
                    }
                }
            }, null, (info, arrow, bow) -> {
                if(info.locSettings.arrowTask != null) {
                    info.locSettings.arrowTask.cancel();
                }
                info.locSettings.arrowTask = (new BukkitRunnable() {
                    @Override
                    public void run() {
                        double npcX = npc.npc.locX;
                        double npcY = npc.npc.locY;
                        double npcZ = npc.npc.locZ;
                        double hitEpsilon = 1.1;
                        Location arrowLoc = arrow.getLocation();
                        if(arrowLoc.getBlockY() < 75) {
                            this.cancel();
                            info.locSettings.arrowTask = null;
                            arrow.remove();
                            return;
                        }

                        if(Math.abs(arrowLoc.getBlockX() - npcX) <= hitEpsilon && Math.abs(arrowLoc.getBlockZ() - npcZ) <= hitEpsilon && (arrowLoc.getBlockY() == Math.floor(npcY) || arrowLoc.getBlockY() == Math.floor(npcY) + 1)) {
                            arrow.remove();
                            npc.hitFromArrow(player, arrow);
                            this.cancel();
                            info.locSettings.arrowTask = null;
                        }
                    }
                }).runTaskTimer(Bridge.instance, 0, 1);
            }, null));

            PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());

            leaderboard[0] = new Leaderboard("§bWinstreak Leaderboard", player, info.relXZ[0] + 0.5, 97.3, info.relXZ[1] - 6, Leaderboard.Direction.Descending, Leaderboard.ColumnType.Integer);
            leaderboard[0].loadColumn("botWinStreak");

            npc.setRespawnLocation(info.relXZ[0]+0.5f, 96.3f, 49.5f)
                    .setLocation(info.relXZ[0]+0.5f, 102.3f, 49.5f)
                    .setBridge()
                    .setBridgeX(info.relXZ[0])
                    .showToPlayer();

            player.sendMessage("\n" + dashes);
            player.sendMessage("\n                              §a§lBot 1v1");
            player.sendMessage("\n      §eFirst player/bot to score 5 goals wins!");
            player.sendMessage("\n                  §f§lOpponent: §r§b" + npcName[0]);
            player.sendMessage("\n" + dashes);

            BukkitRunnable notifyAboutBeta = new BukkitRunnable() {
                @Override
                public void run() {
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8f, 1.2f);
                    player.sendMessage("§b§lHEY!§r §aThis mode is currently in §lbeta§r§a. Report any bugs you find on the Discord!");
                }
            };
            notifyAboutBeta.runTaskLater(Bridge.instance, 20);

            player.getInventory().setHeldItemSlot(0);

            info.locSettings.npcId = npc.npc.getId();
            info.locSettings.onNpcHit = (i) -> {
                npc.hit(player);
            };

            putInCages.call(-1, npc);

            Bridge.setBridgeInventory(player, false);

            try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT botWinStreak, botWins FROM players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString()); // uuid
                ResultSet res = statement.executeQuery();
                if(!res.next()) {
                    throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                }
                int botWinStreak = res.getInt(1); // 1 indexing!
                int botWins = res.getInt(2);
                winstreak[0] = botWinStreak;
                board.getTeam("ws").setSuffix("§a" + botWinStreak);
                board.getTeam("wins").setSuffix("§a" + botWins);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
            }

            return true;
        } else {
            sender.sendMessage("You must be a player!");
        }
        return false;
    }
    private void sendWinLossWebhook(boolean botWon, Player player, long startTime, int playerWs) {
        (new BukkitRunnable() {
            @Override
            public void run() {
                // send a winstreak log message in the discord - this is so winstreak audits are possible
                JsonObject webhook = new JsonObject();
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                JsonObject author = new JsonObject();

                webhook.add("embeds", embeds);
                embeds.add(embed);

                embed.addProperty("color", 0x39c2ff);

                embed.add("author", author);
                author.addProperty("name", "Winstreak Change (Mode: Bot 1v1)");

                int winningPlayerWs = botWon ? -1 : playerWs;
                int losingPlayerWs = botWon ? playerWs : -1;

                long time = System.currentTimeMillis() - startTime;
                long minutes = time / (60 * 1000);
                long seconds = (time / 1000) % 60;
                String formattedTime = String.format("%d:%02d", minutes, seconds);

                String winningPlayer = botWon ? "Bot" : player.getName();
                String losingPlayer = botWon ? player.getName() : "Bot";

                embed.addProperty("title", winningPlayer + ": " + (winningPlayerWs) + " ⇢ " + (winningPlayerWs + 1) +
                        " | " + losingPlayer + ": " + losingPlayerWs + " ⇢ 0");

                embed.addProperty("description", winningPlayer + " beat " + losingPlayer + "           |           on Bot Map\n"
                        + "```pascal\n"
                        + " Unknown Score (Bot) {" + formattedTime + "} \n"
                        + "```");

                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", (botWon ? "https://cdn.discordapp.com/attachments/869332740489764864/926512822387605604/64.png" : "https://minotar.net/armor/bust/" + winningPlayer + "/64"));
                embed.add("thumbnail", thumbnail);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", losingPlayer + " lost their ws to " + winningPlayer + " [" + losingPlayerWs + "]");
                embed.add("footer", footer);

                embed.addProperty("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

                Utils.sendWebhookSync(webhook);
            }
        }).runTaskAsynchronously(Bridge.instance);
    }
    interface DidBotWinHandler {
        void call(boolean botWon);
    }
}