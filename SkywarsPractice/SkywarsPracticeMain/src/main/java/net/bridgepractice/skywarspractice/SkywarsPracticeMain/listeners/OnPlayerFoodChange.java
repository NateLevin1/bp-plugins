package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class OnPlayerFoodChange implements Listener {
    @EventHandler
    public void onPlayerFoodChange(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        Player player = (Player) e.getEntity();
        player.setFoodLevel(20);

    }
}
