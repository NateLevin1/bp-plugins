package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class OnPlayerDamage implements Listener {
  @EventHandler
  public static void onPlayerDamage(EntityDamageEvent e) {
    if (e.getEntityType() != EntityType.PLAYER) return;
    Player player = (Player) e.getEntity();

    if (e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
        e.setCancelled(true);
    }
  }
}