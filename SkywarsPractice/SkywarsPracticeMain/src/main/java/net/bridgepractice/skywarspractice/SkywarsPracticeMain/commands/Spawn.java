package net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Spawn implements CommandExecutor {
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Player player = (Player)sender;

    if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
      String[] data = Main.playersInLootPractice.get(player.getUniqueId()).split(":");
      String mapname = data[0];
      LootPractice.disconnect(player, mapname);
    }

    if (Main.playersInPearlPractice.containsKey(player.getUniqueId())) {
      String[] data = Main.playersInPearlPractice.get(player.getUniqueId()).split(":");
      String mapname = data[0];
      PearlPractice.disconnect(player, mapname);
    }

    Utils.sendPlayerToSpawn(player);
    return true;
  }

  public static boolean isPlayerAtSpawn(Player player) {
    return Utils.isLocationInLocation(player.getLocation(), new Location(Bukkit.getWorld("skywars"), 50, 88, -10), new Location(Bukkit.getWorld("skywars"), 105, -20, 73));
  }
}