package net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveQueue
  implements CommandExecutor {
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("You must be a player to use this command!");
      return true;
    }

    Player player = (Player) sender;

    if (Main.playersInLootPractice.containsKey(player.getUniqueId())) {
      String[] data = Main.playersInLootPractice.get(player.getUniqueId()).split(":");
      String mapname = data[0];
      LootPractice.disconnect(player, mapname);
    }

    if (Main.lootPracticeQueue.contains(player.getUniqueId())) {
      Main.lootPracticeQueue.remove(player.getUniqueId());
      player.sendMessage("Successfully left the Loot Practice queue!");
    }

    if (Main.playersInPearlPractice.containsKey(player.getUniqueId())) {
      if (Main.playersInPearlPractice.get(player.getUniqueId()).contains(":")) {
        String[] data = Main.playersInPearlPractice.get(player.getUniqueId()).split(":");
        String mapname = data[0];
        PearlPractice.disconnect(player, mapname);
      } else {
        Main.playersInPearlPractice.remove(player.getUniqueId());
      }
    }

    if (Main.pearlPracticeQueue.contains(player.getUniqueId())) {
      Main.pearlPracticeQueue.remove(player.getUniqueId());
      player.sendMessage("Successfully left the Pearl Practice queue!");
    }

    return true;
  }
}