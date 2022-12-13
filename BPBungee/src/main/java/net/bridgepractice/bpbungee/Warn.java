package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Warn extends Command {
    public Warn() {
        super("Warn");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(sender.hasPermission("bridgepractice.command.warn")) {
            if(args.length <= 1) {
                sender.sendMessage(new ComponentBuilder("Usage: /warn <player> reason").color(ChatColor.RED).create());
                return;
            }
            String playerName = args[0];
            ProxiedPlayer player = BPBungee.instance.getProxy().getPlayer(playerName);
            if(player == null) {
                sender.sendMessage(new ComponentBuilder("Unknown player \"" + playerName + "\"").color(ChatColor.RED).create());
                return;
            }

            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            String staffName = sender.getName();
            String senderName;
            if(sender instanceof ProxiedPlayer) {
                senderName = ((ProxiedPlayer) sender).getDisplayName();
            } else {
                senderName = sender.getName();
            }

            player.sendMessage(new ComponentBuilder("\n§e§lYou were warned by "+senderName+" §e§lfor").create());
            player.sendMessage(new ComponentBuilder("§c§l\"§e§l"+reason+"§c§l\"").create());
            BungeeTitle title = new BungeeTitle();
            title.title(TextComponent.fromLegacyText("§e§lYou were warned"));
            title.subTitle(TextComponent.fromLegacyText("§c§l"+reason));
            title.fadeIn(5);
            title.fadeOut(5);
            title.stay(4*20);
            player.sendTitle(title);
            player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§e§lYou were warned by "+senderName));
            BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, ()->player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§e§lYou were warned by "+senderName)), 2, TimeUnit.SECONDS);
            Utils.playSound(player, "mob.wither.spawn");

            sender.sendMessage(new ComponentBuilder("Successfully warned '"+playerName+"' for '"+reason+"'.").color(ChatColor.GREEN).create());
            Utils.sendPunishmentWebhook(false, true, "warned", reason, 0, staffName, player.getUniqueId().toString(), playerName, sender);
        } else {
            sender.sendMessage(new ComponentBuilder("You do not have permission to use this command").color(ChatColor.RED).create());
        }
    }
}
