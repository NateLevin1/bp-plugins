package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;

public class SetChat extends Command {
    public SetChat() {
        super("SetChat", null, "chat", "c");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Must be a player to use this command!").color(ChatColor.RED).create());
            return;
        }
        if(args.length == 0 || args[0] == null) {
            if (sender.hasPermission("group.helper")) {
                // is staff
                sender.sendMessage(new ComponentBuilder("1 argument required! Usage: /chat <a, all, m, msg, message, r, rank, s, staff> [player]").color(ChatColor.RED).create());
            } else if (sender.hasPermission("group.legend")) {
                // not staff but has rank
                sender.sendMessage(new ComponentBuilder("1 argument required! Usage: /chat <a, all, m, msg, message, r, rank> [player]").color(ChatColor.RED).create());
            } else {
                // not staff
                sender.sendMessage(new ComponentBuilder("1 argument required! Usage: /chat <a, all, m, msg, message> [player]").color(ChatColor.RED).create());
            }
        }

        ProxiedPlayer player = ((ProxiedPlayer) sender);

        switch(args[0]) {
            case "a":
            case "all":
                if (BPBungee.playerChatChannels.containsKey(player.getUniqueId())) {
                    BPBungee.playerChatChannels.remove(player.getUniqueId());
                    player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("ALL").color(ChatColor.GOLD).append(" chat!").color(ChatColor.GREEN).create());
                } else {
                    player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                }
                break;
            case "s":
            case "staff":
                if (sender.hasPermission("group.helper")) {
                        if (BPBungee.playerChatChannels.get(player.getUniqueId()) == "staff") {
                            player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                        } else {
                            BPBungee.playerChatChannels.put(player.getUniqueId(), "staff");
                            player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("STAFF").color(ChatColor.GOLD).append(" chat!").color(ChatColor.GREEN).create());
                        }
                } else {
                    player.sendMessage(new ComponentBuilder("You don't have access to this channel!").color(ChatColor.RED).create());
                }
                break;
            case "r":
            case "rank":
                if (sender.hasPermission("group.legend")) {
                    if (BPBungee.playerChatChannels.get(player.getUniqueId()) == "rank") {
                        player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                    } else {
                        BPBungee.playerChatChannels.put(player.getUniqueId(), "rank");
                        player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("RANK").color(ChatColor.GOLD).append(" chat!").color(ChatColor.GREEN).create());
                    }
                } else {
                    player.sendMessage(new ComponentBuilder("You don't have access to this channel!").color(ChatColor.RED).create());
                }
                break;
            case "m":
            case "msg":
            case "message":
                if (BPBungee.playerChatChannels.get(player.getUniqueId()) == "message") {
                    player.sendMessage(new ComponentBuilder("You are already in that channel!").color(ChatColor.RED).create());
                } else {
                    if (args.length == 1) {
                        // Open message channel with last person that replied to them
                        BPBungee.NamedPlayer playerName = BPBungee.instance.playerReplyTo.get(player.getUniqueId());
                        if(playerName == null) {
                            sender.sendMessage(new ComponentBuilder("Nobody has messaged you!").color(ChatColor.RED).create());
                        }
                        String text = String.join(" ", args);
                        ProxiedPlayer playerToSendMessage = BPBungee.instance.getProxy().getPlayer(playerName.name);
                        if(playerToSendMessage != null) {
                            BPBungee.playerChatChannels.put(player.getUniqueId(), "message");
                            BPBungee.playerMessageChannel.put(player.getUniqueId(), playerToSendMessage.getUniqueId());
                            player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("MESSAGE").color(ChatColor.GOLD).append(" chat with " + playerToSendMessage.getDisplayName() + "!").color(ChatColor.GREEN).create());
                        } else {
                            sender.sendMessage(new ComponentBuilder("That player is not online! Use /chat all to get out of this channel.").color(ChatColor.RED).create());
                        }
                    } else {
                        String playerName = args[1];
                        if(playerName.equalsIgnoreCase(sender.getName())) {
                            sender.sendMessage(new ComponentBuilder("You cannot message yourself!").color(ChatColor.RED).create());
                        }
                        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        ProxiedPlayer playerToSendMessage = BPBungee.instance.getProxy().getPlayer(playerName);
                        if(playerToSendMessage != null) {
                            BPBungee.playerChatChannels.put(player.getUniqueId(), "message");
                            BPBungee.playerMessageChannel.put(player.getUniqueId(), playerToSendMessage.getUniqueId());
                            player.sendMessage(new ComponentBuilder("Switched to ").color(ChatColor.GREEN).append("MESSAGE").color(ChatColor.GOLD).append(" chat with " + playerToSendMessage.getDisplayName() + "!").color(ChatColor.GREEN).create());
                        } else {
                            sender.sendMessage(new ComponentBuilder("Unknown player \""+playerName+"\"").color(ChatColor.RED).create());
                        }
                    }
                }
                break;
            default:
                player.sendMessage(new ComponentBuilder("Not a valid channel! ("+args[0]+")").color(ChatColor.RED).create());
                break;
        }
    }
}
