package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.LootPractice;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

public class OnPlayerMove implements Listener {
    @EventHandler
    public static void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (player.getLocation().getY() <= 1) {
            if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
                String[] parts = Main.playersInLootPractice.get(player.getUniqueId()).split(":");

                LootPractice.lose(player, parts[0], 0);
            } else {
                Utils.sendPlayerToSpawn(player);
            }
        }

        // Loot Practice Managing
        if (Main.lootPracticeQueue.size() > 0 && Main.availableLootPracticeMaps.size() > 0) {
            // New game
            LootPractice.start(Main.availableLootPracticeMaps.get(Main.availableLootPracticeMaps.size() - 1), Bukkit.getPlayer(Main.lootPracticeQueue.get(0)));
        }

        // Is in Loot Practice
        if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
            String[] data = Main.playersInLootPractice.get(player.getUniqueId()).split(":");
            String mapname = data[0];
            if (Objects.equals(mapname, "plainsone")) {
                if (!(Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -74), new Location(Bukkit.getWorld("skywars"), 29, -10, -59))) && !(LootPractice.playersInCage.contains(player.getUniqueId()))) {
                    player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -68));
                }
            } else if (Objects.equals(mapname, "plainstwo")) {
                if (!(Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -89), new Location(Bukkit.getWorld("skywars"), 29, -10, -74))) && !(LootPractice.playersInCage.contains(player.getUniqueId()))) {
                    player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -83));
                }
            } else if (Objects.equals(mapname, "plainsthree")) {
                if (!(Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -105), new Location(Bukkit.getWorld("skywars"), 29, -10, -90))) && !(LootPractice.playersInCage.contains(player.getUniqueId()))) {
                    player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -99));
                }
            } else if (Objects.equals(mapname, "plainsfour")) {
                if (!(Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -121), new Location(Bukkit.getWorld("skywars"), 29, -10, -106))) && !(LootPractice.playersInCage.contains(player.getUniqueId()))) {
                    player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -115));
                }
            }

            Location blockBelow = new Location(Bukkit.getWorld("skywars"), player.getLocation().getBlockX(), player.getLocation().getBlockY() - 1, player.getLocation().getBlockZ());
            if (blockBelow.getBlock().getType() == Material.GOLD_BLOCK) {
                // Player touched Gold block and won

                int weapon = 0;
                for (ItemStack item : player.getInventory()) {
                    if (item != null) {
                        if (item.getType() == Material.IRON_SWORD) {
                            ++weapon;
                        } else if (item.getType() == Material.DIAMOND_SWORD) {
                            ++weapon;
                        }
                    }
                }
                if (player.getInventory().getHelmet() == null && player.getInventory().getChestplate() == null && player.getInventory().getLeggings() == null && player.getInventory().getBoots() == null) {
                    LootPractice.lose(player, mapname, 1);
                } else if (weapon == 0) {
                    LootPractice.lose(player, mapname, 2);
                } else {
                    LootPractice.win(mapname, player);
                }
            }
        } else {
            player.setFallDistance(0F);
        }
    }
}
