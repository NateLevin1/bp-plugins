package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;

public class OnBlockBreak implements Listener {
    @EventHandler
    public static void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();

        // Loot Practice stuff
        if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
            if (e.getBlock().getType() != Material.CHEST || !(Main.lootPracticeBlocksPlaced.get(player.getUniqueId()).contains(e.getBlock()))) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Breaking blocks is not the aim of the game!");
            }
        }
    }
}
