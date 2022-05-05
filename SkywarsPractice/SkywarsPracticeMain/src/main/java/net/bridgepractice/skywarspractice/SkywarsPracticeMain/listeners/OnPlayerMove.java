package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import java.util.Objects;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.LootPractice;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.PearlPractice;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.Spawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class OnPlayerMove implements Listener {
  @EventHandler
  public static void onPlayerMove(PlayerMoveEvent e) {
    Player player = e.getPlayer();
    if (player.getLocation().getY() <= 30) {
      if (Spawn.isPlayerAtSpawn(player)) {
        Utils.sendPlayerToSpawn(player);
      } else if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
        String[] data = Main.playersInLootPractice.get(player.getUniqueId()).split(":");
        String mapname = data[0];

        LootPractice.lose(player, mapname, 0);
      }
    }


    if (Main.lootPracticeQueue.size() > 0 && Main.availableLootPracticeMaps.size() > 0) {
      LootPractice.start(Main.availableLootPracticeMaps.get(Main.availableLootPracticeMaps.size() - 1), Bukkit.getPlayer(Main.lootPracticeQueue.get(0)), false);
    }


    if (Main.pearlPracticeQueue.size() > 0 && Main.availablePearlPracticeMaps.size() > 0) {
      PearlPractice.start(Main.availablePearlPracticeMaps.get(Main.availablePearlPracticeMaps.size() - 1), Bukkit.getPlayer(Main.pearlPracticeQueue.get(0)), false);
    }


    if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
      String[] data = Main.playersInLootPractice.get(player.getUniqueId()).split(":");
      String mapname = data[0];
      if (Objects.equals(mapname, "plainsone")) {
        if (!Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -74), new Location(Bukkit.getWorld("skywars"), 29, -10, -59))) {
          player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -68));
        }
      } else if (Objects.equals(mapname, "plainstwo")) {
        if (!Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -89), new Location(Bukkit.getWorld("skywars"), 29, -10, -74))) {
          player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -83));
        }
      } else if (Objects.equals(mapname, "plainsthree")) {
        if (!Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -105), new Location(Bukkit.getWorld("skywars"), 29, -10, -90))) {
          player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -99));
        }
      } else if (Objects.equals(mapname, "plainsfour") &&
        !Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 56, 95, -121), new Location(Bukkit.getWorld("skywars"), 29, -10, -106))) {
        player.teleport(new Location(Bukkit.getWorld("skywars"), 52, 83, -115));
      }


      Location blockBelow = new Location(Bukkit.getWorld("skywars"), player.getLocation().getBlockX(), (player.getLocation().getBlockY() - 1), player.getLocation().getBlockZ());
      if (blockBelow.getBlock().getType() == Material.GOLD_BLOCK) {


        int weapon = 0;
        for (ItemStack item : player.getInventory()) {
          if (item != null) {
            if (item.getType() == Material.IRON_SWORD || item.getType() == Material.DIAMOND_SWORD) {
              weapon++;
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
      player.setFallDistance(0.0F);
    }

    if (Main.playersInPearlPractice.containsKey(player.getUniqueId())) {
      if (Main.playersInPearlPractice.get(player.getUniqueId()).contains(":")) {
        String[] data = Main.playersInPearlPractice.get(player.getUniqueId()).split(":");
        String mapname = data[0];

        switch (mapname) {
          case "pearlone":
            if (!Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), -41, 95, -64), new Location(Bukkit.getWorld("skywars"), 21, -10, -124))) {
              PearlPractice.lose(mapname, player);
            }
            break;
          case "pearltwo":
            if (!Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), -54, 95, -64), new Location(Bukkit.getWorld("skywars"), -34, -10, -124))) {
              PearlPractice.lose(mapname, player);
            }
            break;
          case "pearlthree":
            if (!Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), -67, 95, -64), new Location(Bukkit.getWorld("skywars"), -47, -10, -124))) {
              PearlPractice.lose(mapname, player);
            }
            break;
          case "pearlfour":
            if (!Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), -80, 95, -64), new Location(Bukkit.getWorld("skywars"), -60, -10, -124))) {
              PearlPractice.lose(mapname, player);
            }
            break;
          default:
            throw new IllegalArgumentException("Unexpected mapname: " + mapname);
        }

        Location blockBelow = new Location(Bukkit.getWorld("skywars"), player.getLocation().getBlockX(), (player.getLocation().getBlockY() - 1), player.getLocation().getBlockZ());
        if (blockBelow.getBlock().getType() == Material.GOLD_BLOCK && Objects.equals(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[2], "ingame"))
        {
          PearlPractice.win(mapname, player);
        }

        if (blockBelow.getBlock().getType() == Material.GRASS && Objects.equals(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[2], "waiting")) {
          PearlPractice.gameP2(mapname, player);
        }
        if (blockBelow.getBlock().getType() == Material.GRASS && Objects.equals(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[2], "launched") && !player.getInventory().contains(Material.ENDER_PEARL)) {
          PearlPractice.win(mapname, player);
        }

        if (player.getLocation().getY() < 70) {
          String[] parts = Main.playersInPearlPractice.get(player.getUniqueId()).split(":");

          PearlPractice.lose(parts[0], player);
        }
      }
    }
  }
}