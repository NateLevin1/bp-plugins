package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class Unfreeze extends Command {
    public Unfreeze() {
        super("Unfreeze");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(sender.hasPermission("bridgepractice.moderation.players")) {
            if(args.length == 0) {
                sender.sendMessage(new ComponentBuilder("You need to provide a player for this command!").color(ChatColor.RED).create());
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

                try(PreparedStatement statement = BPBungee.connection.prepareStatement("UPDATE players SET frozen = FALSE WHERE uuid=?;")) {
                    statement.setString(1, playerUuid); // uuid, set to player uuid
                    statement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    sender.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
                    return;
                }

                sender.sendMessage(new ComponentBuilder("Successfully unfroze player " + playerName + ".").color(ChatColor.GREEN).create());
            }, 0, TimeUnit.MILLISECONDS);
        } else {
            sender.sendMessage(new ComponentBuilder("You do not have permission to use this command").color(ChatColor.RED).create());
        }
    }
}
