package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class RankChat extends Command {
    public RankChat() {
        super("RankChat", "group.legend", "rc", "rankc", "rchat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("You need to provide a message for this command!").color(ChatColor.RED).create());
            return;
        }
        String senderName;
        if(sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = ((ProxiedPlayer) sender);
            senderName = player.getDisplayName();
        } else {
            senderName = sender.getName();
        }
        String text = String.join(" ", args);
        sendToRankChat(senderName, text);
    }

    public static void sendToRankChat(String senderName, String text) {
        Utils.broadcastToPermission("group.legend", new ComponentBuilder("Rank").color(ChatColor.GOLD).append(new ComponentBuilder(" > ").color(ChatColor.DARK_GRAY).create()).append(senderName).append(new ComponentBuilder(": "+text).color(ChatColor.WHITE).create()).create());
    }
}
