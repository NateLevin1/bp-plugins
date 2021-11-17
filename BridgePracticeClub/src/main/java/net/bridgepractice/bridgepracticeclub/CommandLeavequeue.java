package net.bridgepractice.bridgepracticeclub;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandLeavequeue implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo.removeFromQueue(player, true);
        } else {
            sender.sendMessage("You must be a player!");
        }
        return true;
    }
}
