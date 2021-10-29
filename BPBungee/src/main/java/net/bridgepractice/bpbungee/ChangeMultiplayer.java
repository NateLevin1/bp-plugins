package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class ChangeMultiplayer extends Command {
    public ChangeMultiplayer() {
        super("ChangeMultiplayer", "bridgepractice.broadcast", "cm");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("Usage: /cm (enable|disable)").color(ChatColor.RED).create());
            return;
        }
        if(args[0].equals("enable")) {
            BPBungee.multiplayerEnabled = true;
            sender.sendMessage(new ComponentBuilder("Successfully enabled multiplayer.").color(ChatColor.GREEN).create());
        } else if(args[0].equals("disable")) {
            BPBungee.multiplayerEnabled = false;
            sender.sendMessage(new ComponentBuilder("Successfully disabled multiplayer.").color(ChatColor.GREEN).create());
        } else {
            sender.sendMessage(new ComponentBuilder("Usage: /cm (enable|disable)").color(ChatColor.RED).create());
        }
    }
}
