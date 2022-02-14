package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

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

import java.util.List;

public class OnBlockPlace implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        // Loot Practice stuff
        if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
            String[] data = Main.playersInLootPractice.get(player.getUniqueId()).split(":");
            String mapname = data[0];
            switch(mapname) {
                case "plainsone":
                    if ((Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56,95, -74), new Location(Bukkit.getWorld("skywars"), 29, -10, -59)))) {
                        List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
                        blocksPlaced.add(e.getBlockPlaced());
                        Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
                    } else {
                        e.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You can't place blocks here!");
                    }
                case "plainstwo":
                    if ((Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56,95, -89), new Location(Bukkit.getWorld("skywars"), 29, -10, -74)))) {
                        List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
                        blocksPlaced.add(e.getBlockPlaced());
                        Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
                    } else {
                        e.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You can't place blocks here!");
                    }
                case "plainsthree":
                    if ((Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56,95, -105), new Location(Bukkit.getWorld("skywars"), 29, -10, -90)))) {
                        List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
                        blocksPlaced.add(e.getBlockPlaced());
                        Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
                    } else {
                        e.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You can't place blocks here!");
                    }
                case "plainsfour":
                    if ((Utils.isLocationInLocation(block.getLocation(), new Location(Bukkit.getWorld("skywars"), 56,95, -121), new Location(Bukkit.getWorld("skywars"), 29, -10, -106)))) {
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
