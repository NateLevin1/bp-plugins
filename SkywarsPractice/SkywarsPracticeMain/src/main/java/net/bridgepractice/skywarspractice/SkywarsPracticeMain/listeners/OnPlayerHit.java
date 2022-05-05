package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class OnPlayerHit implements Listener {
    @EventHandler
    public static void onPlayerHit(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.PLAYER) return;

//        Player player = (Player) e.getEntity();
        e.setCancelled(true);
    }
}
