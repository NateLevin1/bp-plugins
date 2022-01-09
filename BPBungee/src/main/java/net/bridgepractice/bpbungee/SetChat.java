package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class SetChat extends Command {
    public SetChat() {
        super("SetChat", null, "chat", "c");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Must be a player to use this command!").color(ChatColor.RED).create());
            return;
        }
        if(args.length == 0 || args[0] == null) {
            if (sender.hasPermission("group.helper")) {
                // is staff
                sender.sendMessage(new ComponentBuilder("1 argument required! Usage: /chat <a, all, s, staff>").color(ChatColor.RED).create());
            } else {
                // not staff
                sender.sendMessage(new ComponentBuilder("1 argument required! Usage: /chat <a, all>").color(ChatColor.RED).create());
            }
            return;
        }

        ProxiedPlayer player = ((ProxiedPlayer) sender);

        switch(args[0]) {
            case "a":
            case "all":
                if (BPBungee.playerChatChannels.containsKey(player.getUniqueId())) {
                    BPBungee.playerChatChannels.remove(player.getUniqueId());
                    player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("ALL").color(ChatColor.GOLD).append(" chat!").color(ChatColor.GREEN).create());
                } else {
                    player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                }
                break;
            case "s":
            case "staff":
                if (sender.hasPermission("group.helper")) {
                        if (BPBungee.playerChatChannels.get(player.getUniqueId()) == "staff") {
                            player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                        } else {
                            BPBungee.playerChatChannels.put(player.getUniqueId(), "staff");
                            player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("STAFF").color(ChatColor.GOLD).append(" chat!").color(ChatColor.GREEN).create());
                        }
                } else {
                    player.sendMessage(new ComponentBuilder("You don't have access to this channel!").color(ChatColor.RED).create());
                }
                break;
            default:
                player.sendMessage(new ComponentBuilder("Not a valid channel! ("+args[0]+")").color(ChatColor.RED).create());
                break;
        }
    }
}
