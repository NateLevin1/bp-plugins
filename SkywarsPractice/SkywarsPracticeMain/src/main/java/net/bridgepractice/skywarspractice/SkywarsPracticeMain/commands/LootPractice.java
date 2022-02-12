package net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import net.minecraft.server.v1_8_R3.Container;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class LootPractice implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
            Player player = (Player) sender;
            if (Main.lootPracticeQueue.contains(player.getUniqueId())) {
                player.sendMessage("You are already in the Loot Practice queue!");
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
        Main.lootPracticeQueue.remove(p1.getUniqueId());
        Main.availableLootPracticeMaps.remove(mapname);
        Main.playersInLootPractice.put(p1.getUniqueId(), mapname + ":" + "none");
        Main.lootPracticeBlocksPlaced.put(p1.getUniqueId(), new ArrayList<>());
        Main.lootPracticeBlocksBroken.put(p1.getUniqueId(), new ArrayList<>());

        Location chest1;
        Location chest2;
        Location chest3;
        Location player1Loc;
        BukkitRunnable countdown;

        switch (mapname) {
            case "plainsone":
                Main.availableLootPracticeMaps.remove("plainsone");
                chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -64);
                chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -69);
                chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -64);

                player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -67.5);

                p1.teleport(player1Loc);
                p1.getInventory().clear();

                Location finalPlayer1Loc = player1Loc;
                Location finalChest = chest1;
                Location finalChest1 = chest2;
                Location finalChest2 = chest3;
                countdown = new BukkitRunnable() {
                    int counter = 10;
                    @Override
                    public void run() {
                        if ( counter == 0) {
                            this.cancel();
                            gameP2(p1, finalPlayer1Loc, finalChest, finalChest1, finalChest2);
                        } else {
                            Utils.sendTitle(p1, String.valueOf(counter), "", 0, 0, 25);
                            counter--;
                        }
                    }
                };
                countdown.runTaskTimer(Main.instance, 0L, 20);
            case "plainstwo":
                Main.availableLootPracticeMaps.remove("plainstwo");
                chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -79);
                chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -84);
                chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -79);

                player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -82.5);

                p1.teleport(player1Loc);
                p1.getInventory().clear();

                Location finalPlayer1Loc1 = player1Loc;
                Location finalChest3 = chest1;
                Location finalChest4 = chest2;
                Location finalChest5 = chest3;
                countdown = new BukkitRunnable() {
                    int counter = 10;
                    @Override
                    public void run() {
                        if ( counter == 0) {
                            this.cancel();
                            gameP2(p1, finalPlayer1Loc1, finalChest3, finalChest4, finalChest5);
                        } else {
                            Utils.sendTitle(p1, String.valueOf(counter), "", 0, 0, 25);
                            counter--;
                        }
                    }
                };
                countdown.runTaskTimer(Main.instance, 0L, 20);
            case "plainsthree":
                Main.availableLootPracticeMaps.remove("plainsthree");
                chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -95);
                chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -100);
                chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -95);

                player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -98);

                p1.teleport(player1Loc);
                p1.getInventory().clear();

                Location finalPlayer1Loc2 = player1Loc;
                Location finalChest6 = chest2;
                Location finalChest7 = chest3;
                Location finalChest8 = chest1;
                countdown = new BukkitRunnable() {
                    int counter = 10;
                    @Override
                    public void run() {
                        if ( counter == 0) {
                            this.cancel();
                            gameP2(p1, finalPlayer1Loc2, finalChest8, finalChest6, finalChest7);
                        } else {
                            Utils.sendTitle(p1, String.valueOf(counter), "", 0, 0, 25);
                            counter--;
                        }
                    }
                };
                countdown.runTaskTimer(Main.instance, 0L, 20);
            case "plainsfour":
                Main.availableLootPracticeMaps.remove("plainsfour");
                chest1 = new Location(Bukkit.getWorld("skywars"), 51, 80, -111);
                chest2 = new Location(Bukkit.getWorld("skywars"), 54, 83, -116);
                chest3 = new Location(Bukkit.getWorld("skywars"), 51, 85, -111);

                player1Loc = new Location(Bukkit.getWorld("skywars"), 52.5, 88.1, -114.5);

                p1.teleport(player1Loc);
                p1.getInventory().clear();

                Location finalPlayer1Loc3 = player1Loc;
                Location finalChest9 = chest1;
                Location finalChest10 = chest2;
                Location finalChest11 = chest3;
                countdown = new BukkitRunnable() {
                    int counter = 10;
                    @Override
                    public void run() {
                        if ( counter == 0) {
                            this.cancel();
                            gameP2(p1, finalPlayer1Loc3, finalChest9, finalChest10, finalChest11);
                        } else {
                            Utils.sendTitle(p1, String.valueOf(counter), "", 0, 0, 25);
                            counter--;
                        }
                    }
                };
                countdown.runTaskTimer(Main.instance, 0L, 20);
        }
    }

    public static void gameP2(Player p1, Location player1Loc, Location chest1Loc, Location chest2Loc, Location chest3Loc) {
        Location cage = new Location(player1Loc.getWorld(), player1Loc.getX(), player1Loc.getY() - 1, player1Loc.getZ());

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
        Main.lootPracticeTimes.put(Main.playersInLootPractice.get(p1.getUniqueId()).split(":")[0], System.currentTimeMillis());
        BukkitRunnable countdown = new BukkitRunnable() {
            int counter = 3;

            @Override
            public void run() {
                if (counter == 0) {
                    this.cancel();
                    cage.getBlock().setType(Material.GLASS);
                } else {
                    counter--;
                }
            }
        };
        countdown.runTaskTimer(Main.instance, 0L, 20);
    }

    public static void win(String mapname, Player player) {
        // Reset map
        Utils.resetBlocks(Main.lootPracticeBlocksPlaced.get(player.getUniqueId()), Main.lootPracticeBlocksBroken.get(player.getUniqueId()));

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - Main.lootPracticeTimes.get(mapname);
        int secs = (int) timeElapsed / 1000;
        int mili = (int) timeElapsed / 10;

        PlayerInventory pli1= player.getInventory();
        pli1.clear();

        pli1.setHelmet(new ItemStack(Material.AIR));
        pli1.setChestplate(new ItemStack(Material.AIR));
        pli1.setLeggings(new ItemStack(Material.AIR));
        pli1.setBoots(new ItemStack(Material.AIR));

        player.sendMessage(String.valueOf(secs) + "." + String.valueOf(mili) + " seconds!");
        // We don't need this:
        // Main.availableLootPracticeMaps.add(mapname);
        start(mapname, player);
    }

    public static void lose(Player player, String mapname, Integer data) {
//         Reset map
        Utils.resetBlocks(Main.lootPracticeBlocksPlaced.get(player.getUniqueId()), Main.lootPracticeBlocksBroken.get(player.getUniqueId()));

        Main.availableLootPracticeMaps.add(mapname);
        Utils.sendPlayerToSpawn(player);
        Main.playersInLootPractice.remove(player.getUniqueId());
        player.getInventory().clear();

        PlayerInventory pli1= player.getInventory();
        pli1.clear();
        pli1.setItem(0, new ItemStack(Material.COMPASS, 1));

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
}
