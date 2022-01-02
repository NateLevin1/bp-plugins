package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;

public class Message extends Command {
    public Message() {
        super("Message", null, "msg", "w");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length <= 1) {
            sender.sendMessage(new ComponentBuilder("Usage: /msg <player> <message here>").color(ChatColor.RED).create());
            return;
        }
        String senderName;
        if(sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = ((ProxiedPlayer) sender);
            senderName = player.getDisplayName();
        } else {
            senderName = sender.getName();
        }
        String playerName = args[0];
        if(playerName.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(new ComponentBuilder("You cannot message yourself!").color(ChatColor.RED).create());
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        ProxiedPlayer player = BPBungee.instance.getProxy().getPlayer(playerName);
        if(player != null) {
            sender.sendMessage(new ComponentBuilder("§dTo "+player.getDisplayName()).append(": "+text).color(ChatColor.GRAY).create());
            player.sendMessage(new ComponentBuilder("§dFrom "+senderName).append(": "+text).color(ChatColor.GRAY).create());
            BPBungee.instance.playerReplyTo.put(player.getUniqueId(), new BPBungee.NamedPlayer(sender.getName(), senderName));
            Utils.log(new ComponentBuilder("From "+senderName).color(ChatColor.LIGHT_PURPLE).append(" To "+player.getDisplayName()).color(ChatColor.LIGHT_PURPLE).append(": "+text).color(ChatColor.GRAY).create(), "bridgepractice.moderation.socialspy.show");
        } else {
            sender.sendMessage(new ComponentBuilder("Unknown player \""+playerName+"\"").color(ChatColor.RED).create());
        }
    }
}
