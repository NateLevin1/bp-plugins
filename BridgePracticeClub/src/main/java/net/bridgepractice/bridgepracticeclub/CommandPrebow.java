package net.bridgepractice.bridgepracticeclub;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

public class CommandPrebow implements CommandExecutor {
    public static BlockState[][][] targetContent;
    public static BlockState[][][] mushroomContent;
    public static BlockState[][][] flowerContent;
    public static BlockState[][][] sailboatContent;
    final int[] distances = {74, 68, 64, 62, 60, 57, 48, 46};
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo oldInfo = Bridge.instance.getPlayer(player.getUniqueId());
            if(oldInfo != null && oldInfo.location == PlayerLocation.Prebow) {
                player.sendMessage("§cYou are already practicing prebowing!");
                return true;
            }

            if(Bridge.disabledGames.getOrDefault("prebow", false)) {
                player.sendMessage("§cQueueing for that game has been temporarily disabled");
                return true;
            }

            boolean wasQueueNeeded = PlayerInfo.addToQueueIfNeeded(player, PlayerLocation.Prebow);
            if(wasQueueNeeded) {
                return true;
            }

            PlayerInfo.askToLeaveQueue(player);

            final int[] sessionPrebows = {0};
            final int[] allTime = {0};

            Scoreboard board = Bridge.createScoreboard("   §b§ePrebow Practice    ", new String[] {
                    "",
                    " §l§bPrebows Hit (All Time)",
                    "%alltime%§e 0",
                    "",
                    " §l§aPrebows Hit (Session)",
                    "%session%§e 0",
                    "",
                    " §l§dArrow XYZ §7(All 0s is best)",
                    "%xyz%§e 0 / 0 / 0",
                    "",
                    "   §7bridgepractice.net  "
            });
            Bridge.setScoreboard(player, board);

            final ArrowPositionTracker[] arrowPositionTracker = {null};
            final int[] relX = {97};
            final boolean[] respawnAfterShot = {false};
            final int[] currentDistanceCol = {3};
            Structure spawn = new Structure(mushroomContent);
            Structure target = new Structure(targetContent);

