package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import java.util.Objects;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.PearlPractice;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class OnPlayerTeleport
  implements Listener {
  @EventHandler
  public void onPlayerTeleport(PlayerTeleportEvent e) {
    Player player = e.getPlayer();

    if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {

      e.setCancelled(true);
      player.teleport(e.getTo());

      if (Main.playersInPearlPractice.containsKey(player.getUniqueId()) && Main.playersInPearlPractice.get(player.getUniqueId()).contains(":")) {

        Location blockBelow = new Location(Bukkit.getWorld("skywars"), player.getLocation().getBlockX(), (player.getLocation().getBlockY() - 0.001), player.getLocation().getBlockZ());
        if (blockBelow.getBlock().getType() == Material.GRASS && Objects.equals(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[2], "launched")) {
          PearlPractice.win(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[0], player);
        }

        String mapname = Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[0];
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
      }
    }
  }
}