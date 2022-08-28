package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class AllChat extends Command {
    public AllChat() {
        super("AllChat", null, "ac", "allc", "achat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("You need to provide a message for this command!").color(ChatColor.RED).create());
            return;
        }
        if(!(sender instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = ((ProxiedPlayer) sender);
        if (BPBungee.mutedPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(new ComponentBuilder()
                    .append((new ComponentBuilder("----------------------------------------------------------------")).color(ChatColor.RED).strikethrough(true).create())
                    .append((new ComponentBuilder("\nYou cannot chat because you are muted.")).strikethrough(false).color(ChatColor.RED).create())
                    .append((new ComponentBuilder("\nYour mute will expire in ")).color(ChatColor.GRAY).append((new ComponentBuilder((new StringBuilder()).append(BPBungee.mutedPlayers.get(player.getUniqueId())).append(" days").toString())).color(ChatColor.RED).create()).create())
                    .append((new ComponentBuilder("\nReason: ")).color(ChatColor.GRAY).append((new ComponentBuilder(Utils.getMuteReason(player))).color(ChatColor.RED).create()).create())
                    .append((new ComponentBuilder("\n\nTo appeal your mute, ")).color(ChatColor.GRAY).append((new ComponentBuilder("join the Discord (click)")).color(ChatColor.AQUA).underlined(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://bridgepractice.net/discord")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Content[]{(Content) new Text("to go to the Discord invite.")})).create()).create())
                    .append((new ComponentBuilder("\n----------------------------------------------------------------")).color(ChatColor.RED).strikethrough(true).underlined(false).event((ClickEvent) null).event((HoverEvent) null).create())
                    .create());
            return;
        }
        String text = String.join(" ", args);
        if (BPBungee.playerChatChannels.containsKey(player.getUniqueId())) {
            String channel = BPBungee.playerChatChannels.get(player.getUniqueId());
            BPBungee.playerChatChannels.remove(player.getUniqueId());
            player.chat(text);
            BPBungee.playerChatChannels.put(player.getUniqueId(), channel);
        } else {
            player.chat(text);
        }
    }
}