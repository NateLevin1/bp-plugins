package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import java.util.List;
import java.util.Objects;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class OnBlockPlace
  implements Listener {
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent e) {
    Player player = e.getPlayer();
    Block block = e.getBlock();

    if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
      String[] data = ((String)Main.playersInLootPractice.get(player.getUniqueId())).split(":");
      String mapname = data[0];
      if (Objects.equals(mapname, "plainsone")) {
        if (Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56.0D, 95.0D, -74.0D), new Location(Bukkit.getWorld("skywars"), 29.0D, -10.0D, -59.0D))) {
          List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
          blocksPlaced.add(e.getBlockPlaced());
          Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
        } else {
          e.setCancelled(true);
          player.sendMessage(ChatColor.RED + "You can't place blocks here!");
        }
      } else if (Objects.equals(mapname, "plainstwo")) {
        if (Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56.0D, 95.0D, -89.0D), new Location(Bukkit.getWorld("skywars"), 29.0D, -10.0D, -74.0D))) {
          List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
          blocksPlaced.add(e.getBlockPlaced());
          Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
        } else {
          e.setCancelled(true);
          player.sendMessage(ChatColor.RED + "You can't place blocks here!");
        }
      } else if (Objects.equals(mapname, "plainsthree")) {
        if (Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56.0D, 95.0D, -105.0D), new Location(Bukkit.getWorld("skywars"), 29.0D, -10.0D, -90.0D))) {
          List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
          blocksPlaced.add(e.getBlockPlaced());
          Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
        } else {
          e.setCancelled(true);
          player.sendMessage(ChatColor.RED + "You can't place blocks here!");
        }
      } else if (Objects.equals(mapname, "plainsfour")) {
        if (Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56.0D, 95.0D, -121.0D), new Location(Bukkit.getWorld("skywars"), 29.0D, -10.0D, -106.0D))) {
          List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
          blocksPlaced.add(e.getBlockPlaced());
          Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
        } else {
          e.setCancelled(true);
          player.sendMessage(ChatColor.RED + "You can't place blocks here!");
        }
      }
    }
  }
}