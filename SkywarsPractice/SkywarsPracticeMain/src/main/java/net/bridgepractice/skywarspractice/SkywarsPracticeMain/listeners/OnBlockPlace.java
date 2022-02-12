package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
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
        // Loot Practice stuff
        if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
            List<Block> blocksPlaced = Main.lootPracticeBlocksPlaced.get(player.getUniqueId());
            blocksPlaced.add(e.getBlockPlaced());
            Main.lootPracticeBlocksPlaced.put(player.getUniqueId(), blocksPlaced);
        }
    }
}
