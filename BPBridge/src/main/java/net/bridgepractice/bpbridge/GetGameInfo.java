package net.bridgepractice.bpbridge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GetGameInfo implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(sender.hasPermission("group.admin")) {
            if(args.length > 1) {
                String type = args[0];
                String value = args[1];
                String worldName;
                if(type.equals("player")) {
                    Player player = BPBridge.instance.getServer().getPlayerExact(value);
                    if(player == null) {
                        sender.sendMessage("§cPlayer not online!");
                        return true;
                    }
                    worldName = player.getWorld().getName();
                } else if(type.equals("world")) {
                    worldName = value;
                } else {
                    sender.sendMessage("§cUsage: /getgameinfo (player|worldName) <value>");
                    return true;
                }
                Game game = BPBridge.instance.gamesByWorld.get(worldName);
                if(game == null) {
                    sender.sendMessage("§cCan't find game");
                    return true;
                }
                sender.sendMessage("§aSuccessfully found game.\n"+game);
            } else {
                sender.sendMessage("§cUsage: /getgameinfo (player|worldName) <value>");
            }
            return true;
        }
        return false;
    }
}
