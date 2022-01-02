package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;
import java.util.Arrays;

public class EditBan extends Command {
    public EditBan() {
        super("EditBan", "bridgepractice.moderation.players", "silentban");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        // TODO: this is mostly copy+paste from Ban.java -- can we combine them?
        if(args.length <= 1) {
            sender.sendMessage(new ComponentBuilder("Usage: /editban <player> <days> [reason]").color(ChatColor.RED).create());
            return;
        }
        String playerName = args[0];
        int days;
        try {
            days = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(new ComponentBuilder("Invalid number of days "+args[1]).color(ChatColor.RED).create());
            return;
        }
        String reason = "Unfair Advantage";
        if(args.length >= 3) {
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }
        if(reason.length() >= 50) {
            sender.sendMessage(new ComponentBuilder("Reason is too long").color(ChatColor.RED).create());
            return;
        }

        String finalReason = reason;
        BPBungee.instance.getProxy().getScheduler().runAsync(BPBungee.instance, ()->{
            String playerUuid;
            try {
                playerUuid = Utils.getUuidFromNameSync(playerName);
            } catch (IOException e) {
                sender.sendMessage(new ComponentBuilder("✕ '" + playerName + "' is not a valid username").color(ChatColor.RED).create());
                return;
            }

            boolean shouldReturn = Ban.applyBan(playerName, days, finalReason, playerUuid, sender);
            if(shouldReturn) return;

            sender.sendMessage(new ComponentBuilder("Successfully edited ban of player " + playerName + ".").color(ChatColor.GREEN).create());

            Utils.sendPunishmentWebhook(true, "silently banned", finalReason, days, sender.getName(), sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "SERVER", playerName, sender);
        });
    }
}
