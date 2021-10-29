package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class Mutechat extends Command {
    public Mutechat() {
        super("Mutechat", "bridgepractice.command.mutechat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        BPBungee.chatEnabled = false;
        sender.sendMessage(new ComponentBuilder("Successfully muted chat.").color(ChatColor.GREEN).create());
    }
}
