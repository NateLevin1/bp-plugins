package net.bridgepractice.bpbridge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MapCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("§cYou must be a player to run this command!");
            return true;
        }

        Player player = ((Player) sender);
        GameInfo gameInfo = BPBridge.instance.gameOfPlayer(player);

        if(gameInfo == null) {
            player.sendMessage("§cYou aren't in a game right now!");
        } else {
            player.sendMessage("§aYou are currently playing on "+Maps.humanReadableMapName(gameInfo.map)+"!");
        }

        return true;
    }
}
