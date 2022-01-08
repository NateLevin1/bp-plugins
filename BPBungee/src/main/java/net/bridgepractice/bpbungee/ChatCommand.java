package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ChatCommand extends Command {
    public ChatCommand() {
        super("ChatCommand", null, "chat", "c");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0 || args[0] == null) {
            if (sender.hasPermission("group.helper")) {
                // is staff
                sender.sendMessage(new ComponentBuilder("1 argument required! Usage: /chat <a, all, s, staff>").color(ChatColor.RED).create());
                return;
            } else {
                // not staff
                sender.sendMessage(new ComponentBuilder("1 argument required! Usage: /chat <a, all>").color(ChatColor.RED).create());
                return;
            }
        }
        if(!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Must be a player to use this command!").color(ChatColor.RED).create());
            return;
        }
        ProxiedPlayer player = ((ProxiedPlayer) sender);
        if (args[0] == "a" || args[0] == "all") {
            if (BPBungee.playerChatChannels.containsKey(player.getUniqueId())) {
                BPBungee.playerChatChannels.remove(player.getUniqueId());
                player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("ALL").color(ChatColor.GOLD).append(" chat!").color(ChatColor.GREEN).create());
            } else {
                player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                return;
            }
        } else if (args[0] == "s" || args[0] == "staff") {
            if (BPBungee.playerChatChannels.containsKey(player.getUniqueId())) {
                if (BPBungee.playerChatChannels.get(player.getUniqueId()) == "staff") {
                    player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                    return;
                } else {
                    BPBungee.playerChatChannels.replace(player.getUniqueId(), "staff");
                }
            } else {
                BPBungee.playerChatChannels.put(player.getUniqueId(), "staff");
                player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("STAFF").color(ChatColor.GOLD).append(" chat!").color(ChatColor.GREEN).create());
            }
        }
    }
}
