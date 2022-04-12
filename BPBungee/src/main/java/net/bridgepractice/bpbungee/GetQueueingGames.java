package net.bridgepractice.bpbungee;

import com.google.gson.GsonBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;

public class GetQueueingGames extends Command {
    public GetQueueingGames() {
        super("GetQueueingGames", "group.admin", "getqueues");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length > 0) {
            String arg = args[0];
            if(arg.equals("clear")) {
                BPBungee.queueingGames.put(MultiplayerMode.unranked, new ArrayList<>());
                BPBungee.queueingGames.put(MultiplayerMode.pvp, new ArrayList<>());
                BPBungee.queueingGames.put(MultiplayerMode.nobridge, new ArrayList<>());
                sender.sendMessage(new ComponentBuilder("Cleared queueing games.").color(ChatColor.GREEN).create());
            } else {
                sender.sendMessage(new ComponentBuilder("Unknown option '"+arg+"'\nOnly 'clear' is valid").color(ChatColor.RED).create());
            }
        } else {
            sender.sendMessage(new ComponentBuilder(new GsonBuilder().setPrettyPrinting().create().toJson(BPBungee.queueingGames)).color(ChatColor.YELLOW).create());
        }
    }
}
