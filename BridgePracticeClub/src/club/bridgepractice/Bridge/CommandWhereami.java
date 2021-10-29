package club.bridgepractice.Bridge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandWhereami implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage("§aYou are in "+Bridge.getPlayerReadableLocation(Bridge.instance.getPlayer(player.getUniqueId()).location)+"!");
            return true;
        }
        return false;
    }
}