            SettingsMenu[] menuCopy = {null};
            SettingsMenu menu = new SettingsMenu(
                    new SettingsMenu.Entry[] {
                            new SettingsMenu.Entry(1, 1, Bridge.makeItem(Material.RED_MUSHROOM, 1, "Mushroom Cage", new String[]{"Practice prebowing with the","mushroom cage."}, -1), "cage", true),
                            new SettingsMenu.Entry(1, 2, Bridge.makeItem(Material.RED_ROSE, 1, "Flower Cage", new String[]{"Practice prebowing with the","flower cage."}, -1), "cage", false),
                            new SettingsMenu.Entry(1, 3, Bridge.makeItem(Material.BOAT, 1, "Sailboat Cage", new String[]{"Practice prebowing with the","sailboat cage."}, -1), "cage", false),

                            new SettingsMenu.Entry(1, 6, Bridge.makeItem(Material.STAINED_GLASS_PANE, 1, "Don't Respawn After Shooting", new String[]{"Will §cnot§7 respawn you after","shooting an arrow."}, 14), "respawn", true),
                            new SettingsMenu.Entry(1, 7, Bridge.makeItem(Material.BEACON, 1, "Do Respawn After Shooting", new String[]{"Allows you to practice getting","into the correct position for","the prebow on each attempt."}, -1), "respawn", false),

                            new SettingsMenu.Entry(3, 1, Bridge.makeItem(Material.PRISMARINE,    1, "Hyperfrost Distance", new String[]{"Set the distance to the","distance between cages in","Hyperfrost (74 blocks.)"}, 1), "0", false),

                            new SettingsMenu.Entry(3, 2, Bridge.makeItem(Material.ENDER_STONE,   1, "Atlantis Distance", new String[]{"Set the distance to the","distance between cages in","Atlantis (68 blocks.)"}, -1), "1", false),
                            new SettingsMenu.Entry(4, 2, Bridge.makeItem(Material.STONE,         1, "Condo Distance", new String[]{"Set the distance to the","distance between cages in","Condo (68 blocks.)"}, 6), "1", false),

                            new SettingsMenu.Entry(3, 3, Bridge.makeItem(Material.QUARTZ_BLOCK,  1, "Galaxy Distance", new String[]{"Set the distance to the","distance between cages in","Galaxy (64 blocks.)"}, -1), "2", false),
                            new SettingsMenu.Entry(4, 3, Bridge.makeItem(Material.STAINED_CLAY,  1, "Licorice Distance", new String[]{"Set the distance to the","distance between cages in","Licorice (64 blocks.)"}, 10), "2", false),

                            new SettingsMenu.Entry(2, 4, Bridge.makeItem(Material.WOOL,          1, "Sorcery Distance", new String[]{"Set the distance to the","distance between cages in","Sorcery (62 blocks.)"}, 13), "3", true),
                            new SettingsMenu.Entry(3, 4, Bridge.makeItem(Material.NETHER_BRICK,  1, "Boo Distance", new String[]{"Set the distance to the","distance between cages in","Boo (62 blocks.)"}, -1), "3", true),
                            new SettingsMenu.Entry(4, 4, Bridge.makeItem(Material.SMOOTH_BRICK,  1, "Fortress Distance", new String[]{"Set the distance to the","distance between cages in","Fortress (62 blocks.)"}, -1), "3", true),
                            new SettingsMenu.Entry(5, 4, Bridge.makeItem(Material.STAINED_CLAY,  1, "Twilight Distance", new String[]{"Set the distance to the","distance between cages in","Twilight (62 blocks.)"}, 9), "3", true),

                            new SettingsMenu.Entry(2, 5, Bridge.makeItem(Material.GRASS,         1, "Urban Distance", new String[]{"Set the distance to the","distance between cages in","Urban (60 blocks.)"}, -1), "4", false),
                            new SettingsMenu.Entry(3, 5, Bridge.makeItem(Material.LOG,           1, "Stumped Distance", new String[]{"Set the distance to the","distance between cages in","Stumped (60 blocks.)"}, -1), "4", false),
                            new SettingsMenu.Entry(4, 5, Bridge.makeItem(Material.SANDSTONE,     1, "Lighthouse Distance", new String[]{"Set the distance to the","distance between cages in","Lighthouse (60 blocks.)"}, -1), "4", false),
                            new SettingsMenu.Entry(5, 5, Bridge.makeItem(Material.STAINED_CLAY,  1, "Flora Distance", new String[]{"Set the distance to the","distance between cages in","Flora (60 blocks.)"}, 13), "4", false),

                            new SettingsMenu.Entry(3, 6, Bridge.makeItem(Material.STAINED_CLAY,  1, "Dojo Distance", new String[]{"Set the distance to the","distance between cages in","Dojo (57 blocks.)"}, 14), "5", false),
                            new SettingsMenu.Entry(4, 6, Bridge.makeItem(Material.PRISMARINE,    1, "Aquatica Distance", new String[]{"Set the distance to the","distance between cages in","Aquatica (57 blocks.)"}, 2), "5", false),
                            new SettingsMenu.Entry(5, 6, Bridge.makeItem(Material.WOOD,          1, "Treehouse Distance", new String[]{"Set the distance to the","distance between cages in","Treehouse (57 blocks.)"}, 2), "5", false),

                            new SettingsMenu.Entry(3, 7, Bridge.makeItem(Material.SNOW_BLOCK,    1, "Tundra Distance", new String[]{"Set the distance to the","distance between cages in","Tundra (48 blocks.)"}, -1), "6", false),
                            new SettingsMenu.Entry(4, 7, Bridge.makeItem(Material.SMOOTH_BRICK,  1, "Sunstone Distance", new String[]{"Set the distance to the","distance between cages in","Sunstone (46 blocks.)"}, 1), "7", false),
                    },
                    6,
                    "Prebow Settings",
                    (itemClicked, groupName)->{
                        PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
                        if(groupName.equals("cage")) {
                            if(itemClicked.getType() == Material.RED_MUSHROOM && spawn.content != mushroomContent) {
                                spawn.removeAndSwitchContentTo(mushroomContent);
                                spawn.place(new Location(Bridge.instance.world, relX[0]-4, 105, -142));
                            } else if(itemClicked.getType() == Material.RED_ROSE && spawn.content != flowerContent) {
                                spawn.removeAndSwitchContentTo(flowerContent);
                                spawn.place(new Location(Bridge.instance.world, relX[0]-4, 103, -142));
                            } else if(itemClicked.getType() == Material.BOAT && spawn.content != sailboatContent) {
                                spawn.removeAndSwitchContentTo(sailboatContent);
                                spawn.place(new Location(Bridge.instance.world, relX[0]-4, 106, -142));
                            }
                        } else if(groupName.equals("respawn")) {
                            if(itemClicked.getType() == Material.STAINED_GLASS_PANE) {
                                respawnAfterShot[0] = false;
                            } else if(itemClicked.getType() == Material.BEACON) {
                                respawnAfterShot[0] = true;
                            }
                        } else {
                            // map block
                            int column = Integer.parseInt(groupName);
                            if(column == currentDistanceCol[0]) return;
                            assert menuCopy[0] != null;
                            for(Map.Entry<String, ArrayList<SettingsMenu.Entry>> entry : menuCopy[0].groups.entrySet()) {
                                try{
                                    int keyColumn = Integer.parseInt(entry.getKey());
                                    if(column == keyColumn) {
                                        for(SettingsMenu.Entry block : entry.getValue()) {
                                            // highlight
                                            menuCopy[0].inventory.setItem(block.index, block.enchantedItem);
                                        }
                                    } else {
                                        for(SettingsMenu.Entry block : entry.getValue()) {
                                            // un-highlight
                                            menuCopy[0].inventory.setItem(block.index, block.item);
                                        }
                                    }
                                } catch (NumberFormatException ignored) {} // means it is a non-column group
                            }
                            target.remove();
                            target.place(new Location(Bridge.instance.world, relX[0]-4, 105, -142+distances[column]));
                            currentDistanceCol[0] = column;
                        }
                        info.onDeath.call(info);
                    }
            );
            menuCopy[0] = menu;

