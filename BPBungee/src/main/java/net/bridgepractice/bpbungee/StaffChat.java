package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class StaffChat extends Command {
    public StaffChat() {
        super("StaffChat", "group.helper", "sc", "staffc", "schat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("You need to provide a message for this command!").color(ChatColor.RED).create());
        }
        String senderName;
        if(sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = ((ProxiedPlayer) sender);
            senderName = player.getDisplayName();
        } else {
            senderName = sender.getName();
        }
        String text = String.join(" ", args);
        sendToStaffChat(senderName, text);
    }

    public static void sendToStaffChat(String senderName, String text) {
        Utils.broadcastToPermission("group.helper", new ComponentBuilder("Staff").color(ChatColor.RED).append(new ComponentBuilder(" > ").color(ChatColor.DARK_GRAY).create()).append(senderName).append(new ComponentBuilder(": "+text).color(ChatColor.WHITE).create()).create());
    }
}
