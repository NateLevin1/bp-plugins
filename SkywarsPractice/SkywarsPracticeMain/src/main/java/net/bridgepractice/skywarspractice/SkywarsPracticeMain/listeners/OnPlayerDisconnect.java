package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.LootPractice;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.PearlPractice;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnPlayerDisconnect implements Listener {
  @EventHandler
  public void onPlayerDisconnect(PlayerQuitEvent e) {
    Player player = e.getPlayer();

    e.setQuitMessage("§7[§c-§7] " + player.getDisplayName() + "§7 left the server.");

    if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
      LootPractice.disconnect(player, Main.playersInLootPractice.get(player.getUniqueId()).split(":")[0]);
    }

    if (Main.playersInPearlPractice.containsKey(player.getUniqueId()))
      PearlPractice.disconnect(player, Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[0]);
  }
}