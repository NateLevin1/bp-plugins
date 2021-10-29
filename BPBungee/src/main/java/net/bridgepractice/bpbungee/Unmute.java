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

public class Unmute extends Command {
    public Unmute() {
        super("Unmute");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(sender.hasPermission("bridgepractice.moderation.chat")) {
            if(args.length == 0) {
                sender.sendMessage(new ComponentBuilder("You need to provide more arguments for this command!").color(ChatColor.RED).create());
                return;
            }

            BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
                String playerName = args[0];
                String playerUuid;
                try {
                    playerUuid = Utils.getUuidFromNameSync(playerName);
                } catch (IOException e) {
                    sender.sendMessage(new ComponentBuilder("✕ '" + playerName + "' is not a valid username").color(ChatColor.RED).create());
                    return;
                }

                // add to database
                try(PreparedStatement updateMutedAt = BPBungee.connection.prepareStatement("UPDATE players SET mutedAt = NULL, mutedDays = NULL WHERE uuid=?;")) {
                    updateMutedAt.setString(1, playerUuid); // uuid, set to player uuid
                    updateMutedAt.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    sender.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
                    return;
                }

                sender.sendMessage(new ComponentBuilder("Successfully unmuted player " + playerName + ".").color(ChatColor.GREEN).create());

                // if online, make them unmuted
                ProxiedPlayer onlinePlayer = BPBungee.instance.getProxy().getPlayer(playerName);
                if(onlinePlayer != null) {
                    BPBungee.instance.mutedPlayers.remove(onlinePlayer.getUniqueId());
                }
            }, 0, TimeUnit.MILLISECONDS);
        } else {
            sender.sendMessage(new ComponentBuilder("You do not have permission to use this command.").color(ChatColor.RED).create());
        }
    }
}
