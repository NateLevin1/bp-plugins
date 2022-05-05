package net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands;

import java.util.Objects;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Debug
  implements CommandExecutor
{
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Player player = (Player)sender;

    if (args.length == 0 || args.length == 2) {
      player.sendMessage(ChatColor.RED + "2 arguments required!");
      return true;
    }

    String mode = args[0];
    String var = args[1];

    if (Objects.equals(mode, "lootpractice")) {
      switch (var) {
        case "queue":
          player.sendMessage(Main.lootPracticeQueue.toString());
        case "maps":
          player.sendMessage(Main.availableLootPracticeMaps.toString());
        case "players":
          player.sendMessage(Main.playersInLootPractice.toString());
        case "times":
          player.sendMessage(Main.lootPracticeMapTimes.toString());
          break;
      }
    }
    return true;
  }
}