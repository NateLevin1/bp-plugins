package net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import net.minecraft.server.v1_8_R3.Container;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LootPractice implements CommandExecutor {
    static HashMap<UUID, Team> leaderTeams = new HashMap<>();
    static HashMap<String, Float> playerTimes = new HashMap<>();
    public static List<UUID> playersInCage = new ArrayList<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        if (Main.lootPracticeQueue.contains(player.getUniqueId())) {
            player.sendMessage("You are already in the Loot Practice queue!");
        } else if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
            player.sendMessage("You are already in Loot Practice!");
        } else {
            int lastplace = Main.lootPracticeQueue.toArray().length;
            Main.lootPracticeQueue.add(lastplace, player.getUniqueId());
            String placeMessage = "There are " + Main.lootPracticeQueue.indexOf(player.getUniqueId()) + " players ahead of you.";
            if (Integer.toString(Main.lootPracticeQueue.indexOf(player.getUniqueId())).equals("0")) {
                player.sendMessage("You are next in queue!");
            } else if (placeMessage.equals("There are 1 players ahead of you.")) {
                player.sendMessage("There is 1 player ahead of you.");
            } else {
                player.sendMessage(placeMessage);
            }
        }
        return true;
    }

    public static void start(String mapname, Player p1) {
        PlayerInventory pli1 = p1.getInventory();
        pli1.clear();
        Main.availableLootPracticeMaps.remove(mapname);
        p1.sendMessage(Main.availableLootPracticeMaps.toString());
        Main.lootPracticeQueue.remove(p1.getUniqueId());
        playersInCage.add(p1.getUniqueId());
        Main.playersInLootPractice.put(p1.getUniqueId(), mapname + ":" + "none");
        Main.lootPracticeBlocksPlaced.put(p1.getUniqueId(), new ArrayList<>());
        p1.setGameMode(GameMode.ADVENTURE);

        Scoreboard board = Main.createScoreboard("    §b§eLoot Practice     ", new String[] {
                "",
                " §l§bTime",
                "%time%§e 0",
                "",
                " §l§dSession Leader",
                "%leader%§e None",
                "",
                " §l§9Personal Best  ",
                "%pb%§e None",
                "",
                "   §7bridgepractice.net  "
        });
        Main.setScoreboard(p1, board);
        updateSessionLeader();
        leaderTeams.put(p1.getUniqueId(), board.getTeam("leader"));

        // get pb from db
        float pb = 0;
        try(PreparedStatement statement = Main.connection.prepareStatement("SELECT lootPB FROM skywarsPlayers WHERE uuid=?;")) {
            statement.setString(1, p1.getUniqueId().toString()); // uuid
            ResultSet res = statement.executeQuery();
            if(!res.next()) {
                throw new SQLException("Did not get a row from the database. Player name: "+p1.getName()+" Player UUID: "+p1.getUniqueId());
            }
            // display it if it exists, otherwise display "None"
            pb = res.getFloat(1); // 1 indexing!
            if(!res.wasNull()) {
                board.getTeam("pb").setPrefix("§e "+Utils.prettifyNumber(pb));
            } else {
                // intro message
                BukkitRunnable intro = new BukkitRunnable() {
                    @Override
                    public void run() {
                        p1.sendMessage("\n§6"+new String(new char[54]).replace("\0", "-"));
                        p1.sendMessage("\nLooks like it's your first time playing §aLoot Practice§f!");
                        p1.playSound(p1.getLocation(), Sound.ORB_PICKUP, 1, 1);
                        BukkitRunnable a = new BukkitRunnable() {
                            @Override
                            public void run() {
                                p1.sendMessage("\nThe goal is to loot the chests, obtaining armour, a melee weapon and some blocks.");
                                p1.playSound(p1.getLocation(), Sound.ORB_PICKUP, 1, 1);
                            }
                        };
                        BukkitRunnable b = new BukkitRunnable() {
                            @Override
                            public void run() {
                                p1.sendMessage("\nYou should then bridge to the other side and stand on the gold blocks. Have fun!");
                                p1.playSound(p1.getLocation(), Sound.ORB_PICKUP, 1, 1);
                            }
                        };
                        a.runTaskLater(Main.instance, 3*20);
                        b.runTaskLater(Main.instance, 6*20);
                    }
                };

                intro.runTaskLater(Main.instance, 10);

                board.getTeam("pb").setPrefix("§e None");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            p1.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
        }

        if (Objects.equals(mapname, "plainsone")) {
            Bukkit.getLogger().info("Loot Practice on " + mapname);
            Main.availableLootPracticeMaps.remove("plainsone");
            Location chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -64);
            Location chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -69);
            Location chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -64);

            Location player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -67.5);

            p1.teleport(player1Loc);
            p1.getInventory().clear();

            BukkitRunnable countdown = new BukkitRunnable() {
                int counter = 5;

                @Override
                public void run() {
                    if (counter == 0) {
                        this.cancel();
                        gameP2(p1, player1Loc, chest1, chest2, chest3);
                    } else {
                        Utils.sendTitle(p1, colorNumber(counter), "", 0, 0, 25);
                        counter--;
                    }
                }
            };
            countdown.runTaskTimer(Main.instance, 0L, 20);
        } else if (Objects.equals(mapname, "plainstwo")) {
            Bukkit.getLogger().info("Loot Practice on " + mapname);
            Main.availableLootPracticeMaps.remove("plainstwo");
            Location chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -79);
            Location chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -84);
            Location chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -79);

            Location player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -82.5);

            p1.teleport(player1Loc);
            p1.getInventory().clear();

            BukkitRunnable countdown = new BukkitRunnable() {
                int counter = 5;

                @Override
                public void run() {
                    if (counter == 0) {
                        this.cancel();
                        gameP2(p1, player1Loc, chest1, chest2, chest3);
                    } else {
                        Utils.sendTitle(p1, colorNumber(counter), "", 0, 0, 25);
                        counter--;
                    }
                }
            };
            countdown.runTaskTimer(Main.instance, 0L, 20);
        } else if (Objects.equals(mapname, "plainsthree")) {
            Bukkit.getLogger().info("Loot Practice on " + mapname);
            Main.availableLootPracticeMaps.remove("plainsthree");
            Location chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -95);
            Location chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -100);
            Location chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -95);

            Location player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -98.5);

            p1.teleport(player1Loc);
            p1.getInventory().clear();

            BukkitRunnable countdown = new BukkitRunnable() {
                int counter = 5;

                @Override
                public void run() {
                    if (counter == 0) {
                        this.cancel();
                        gameP2(p1, player1Loc, chest1, chest2, chest3);
                    } else {
                        Utils.sendTitle(p1, colorNumber(counter), "", 0, 0, 25);
                        counter--;
                    }
                }
            };
            countdown.runTaskTimer(Main.instance, 0L, 20);
        } else if (Objects.equals(mapname, "plainsfour")) {
            Bukkit.getLogger().info("Loot Practice on " + mapname);
            Main.availableLootPracticeMaps.remove("plainsfour");
            Location chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -111);
            Location chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -116);
            Location chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -111);

            Location player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -114.5);

            p1.teleport(player1Loc);
            p1.getInventory().clear();

            BukkitRunnable countdown = new BukkitRunnable() {
                int counter = 5;

                @Override
                public void run() {
                    if (counter == 0) {
                        this.cancel();
                        gameP2(p1, player1Loc, chest1, chest2, chest3);
                    } else {
                        Utils.sendTitle(p1, colorNumber(counter), "", 0, 0, 25);
                        counter--;
                    }
                }
            };
            countdown.runTaskTimer(Main.instance, 0L, 20);
        }
    }

    public static void gameP2(Player p1, Location player1Loc, Location chest1Loc, Location chest2Loc, Location chest3Loc) {
        Location cage = new Location(player1Loc.getWorld(), player1Loc.getX(), player1Loc.getY() - 1, player1Loc.getZ());
        p1.setGameMode(GameMode.SURVIVAL);

        if (chest1Loc.getBlock().getState() instanceof Container) {
            org.bukkit.block.Chest chest1 = (Chest) chest1Loc.getBlock().getState();
            Inventory chest1Inv = chest1.getInventory();
            chest1Inv.clear();
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_SWORD, 1));
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_CHESTPLATE, 1));
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.STONE, 8));
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.ARROW, 24));
        } else {
            chest1Loc.getBlock().setType(Material.CHEST);
            org.bukkit.block.Chest chest1 = (Chest) chest1Loc.getBlock().getState();
            Inventory chest1Inv = chest1.getInventory();
            chest1Inv.clear();
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_SWORD, 1));
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_CHESTPLATE, 1));
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.STONE, 8));
            chest1Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.ARROW, 24));
        }


        if (chest2Loc.getBlock().getState() instanceof Chest) {
            org.bukkit.block.Chest chest2 = (Chest) chest2Loc.getBlock().getState();
            Inventory chest2Inv = chest2.getInventory();
            chest2Inv.clear();
            ItemStack diaSword = new ItemStack(Material.DIAMOND_SWORD, 1);
            diaSword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
            chest2Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), diaSword);
            chest2Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_LEGGINGS, 1));
            chest2Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.WOOD, 32));
        } else {
            chest2Loc.getBlock().setType(Material.CHEST);
            org.bukkit.block.Chest chest2 = (Chest) chest2Loc.getBlock().getState();
            Inventory chest2Inv = chest2.getInventory();
            chest2Inv.clear();
            ItemStack diaSword = new ItemStack(Material.DIAMOND_SWORD, 1);
            diaSword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
            chest2Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), diaSword);
            chest2Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_LEGGINGS, 1));
            chest2Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.WOOD, 32));
        }

        if (chest3Loc.getBlock().getState() instanceof Chest) {
            org.bukkit.block.Chest chest3 = (Chest) chest3Loc.getBlock().getState();
            Inventory chest3Inv = chest3.getInventory();
            chest3Inv.clear();
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(1, 3 + 1)));
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_HELMET, 1));
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.STONE, 8));
            ItemStack bow = new ItemStack(Material.BOW, 1);
            bow.addEnchantment(Enchantment.ARROW_DAMAGE, ThreadLocalRandom.current().nextInt(1, 2 + 1));
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), bow);
        } else {
            chest3Loc.getBlock().setType(Material.CHEST);
            org.bukkit.block.Chest chest3 = (Chest) chest3Loc.getBlock().getState();
            Inventory chest3Inv = chest3.getInventory();
            chest3Inv.clear();
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(1, 3 + 1)));
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.IRON_HELMET, 1));
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), new ItemStack(Material.STONE, 8));
            ItemStack bow = new ItemStack(Material.BOW, 1);
            bow.addEnchantment(Enchantment.ARROW_DAMAGE, ThreadLocalRandom.current().nextInt(1, 2 + 1));
            chest3Inv.setItem(ThreadLocalRandom.current().nextInt(0, 26 + 1), bow);
        }


        cage.getBlock().setType(Material.AIR);
        Main.lootPracticeMapTimes.put(Main.playersInLootPractice.get(p1.getUniqueId()).split(":")[0], System.currentTimeMillis());
        playersInCage.remove(p1.getUniqueId());
        BukkitRunnable countdown = new BukkitRunnable() {
            int counter = 3;

            @Override
            public void run() {
                if (counter == 0) {
                    this.cancel();
                    cage.getBlock().setType(Material.GLASS);
                    BukkitRunnable countdown = new BukkitRunnable() {
                        int counter = 40;

                        @Override
                        public void run() {
                            if (counter == 0) {
                                this.cancel();
                                p1.setFallDistance(0F);
                            } else {
                                counter--;
                            }
                        }
                    };
                    countdown.runTaskTimer(Main.instance, 0L, 1);
                } else {
                    counter--;
                }
            }
        };
        countdown.runTaskTimer(Main.instance, 0L, 20);
    }

    public static void win(String mapname, Player player) {
        // Reset map
        Utils.resetBlocks(Main.lootPracticeBlocksPlaced.get(player.getUniqueId()));
        // Stop repeat PlayerMoveEvents
        Main.playersInLootPractice.remove(player.getUniqueId());

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - Main.lootPracticeMapTimes.get(mapname);
        float secs = (float) timeElapsed / 1000;
        int mili = (int) timeElapsed / 10;
        int pb = getPB(player);
        // publish to db if pb
        if (pb > timeElapsed) {
            try (PreparedStatement statement = Main.connection.prepareStatement("UPDATE skywarsPlayers SET lootPB = ? WHERE uuid=?;")) {
                statement.setFloat(1, timeElapsed); // wingPB, set to the new PB
                statement.setString(2, player.getUniqueId().toString()); // uuid, set to player uuid
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your PB to our database. Please open a ticket on the discord and screenshot your time!");
            }
            // display in #multiplayer-logs
            sendNewPBWebhook(player, Utils.prettifyNumber(timeElapsed));
            // show to player (we don't need to go through the db at this point)
            Scoreboard board = player.getScoreboard();
            board.getTeam("pb").setPrefix("§e " + secs);
            Utils.sendTitle(player, "§bNew PB! §e" + Utils.prettifyNumber(timeElapsed), "§a+15§r xp", 10, 10, 80);

//            (new BukkitRunnable() {
//                @Override
//                public void run() {
//                    board.getTeam("pb").setSuffix(" §b#" + Leaderboard.getPlace("wingPB", timeTakenNum, Leaderboard.Direction.Ascending));
//                }
//            }).runTaskAsynchronously(Main.instance);
        } else {
            Utils.sendTitle(player, "§bTime: §e" + Utils.prettifyNumber(timeElapsed), "§a+15§r xp", 10, 10, 80);
        }

        String timeTaken = Utils.prettifyNumber(timeElapsed);

        Scoreboard board = player.getScoreboard();
        board.getTeam("time").setPrefix("§e "+timeTaken);

            Float oldTime = playerTimes.get(player.getName());
            if(oldTime == null || timeElapsed < oldTime) {
                playerTimes.put(player.getName(), (float) timeElapsed);
            }
            updateSessionLeader();

        PlayerInventory pli1 = player.getInventory();
        pli1.clear();

        pli1.setHelmet(new ItemStack(Material.AIR));
        pli1.setChestplate(new ItemStack(Material.AIR));
        pli1.setLeggings(new ItemStack(Material.AIR));
        pli1.setBoots(new ItemStack(Material.AIR));

        // Give XP
        Main.givePlayerXP(player, 20);

        // We don't need this:
        // Main.availableLootPracticeMaps.add(mapname);
        BukkitRunnable countdown = new BukkitRunnable() {
            int counter = 5;

            @Override
            public void run() {
                if (counter == 0) {
                    this.cancel();
                    start(mapname, player);
                } else {
                    counter--;
                }
            }
        };
        countdown.runTaskTimer(Main.instance, 0L, 20);
    }

    public static void lose(Player player, String mapname, Integer data) {
//         Reset map
        Utils.resetBlocks(Main.lootPracticeBlocksPlaced.get(player.getUniqueId()));

        Main.availableLootPracticeMaps.add(mapname);
        Utils.sendPlayerToSpawn(player);
        Main.playersInLootPractice.remove(player.getUniqueId());
        player.getInventory().clear();

        leaderTeams.remove(player.getUniqueId());
        playerTimes.remove(player.getName());
        updateSessionLeader();
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

        PlayerInventory pli1 = player.getInventory();
        pli1.clear();

        pli1.setHelmet(new ItemStack(Material.AIR));
        pli1.setChestplate(new ItemStack(Material.AIR));
        pli1.setLeggings(new ItemStack(Material.AIR));
        pli1.setBoots(new ItemStack(Material.AIR));

        if (data == 0) {
            Utils.sendTitle(player, ChatColor.BOLD + "" + ChatColor.RED + "YOU DIED!", "", 0, 1, 50);
        } else if (data == 1) {
            Utils.sendTitle(player, ChatColor.BOLD + "" + ChatColor.RED + "YOU DIED!", ChatColor.YELLOW + "Get some armour to protect you!", 0, 1, 50);
        } else if (data == 2) {
            Utils.sendTitle(player, ChatColor.BOLD + "" + ChatColor.RED + "YOU DIED!", ChatColor.YELLOW + "Acquire a weapon for self-defence!", 0, 1, 50);
        }
    }

    public static void disconnect(Player player, String mapname) {
//         Reset map
        Utils.resetBlocks(Main.lootPracticeBlocksPlaced.get(player.getUniqueId()));

        Main.availableLootPracticeMaps.add(mapname);
        Main.playersInLootPractice.remove(player.getUniqueId());

        leaderTeams.remove(player.getUniqueId());
        playerTimes.remove(player.getName());
        updateSessionLeader();
        playersInCage.remove(player.getUniqueId());
    }

    private static String colorNumber(int i) {
        if (i >= 4) {
            return ChatColor.GREEN + String.valueOf(i);
        } else if (i == 3) {
            return ChatColor.YELLOW + String.valueOf(i);
        } else {
            return ChatColor.RED + String.valueOf(i);
        }
    }

    private static int getPB(Player player) {
        try(PreparedStatement statement = Main.connection.prepareStatement("SELECT lootPB FROM skywarsPlayers WHERE uuid=?;")) {
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
        return 999999;
    }

    private static void sendNewPBWebhook(Player player, String time) {
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
                author.addProperty("name", "New PB (Mode: Loot)");

                String playerName = player.getName();

                embed.addProperty("title", playerName + ": " + (time));

                embed.addProperty("description", playerName + " got new PB            |           on Loot Practice\n"
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
        }).runTaskAsynchronously(Main.instance);
    }

    private static void updateSessionLeader() {
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
            team.setSuffix((lowestTime[0] == 200*1000 ? "" : "§7: §a"+Utils.prettifyNumber(lowestTime[0])));
        }
    }
}
