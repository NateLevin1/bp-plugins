package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class Unban extends Command {
    public Unban() {
        super("Unban", "bridgepractice.moderation.players", "pardon");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("Usage: /unban <player>").color(ChatColor.RED).create());
        }
        String playerName = args[0];
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
            String playerUuid;
            try {
                playerUuid = Utils.getUuidFromNameSync(playerName);
            } catch (IOException e) {
                sender.sendMessage(new ComponentBuilder("✕ '" + playerName + "' is not a valid username").color(ChatColor.RED).create());
                return;
            }

            if(applyUnban(playerUuid, sender)) {
                return;
            }

            sender.sendMessage(new ComponentBuilder("Successfully unbanned player " + playerName + ".").color(ChatColor.GREEN).create());
        }, 0, TimeUnit.MILLISECONDS);
    }

    public static boolean applyUnban(String playerUuid, CommandSender sender) {
        // add to database
        try(PreparedStatement updateBannedAt = BPBungee.connection.prepareStatement("UPDATE players SET bannedAt = NULL, bannedDays = NULL, bannedReason = NULL WHERE uuid=?;")) {
            updateBannedAt.setString(1, playerUuid); // uuid, set to player uuid
            updateBannedAt.executeUpdate();
            try(PreparedStatement removeFromBannedIps = BPBungee.connection.prepareStatement("DELETE FROM bannedIps WHERE uuid = ?;")) {
                removeFromBannedIps.setString(1, playerUuid); // uuid, set to player uuid
                removeFromBannedIps.executeUpdate();
            }

            // send a webhook
            String bannerName;
            if(sender != null) {
                bannerName = sender.getName();
            } else {
                bannerName = "[Automatic]";
            }

            Utils.sendPunishmentWebhook(false, "unbanned", "[Unban of above uuid]", -1, bannerName, "c06f8906-4c8a-4911-9c29-ea1dbd1aab82", playerUuid, sender);
        } catch (Exception throwables) {
            throwables.printStackTrace();
            if(sender != null) {
                sender.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
            }
            return true;
        }
        return false;
    }
}