            Bridge.instance.setPlayer(player.getUniqueId(), new PlayerInfo(PlayerLocation.Prebow, menu, (info) -> {
                new ResetBridgePlayer(player, info, true).run(); // on death
            }, null, (info)->{
                // on location change
                if(arrowPositionTracker[0] != null) {
                    arrowPositionTracker[0].cancel();
                }
                if(spawn.content != mushroomContent) {
                    spawn.removeAndSwitchContentTo(mushroomContent);
                    spawn.place(new Location(Bridge.instance.world, relX[0]-4, 105, -142));
                }
                if(currentDistanceCol[0] != 3) {
                    target.remove();
                    target.place(new Location(Bridge.instance.world, relX[0]-4, 105, -80));
                }
                // push to DB
                try(PreparedStatement statement = Bridge.connection.prepareStatement("UPDATE players SET prebowHits = ? WHERE uuid=?;")) {
                    statement.setInt(1, allTime[0]); // prebowHits, set to the new value
                    statement.setString(2, player.getUniqueId().toString()); // uuid, set to player uuid
                    statement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your time!");
                }
            }, (info)->{
                // on win
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 0.8f);
                int biar = info.locSettings.bowsInARow;
                player.sendMessage("§6You hit the prebow! "+(biar > 1 ? "§bThat's "+biar+" in a row! " : "")+"§a+"+(3*biar)+" xp");
                sessionPrebows[0]++;
                allTime[0]++;
                board.getTeam("session").setPrefix("§e "+sessionPrebows[0]);
                board.getTeam("alltime").setPrefix("§e "+ allTime[0]);
                Bridge.givePlayerXP(player, (3*biar));
            }, (info, arrow, bow) -> {
                // on bow shoot
                if(arrowPositionTracker[0] != null) {
                    arrowPositionTracker[0].cancel();
                }
                arrowPositionTracker[0] = new ArrowPositionTracker(board, arrow, new int[] { relX[0], 109, -139+distances[currentDistanceCol[0]] });
                arrowPositionTracker[0].runTaskTimer(Bridge.instance, 0, 2);
                if(respawnAfterShot[0]) {
                    BukkitRunnable run = new BukkitRunnable() {
                        @Override
                        public void run() {
                            info.onDeath.call(info);
                        }
                    };
                    run.runTaskLater(Bridge.instance, 15);
                }
            }, null));

            try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT prebowHits FROM players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString()); // uuid
                ResultSet res = statement.executeQuery();
                if(!res.next()) {
                    throw new SQLException("Did not get a row from the database. Player name: "+player.getName()+" Player UUID: "+player.getUniqueId());
                }
                // display it if it exists, otherwise display "None"
                allTime[0] = res.getInt(1); // 1 indexing!
                if(!res.wasNull() && allTime[0] != 0) {
                    board.getTeam("alltime").setPrefix("§e "+ allTime[0]);
                } else {
                    // intro message
                    BukkitRunnable intro = new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("\n§6"+new String(new char[54]).replace("\0", "-"));
                            player.sendMessage("\nLooks like it's your first time playing §aPrebow Practice§f!");
                            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                            BukkitRunnable a = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("\nThe goal is to shoot your arrow over your cage to hit the gold blocks on the other side.");
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                }
                            };
                            BukkitRunnable b = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("\nYou can change your cage at any time using the settings menu.");
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                }
                            };
                            BukkitRunnable c = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("\nYou can also use the settings menu to set the distance to your favorite map.");
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                }
                            };
                            BukkitRunnable d = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("\nThis skill is also known as \"Overshooting\" or \"Kashooting\". Have fun!");
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                    player.sendMessage("\n§6"+new String(new char[54]).replace("\0", "-"));
                                }
                            };
                            a.runTaskLater(Bridge.instance, 3*20);
                            b.runTaskLater(Bridge.instance, 6*20);
                            c.runTaskLater(Bridge.instance, 9*20);
                            d.runTaskLater(Bridge.instance, 12*20);
                        }
                    };

                    intro.runTaskLater(Bridge.instance, 10);

                    board.getTeam("alltime").setPrefix("§e 0");
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
            }

            PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
            relX[0] = (97+(info.position*20));


            target.previousLocation = new Location(Bridge.instance.world, relX[0]-4, 105, -80);
            spawn.previousLocation = new Location(Bridge.instance.world, relX[0]-4, 105, -142);

            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(info.respawnLocation);
            Bridge.setBridgeInventory(player, true);
            return true;
        } else {
            sender.sendMessage("You must be a player!");
        }
        return false;
    }
}

class ArrowPositionTracker extends BukkitRunnable {
    Scoreboard board;
    Arrow arrow;
    int[] goldXYZ;
    ArrowPositionTracker(Scoreboard board, Arrow arrow, int[] goldXYZ) {
        this.board = board;
        this.arrow = arrow;
        this.goldXYZ = goldXYZ;
    }
    @Override
    public void run() {
        Location arrowLoc = arrow.getLocation();
        int x = arrowLoc.getBlockX() - goldXYZ[0];
        int y = arrowLoc.getBlockY() - goldXYZ[1];
        int z = arrowLoc.getBlockZ() - goldXYZ[2];
        board.getTeam("xyz").setPrefix("§e "+x+" / "+y);
        board.getTeam("xyz").setSuffix("§e / "+z);
        if(y <= -40) {
            this.cancel();
        }
    }
}