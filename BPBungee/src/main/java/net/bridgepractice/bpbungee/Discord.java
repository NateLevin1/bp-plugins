package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Discord extends Command {
    public Discord() {
        super("Discord");
    }
    public void execute(CommandSender sender, String[] args) {
        if(sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = ((ProxiedPlayer) sender);
            player.sendMessage(new ComponentBuilder("\nClick to open the link:").color(ChatColor.GREEN).create());
            player.sendMessage(new ComponentBuilder("http://bridgepractice.net/discord").color(ChatColor.AQUA).underlined(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://bridgepractice.net/discord")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to get the invite to the discord!"))).create());
        } else {
            sender.sendMessage(new ComponentBuilder("Only players can use this command!").color(ChatColor.RED).create());
        }
    }
}
