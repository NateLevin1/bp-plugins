package club.bridgepractice.Bridge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandWhereIs implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length != 1) {
            sender.sendMessage("§cYou need to provide a player for this command!");
            return true;
        }
        String playerName = args[0];
        Player player = Bridge.instance.getServer().getPlayerExact(playerName);
        if(player != null) {
            sender.sendMessage("§a"+playerName+" is in "+Bridge.getPlayerReadableLocation(Bridge.instance.getPlayer(player.getUniqueId()).location)+".");
        } else {
            sender.sendMessage("§cUnknown player \""+playerName+"\"");
        }
        return true;
    }
}
