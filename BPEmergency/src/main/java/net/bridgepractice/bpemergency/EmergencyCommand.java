package net.bridgepractice.bpemergency;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class EmergencyCommand extends Command {
    public EmergencyCommand() {
        super("Emergency", "bridgepractice.command.emergency");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) return;
        String s = args[0];
        switch(s) {
            case "enable":
                BPEmergency.enabled = true;
                sender.sendMessage(new ComponentBuilder("Enabled.").color(ChatColor.GREEN).create());
                break;
            case "disable":
                BPEmergency.enabled = false;
                sender.sendMessage(new ComponentBuilder("Disabled.").color(ChatColor.GREEN).create());
                break;
            case "kick":
                if(sender instanceof ProxiedPlayer) {
                    ProxiedPlayer player = ((ProxiedPlayer) sender);
                    sender.sendMessage(new ComponentBuilder("Kicking all players.").color(ChatColor.GREEN).create());
                    for(ProxiedPlayer p : player.getServer().getInfo().getPlayers()) {
                        p.disconnect(new ComponentBuilder("Click \"Reconnect\" to continue playing.").bold(true).color(ChatColor.AQUA).create());
                    }
                }
                break;
        }
    }
}
