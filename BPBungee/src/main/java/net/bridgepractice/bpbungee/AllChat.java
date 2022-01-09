package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
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
        ProxiedPlayer player = ((ProxiedPlayer) sender);
        if(!(sender instanceof ProxiedPlayer)) {
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
