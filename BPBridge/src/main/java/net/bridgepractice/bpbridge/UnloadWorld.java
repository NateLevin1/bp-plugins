package net.bridgepractice.bpbridge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnloadWorld implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(sender.hasPermission("group.admin") && args.length == 1) {
            if(BPBridge.instance.internalUnloadWorld(args[0])) {
                // was success
                sender.sendMessage("§aSuccessfully unloaded world '"+args[0]+"'.");
            } else {
                sender.sendMessage("§cSomething went wrong unloading world '"+args[0]+"'! Check #multiplayer-logs in the discord to see what went wrong.");
            }
            return true;
        }
        return false;
    }
}
