package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class Stat extends Command {
    public Stat() {
        super("Stat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(sender.hasPermission("bridgepractice.moderation.players")) {
            BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, ()->{
                if(args.length <= 2) {
                    sender.sendMessage(new ComponentBuilder("Usage: /stat (remove|set) <player> statName [value]").color(ChatColor.RED).create());
                    return;
                }
                String setOrRemove = args[0];
                String playerName = args[1];
                String statName = args[2];
                String playerUuid;
                try {
                    playerUuid = Utils.getUuidFromNameSync(playerName);
                } catch (IOException e) {
                    sender.sendMessage(new ComponentBuilder("✕ '" + playerName + "' is not a valid username").color(ChatColor.RED).create());
                    return;
                }

                if(setOrRemove.equals("remove")) {
                    try(PreparedStatement statement = BPBungee.connection.prepareStatement("UPDATE players SET " + statName + " = DEFAULT WHERE uuid=?;")) {
                        statement.setString(1, playerUuid); // uuid, set to player uuid
                        statement.executeUpdate();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        sender.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
                        return;
                    }
                    sender.sendMessage(new ComponentBuilder("Successfully reset stat `" + statName + "` for player " + playerName + ".").color(ChatColor.GREEN).create());
                } else if(setOrRemove.equals("set")) {
                    if(args.length <= 3) {
                        sender.sendMessage(new ComponentBuilder("Usage: /stat remove <player> statName value").color(ChatColor.RED).create());
                        return;
                    }
                    String value = args[3];
                    try(PreparedStatement statement = BPBungee.connection.prepareStatement("UPDATE players SET " + statName + " = " + value + " WHERE uuid=?;")) {
                        statement.setString(1, playerUuid); // uuid, set to player uuid
                        statement.executeUpdate();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        sender.sendMessage(new ComponentBuilder("SQL error thrown: " + throwables.getMessage()).color(ChatColor.RED).create());
                        return;
                    }
                    sender.sendMessage(new ComponentBuilder("Successfully set stat `" + statName + "` for player " + playerName + ".").color(ChatColor.GREEN).create());
                } else {
                    sender.sendMessage(new ComponentBuilder("Unknown option '" + setOrRemove + "', expected either 'set' or 'remove'.").color(ChatColor.RED).create());
                }
            }, 0, TimeUnit.MILLISECONDS);
        } else {
            sender.sendMessage(new ComponentBuilder("You do not have permission to use this command").color(ChatColor.RED).create());
        }
    }
}
