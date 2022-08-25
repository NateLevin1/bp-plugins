package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission("group.godlike")) {
                p.sendMessage("§cYou dont have permission");
                return true;
            } else {
                if (p.getAllowFlight() == true) {
                    p.setAllowFlight(false);
                    p.sendMessage("§cYou disabled fly!");
                    return true;
                }
                if (p.getAllowFlight() == false) {
                    p.setAllowFlight(true);
                    p.sendMessage("§aYou enabled fly!");
                    return true;
                }

            }
        }
        return false;
    }
}
