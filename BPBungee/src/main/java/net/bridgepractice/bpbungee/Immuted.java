package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Immuted extends Command {
    HashSet<UUID> lastExecutes = new HashSet<>();
    public Immuted() {
        super("Immuted");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        BPBungee.instance.getProxy().getScheduler().runAsync(BPBungee.instance, ()-> {
            ProxiedPlayer player = ((ProxiedPlayer) sender);
            try(PreparedStatement statement = BPBungee.connection.prepareStatement("SELECT mutedAt FROM players WHERE uuid=?;")) {
                statement.setString(1, player.getUniqueId().toString()); // uuid
                ResultSet res = statement.executeQuery();
                if(!res.next()) {
                    throw new SQLException("Did not get a row from the database. Player name: " + sender.getName() + " Player UUID: " + player.getUniqueId());
                }
                res.getDate(1);
                if(res.wasNull()) {
                    // if mutedAt was null, they are not muted
                    player.sendMessage(new ComponentBuilder("You are not muted! This command is intended to be used to notify players when you are muted!").color(ChatColor.RED).create());
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if(args.length == 0) {
                sender.sendMessage(new ComponentBuilder("You need to provide a player for this command!").color(ChatColor.RED).create());
                return;
            }

            String senderName = player.getDisplayName();
            String playerName = args[0];
            if(playerName.equalsIgnoreCase(sender.getName())) {
                sender.sendMessage(new ComponentBuilder("You cannot message yourself!").color(ChatColor.RED).create());
                return;
            }
            ProxiedPlayer playerToSendTo = BPBungee.instance.getProxy().getPlayer(playerName);
            if(playerToSendTo != null) {
                if(!lastExecutes.contains(player.getUniqueId())) {
                    lastExecutes.add(player.getUniqueId());
                    sender.sendMessage(new ComponentBuilder("§dTo " + playerToSendTo.getDisplayName() + "§7: ").append("I cannot message you because I am muted!").color(ChatColor.RED).create());
                    playerToSendTo.sendMessage(new ComponentBuilder("§dFrom " + senderName + "§7: ").append("I cannot message you because I am muted!").color(ChatColor.RED).create());
                    BPBungee.instance.playerReplyTo.put(playerToSendTo.getUniqueId(), new BPBungee.NamedPlayer(sender.getName(), senderName));

                    BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, ()->{
                        lastExecutes.remove(player.getUniqueId());
                    }, 30, TimeUnit.SECONDS);
                } else {
                    player.sendMessage(new ComponentBuilder("You must wait 30 seconds between usages of this command!").color(ChatColor.RED).create());
                }
            } else {
                sender.sendMessage(new ComponentBuilder("Unknown player \"" + playerName + "\"").color(ChatColor.RED).create());
            }
        });
    }
}
