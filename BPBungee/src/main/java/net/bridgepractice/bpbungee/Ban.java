package net.bridgepractice.bpbungee;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Ban extends Command {
    public Ban() {
        super("Ban", "bridgepractice.moderation.players");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length <= 1) {
            sender.sendMessage(new ComponentBuilder("Usage: /ban <player> <days> [reason]").color(ChatColor.RED).create());
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
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, ()->{
            String playerUuid;
            try {
                playerUuid = Utils.getUuidFromNameSync(playerName);
            } catch (IOException e) {
                sender.sendMessage(new ComponentBuilder("✕ '" + playerName + "' is not a valid username").color(ChatColor.RED).create());
                return;
            }

            boolean shouldReturn = applyBan(playerName, days, finalReason, playerUuid, sender);
            if(shouldReturn) return;

            sender.sendMessage(new ComponentBuilder("Successfully banned player " + playerName + ".").color(ChatColor.GREEN).create());

            String senderName;
            if(sender instanceof ProxiedPlayer) {
                senderName = ((ProxiedPlayer) sender).getDisplayName();
            } else {
                senderName = sender.getName();
            }

            BPBungee.instance.getProxy().broadcast(new ComponentBuilder("\n §c§l✕ §b" + playerName + "§c §cwas §d§lbanned§c by " + senderName + "§c!\n").create());

            Utils.sendPunishmentWebhook("banned", finalReason, days, sender.getName(), sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : "SERVER", playerName, sender);
        }, 0, TimeUnit.MILLISECONDS);
    }

    public static boolean applyBan(String playerName, int days, String finalReason, String playerUuid, CommandSender errorTo) {
        String playerIp = null;
        // if online, kick them
        ProxiedPlayer onlinePlayer = BPBungee.instance.getProxy().getPlayer(playerName);
        if(onlinePlayer != null) {
            playerIp = onlinePlayer.getAddress().getAddress().getHostAddress();
            onlinePlayer.disconnect(Utils.getBanMessage(days, finalReason, false));
        }

        String resetStatsSql = "xp = 0, wingPB = NULL, prebowHits = 0, bypassGoals = 0, bypassStartPB = NULL, bypassEarlyPB = NULL, bypassMiddlePB = NULL, bypassLatePB = NULL, botWinStreak = 0, botWins = 0, unrankedCurrentWinStreak = 0, unrankedAllTimeWinStreak = 0, pvpCurrentWinStreak = 0, pvpAllTimeWinStreak = 0, unrankedWins = 0, unrankedLosses = 0, pvpWins = 0, pvpLosses = 0";

        // add to database
        try(PreparedStatement statement = BPBungee.connection.prepareStatement("UPDATE players SET bannedAt = ?, bannedDays = ?, bannedReason = ?, "+resetStatsSql+" WHERE uuid=?;")) {
            statement.setDate(1, new Date(System.currentTimeMillis()));
            statement.setInt(2, days);
            statement.setString(3, finalReason);
            statement.setString(4, playerUuid); // uuid, set to player uuid
            statement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            if(errorTo != null) {
                errorTo.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
            }
            return true;
        }

        if(playerIp != null) {
            try(PreparedStatement statement = BPBungee.connection.prepareStatement("INSERT INTO bannedIps (ip, uuid) VALUES (?, ?);")) {
                statement.setString(1, playerIp);
                statement.setString(2, playerUuid); // uuid, set to player uuid
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                if(errorTo != null) {
                    errorTo.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
                }
                return true;
            }
        }

        return false;
    }
}
