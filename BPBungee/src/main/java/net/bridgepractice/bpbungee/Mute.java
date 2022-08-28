package net.bridgepractice.bpbungee;

import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Mute extends Command {
    public Mute() {
        super("Mute");
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("bridgepractice.moderation.chat")) {
            int days;
            if (args.length <= 1) {
                sender.sendMessage((new ComponentBuilder("You need to provide more arguments for this command!")).color(ChatColor.RED).create());
                return;
            }
            String playerName = args[0];
            try {
                days = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage((new ComponentBuilder("Invalid number of days " + args[1])).color(ChatColor.RED).create());
                return;
            }
            String reason = "Chat Infraction";
            if (args.length >= 3)
                reason = String.join(" ", Arrays.copyOfRange((CharSequence[]) args, 2, args.length));
            if (reason.length() >= 50) {
                sender.sendMessage((new ComponentBuilder("Reason is too long")).color(ChatColor.RED).create());
                return;
            }

            ProxiedPlayer mutedPlayer = BPBungee.instance.getProxy().getPlayer(playerName);
            String finalReason = reason;
            BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
                String playerUuid;
                try {
                    playerUuid = Utils.getUuidFromNameSync(playerName);
                } catch (IOException e) {
                    sender.sendMessage((new ComponentBuilder("'" + playerName + "' is not a valid username")).color(ChatColor.RED).create());
                    return;
                }
                try (PreparedStatement statement = BPBungee.connection.prepareStatement("UPDATE players SET mutedAt = ?, mutedDays = ?, muteReason = ? WHERE uuid=?;")) {
                    statement.setDate(1, new Date(System.currentTimeMillis()));
                    statement.setInt(2, days);
                    statement.setString(3, finalReason);
                    statement.setString(4, playerUuid);
                    statement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    sender.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
                    return;
                }

                sender.sendMessage(new ComponentBuilder("Successfully muted player " + playerName + ".").color(ChatColor.GREEN).create());

                String senderName;
                if(sender instanceof ProxiedPlayer) {
                    senderName = ((ProxiedPlayer) sender).getDisplayName();
                } else {
                    senderName = sender.getName();
                }

                BPBungee.instance.getProxy().broadcast(new ComponentBuilder("\n §c§l✕ §a" + (mutedPlayer != null ? mutedPlayer.getDisplayName() : playerName) + "§c §cwas §d§lmuted§c by " + senderName + "§c!\n").create());

                // if online, make them muted
                ProxiedPlayer onlinePlayer = BPBungee.instance.getProxy().getPlayer(playerName);
                if(onlinePlayer != null) {
                    BPBungee.instance.mutedPlayers.put(onlinePlayer.getUniqueId(), days);
                }

                Utils.sendPunishmentWebhook(false, "muted", finalReason, days, sender.getName(), sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "SERVER", playerName, sender);
            }, 0, TimeUnit.MILLISECONDS);
        } else {
            sender.sendMessage(new ComponentBuilder("You do not have permission to use this command.").color(ChatColor.RED).create());
        }
    }
}
