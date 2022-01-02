package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;

public class Help extends Command {
    public Help() {
        super("Help", null, "h");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 1) {
            String page = args[0];
            switch(page) {
                case "messaging":
                    sender.sendMessage(new ComponentBuilder("\nBridgePractice Messaging Help").color(ChatColor.YELLOW).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("/msg <player> text").color(ChatColor.GOLD).append(new ComponentBuilder(" - Sends a message").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/r text").color(ChatColor.GOLD).append(new ComponentBuilder(" - Sends a message to who last messaged you").color(ChatColor.GRAY).create()).create());
                    break;
                case "singleplayer":
                    sender.sendMessage(new ComponentBuilder("\nBridgePractice Singleplayer Help").color(ChatColor.YELLOW).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("/wing").color(ChatColor.GOLD).append(new ComponentBuilder(" - Joins wing practice").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/prebow").color(ChatColor.GOLD).append(new ComponentBuilder(" - Joins prebow practice").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/bypass").color(ChatColor.GOLD).append(new ComponentBuilder(" - Joins bypass practice").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/bot").color(ChatColor.GOLD).append(new ComponentBuilder(" - Joins bot practice").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/lq").color(ChatColor.GOLD).append(new ComponentBuilder(" - Leave the current queue you are in").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/whereis <player>").color(ChatColor.GOLD).append(new ComponentBuilder(" - Tells you which mode a player is in").color(ChatColor.GRAY).create()).create());
                    break;
                case "legend":
                    sender.sendMessage(new ComponentBuilder("\n[LEGEND] Rank Help").color(ChatColor.YELLOW).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("/rank").color(ChatColor.GOLD).append(new ComponentBuilder(" - Shows information about your rank").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/rainbow text goes here").color(ChatColor.GOLD).append(new ComponentBuilder(" - Sends a rainbow chat message").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/fw").color(ChatColor.GOLD).append(new ComponentBuilder(" - Launch a firework! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/cookie").color(ChatColor.GOLD).append(new ComponentBuilder(" - Gives you a cookie! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/joinannounce").color(ChatColor.GOLD).append(new ComponentBuilder(" - Toggle join announcement messages").color(ChatColor.GRAY).create()).create());
                    break;
                case "godlike":
                    sender.sendMessage(new ComponentBuilder("\n[GODLIKE] Rank Help").color(ChatColor.YELLOW).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("/rank").color(ChatColor.GOLD).append(new ComponentBuilder(" - Shows information about your rank").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/rainbow text goes here").color(ChatColor.GOLD).append(new ComponentBuilder(" - Sends a rainbow chat message").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/fw").color(ChatColor.GOLD).append(new ComponentBuilder(" - Launch a firework! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/cookie").color(ChatColor.GOLD).append(new ComponentBuilder(" - Gives you a cookie! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/telestick").color(ChatColor.GOLD).append(new ComponentBuilder(" - Teleports you in the direction you look! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/joinannounce").color(ChatColor.GOLD).append(new ComponentBuilder(" - Toggle join announcement messages").color(ChatColor.GRAY).create()).create());
                    break;
                case "custom":
                    sender.sendMessage(new ComponentBuilder("\n[CUSTOM] Rank Help").color(ChatColor.YELLOW).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("/rank").color(ChatColor.GOLD).append(new ComponentBuilder(" - Shows information about your rank").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/rainbow text goes here").color(ChatColor.GOLD).append(new ComponentBuilder(" - Sends a rainbow chat message").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/fw").color(ChatColor.GOLD).append(new ComponentBuilder(" - Launch a firework! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/cookie").color(ChatColor.GOLD).append(new ComponentBuilder(" - Gives you a cookie! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/telestick").color(ChatColor.GOLD).append(new ComponentBuilder(" - Teleports you in the direction you look! (lobby only)").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/changetag").color(ChatColor.GOLD).append(new ComponentBuilder(" - Lets you change your custom tag").color(ChatColor.GRAY).create()).create());
                    sender.sendMessage(new ComponentBuilder("/joinannounce").color(ChatColor.GOLD).append(new ComponentBuilder(" - Toggle join announcement messages").color(ChatColor.GRAY).create()).create());
                    break;
            }
        } else {
            sender.sendMessage(new ComponentBuilder("\nBridgePractice Help").color(ChatColor.YELLOW).bold(true).create());
            sender.sendMessage(new ComponentBuilder("\nGeneral Commands").color(ChatColor.GOLD).bold(true).create());
            sender.sendMessage(new ComponentBuilder("/report <player>").color(ChatColor.GOLD).append(new ComponentBuilder(" - Brings up a report menu").color(ChatColor.GRAY).create()).create());
            sender.sendMessage(new ComponentBuilder("/rules").color(ChatColor.GOLD).append(new ComponentBuilder(" - View the server rules").color(ChatColor.GRAY).create()).create());
            sender.sendMessage(new ComponentBuilder("/l").color(ChatColor.GOLD).append(new ComponentBuilder(" - Takes you to the lobby").color(ChatColor.GRAY).create()).create());
            sender.sendMessage(new ComponentBuilder("/play <game>").color(ChatColor.GOLD).append(new ComponentBuilder(" - Queues you for a multiplayer game").color(ChatColor.GRAY).create()).create());
            sender.sendMessage(new ComponentBuilder("/duel <player> <game>").color(ChatColor.GOLD).append(new ComponentBuilder(" - Sends a duel request to a player").color(ChatColor.GRAY).create()).create());
            sender.sendMessage(new ComponentBuilder("/store").color(ChatColor.GOLD).append(new ComponentBuilder(" - Sends you to the store").color(ChatColor.GRAY).create()).create());
            sender.sendMessage(new ComponentBuilder("\nOther Commands (Click to see)").color(ChatColor.GOLD).bold(true).create());
            sender.sendMessage(new ComponentBuilder(" * ").color(ChatColor.RED).append(new ComponentBuilder("Messaging commands").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help messaging")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bView commands"))).create()).create());
            sender.sendMessage(new ComponentBuilder(" * ").color(ChatColor.RED).append(new ComponentBuilder("Singleplayer commands").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help singleplayer")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bView commands"))).create()).create());

            if(sender.hasPermission("group.custom")) {
                sender.sendMessage(new ComponentBuilder(" * ").color(ChatColor.RED).append(new ComponentBuilder("[CUSTOM]").color(ChatColor.GOLD).create()).append(new ComponentBuilder(" rank commands").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help godlike")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bView commands"))).create()).create());
            } else if(sender.hasPermission("group.godlike")) {
                sender.sendMessage(new ComponentBuilder(" * ").color(ChatColor.RED).append(new ComponentBuilder("[").color(ChatColor.DARK_PURPLE).append("GODLIKE").color(ChatColor.LIGHT_PURPLE).append("]").color(ChatColor.DARK_PURPLE).create()).append(new ComponentBuilder(" rank commands").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help godlike")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bView commands"))).create()).create());
            } else if(sender.hasPermission("group.legend")) {
                sender.sendMessage(new ComponentBuilder(" * ").color(ChatColor.RED).append(new ComponentBuilder("[").color(ChatColor.DARK_RED).append("LEGEND").color(ChatColor.RED).append("]").color(ChatColor.DARK_RED).create()).append(new ComponentBuilder(" rank commands").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help legend")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bView commands"))).create()).create());
            }

            sender.sendMessage(new ComponentBuilder("\nNeed more help? Click here to join the Discord!").color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://bridgepractice.net/discord")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to get the invite to the discord!"))).create());
        }
    }
}
