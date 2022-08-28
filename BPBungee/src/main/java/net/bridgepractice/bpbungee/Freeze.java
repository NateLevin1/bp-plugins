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

public class Freeze extends Command {
    public Freeze() {
        super("Freeze");
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

                // if online, kick with message
                ProxiedPlayer onlinePlayer = BPBungee.instance.getProxy().getPlayer(playerName);
                if(onlinePlayer != null) {
                    onlinePlayer.disconnect(new ComponentBuilder(BPBungee.frozenMessage).create());
                }

                // add to database
                try(PreparedStatement statement = BPBungee.connection.prepareStatement("UPDATE players SET frozen = TRUE WHERE uuid=?;")) {
                    statement.setString(1, playerUuid); // uuid, set to player uuid
                    statement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    sender.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
                    return;
                }

                sender.sendMessage(new ComponentBuilder("Successfully froze player " + playerName + ".").color(ChatColor.GREEN).create());

                String senderName;
                if(sender instanceof ProxiedPlayer) {
                    senderName = ((ProxiedPlayer) sender).getDisplayName();
                } else {
                    senderName = sender.getName();
                }

                BPBungee.instance.getProxy().broadcast(new ComponentBuilder("\n§7[§c>§7]§c Player '§a" + playerName + "§c' was §ffrozen§c by " + senderName + "§c!").create());
                BPBungee.instance.getProxy().broadcast(new ComponentBuilder("\n").create());
            }, 0, TimeUnit.MILLISECONDS);
        } else {
            sender.sendMessage(new ComponentBuilder("You do not have permission to use this command").color(ChatColor.RED).create());
        }
    }
}
