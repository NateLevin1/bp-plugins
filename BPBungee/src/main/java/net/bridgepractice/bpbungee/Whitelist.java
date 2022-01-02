package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class Whitelist extends Command {
    public static boolean enabled = false;
    public Whitelist() {
        super("Whitelist", "group.admin");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("Usage: /whitelist (enable|disable)").color(ChatColor.RED).create());
            return;
        }
        String option = args[0];
        switch(option) {
            case "enable":
            case "on":
                enabled = true;
                sender.sendMessage(new ComponentBuilder("Enabled whitelist!").color(ChatColor.GREEN).create());
                break;
            case "disable":
            case "off":
                enabled = false;
                sender.sendMessage(new ComponentBuilder("Disabled whitelist!").color(ChatColor.GREEN).create());
                break;
            default:
                sender.sendMessage(new ComponentBuilder("Usage: /whitelist (enable|disable)").color(ChatColor.RED).create());
                break;
        }
    }
}
