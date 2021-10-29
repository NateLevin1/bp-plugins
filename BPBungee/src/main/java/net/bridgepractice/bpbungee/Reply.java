package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Reply extends Command {
    public Reply() {
        super("Reply", null, "r");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only a player can run this command!").color(ChatColor.RED).create());
            return;
        }
        ProxiedPlayer senderPlayer = ((ProxiedPlayer) sender);
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("You need to provide more arguments for this command!").color(ChatColor.RED).create());
            return;
        }
        BPBungee.NamedPlayer playerName = BPBungee.instance.playerReplyTo.get(senderPlayer.getUniqueId());
        if(playerName == null) {
            sender.sendMessage(new ComponentBuilder("Nobody has messaged you!").color(ChatColor.RED).create());
            return;
        }
        String text = String.join(" ", args);
        ProxiedPlayer player = BPBungee.instance.getProxy().getPlayer(playerName.name);
        if(player != null) {
            sender.sendMessage(new ComponentBuilder().appendLegacy("§dTo "+playerName.rankedName+"§7: "+text).create());
            player.sendMessage(new ComponentBuilder().appendLegacy("§dFrom "+senderPlayer.getDisplayName()+"§7: "+text).create());
            BPBungee.instance.playerReplyTo.put(player.getUniqueId(), new BPBungee.NamedPlayer(senderPlayer.getName(), senderPlayer.getDisplayName()));
        } else {
            sender.sendMessage(new ComponentBuilder("That player is not online!").color(ChatColor.RED).create());
        }
    }
}
