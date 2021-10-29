package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Ping extends Command {
    public Ping() {
        super("Ping");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) return;
        sender.sendMessage(new ComponentBuilder("Your ping is ").color(ChatColor.GREEN).append(new ComponentBuilder(((ProxiedPlayer) sender).getPing()+"").color(ChatColor.GOLD).create()).append(new ComponentBuilder("ms").color(ChatColor.GREEN).create()).create());
    }
}
