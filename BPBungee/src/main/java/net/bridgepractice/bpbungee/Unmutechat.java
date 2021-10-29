package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class Unmutechat extends Command {
    public Unmutechat() {
        super("Unmutechat", "bridgepractice.command.mutechat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        BPBungee.chatEnabled = true;
        sender.sendMessage(new ComponentBuilder("Successfully unmuted chat.").color(ChatColor.GREEN).create());
    }
}
