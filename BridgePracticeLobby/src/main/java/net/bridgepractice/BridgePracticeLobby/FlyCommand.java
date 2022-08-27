package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;

        if (!player.hasPermission("group.godlike")) {
            player.sendMessage(ChatColor.RED + "Toggling fly is only available to " + ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "GODLIKE" + ChatColor.DARK_PURPLE + "]" + ChatColor.RED + " and above!");
            return true;
        }

        player.setAllowFlight(!player.getAllowFlight());
        player.sendMessage(player.getAllowFlight() ? ChatColor.GREEN + "You enabled fly." : ChatColor.RED + "You disabled fly.");
        return true;
    }
}
