package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;

public class ChangeTag extends Command {
    public ChangeTag() {
        super("ChangeTag", "group.custom");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = ((ProxiedPlayer) sender);
        BPBungee.instance.getProxy().getScheduler().runAsync(BPBungee.instance, ()->{
            try(PreparedStatement statement = BPBungee.connection.prepareStatement("SELECT editedAt FROM rankedPlayers WHERE uuid = ?;")) {
                statement.setString(1, player.getUniqueId().toString()); // uuid, set to player uuid
                ResultSet res = statement.executeQuery();
                if(!res.next()) {
                    player.sendMessage(new ComponentBuilder("Something went wrong.").color(ChatColor.RED).create());
                    return;
                }
                Date editedAt = res.getDate("editedAt");
                if(!res.wasNull() && ChronoUnit.DAYS.between(editedAt.toInstant(), new Date(System.currentTimeMillis()).toInstant()) < 7) {
                    player.sendMessage(new ComponentBuilder("You must wait 7 days between changing your tag!").color(ChatColor.RED).create());
                    return;
                }
                if(args.length == 0) {
                    player.sendMessage(new ComponentBuilder("Usage: /changetag <tag>").color(ChatColor.RED).create());
                    return;
                }
                String newTag = args[0].toUpperCase();
                if(!newTag.matches("^\\w{2,8}$")) {
                    player.sendMessage(new ComponentBuilder("Your tag must be between 2 and 8 characters.").color(ChatColor.RED).create());
                    return;
                }

                if(args.length == 1) {
                    String name = player.getName();
                    player.sendMessage(new ComponentBuilder("----------------------------------------").color(ChatColor.AQUA).strikethrough(true).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.GOLD).append(": /changetag "+args[0]+" gold").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.YELLOW).append(": /changetag "+args[0]+" yellow").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.AQUA).append(": /changetag "+args[0]+" aqua").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.DARK_AQUA).append(": /changetag "+args[0]+" darkaqua").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.LIGHT_PURPLE).append(": /changetag "+args[0]+" pink").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.DARK_PURPLE).append(": /changetag "+args[0]+" darkpurple").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.WHITE).append(": /changetag "+args[0]+" white").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("["+newTag+"] "+name).color(ChatColor.GREEN).append(": /changetag "+args[0]+" green").color(ChatColor.WHITE).create());
                    player.sendMessage(new ComponentBuilder("----------------------------------------").color(ChatColor.AQUA).strikethrough(true).create());
                    player.sendMessage(new ComponentBuilder("^^ You need to provide a color to change your tag with! Choose one from the list above").color(ChatColor.GREEN).bold(true).create());
                } else {
                    String colorChar = colorNameToNumber(args[2]);
                    if(colorChar == null) {
                        player.sendMessage(new ComponentBuilder("Unknown color '"+args[2]+"'").color(ChatColor.RED).create());
                        return;
                    }
                    try(PreparedStatement updateTag = BPBungee.connection.prepareStatement("UPDATE rankedPlayers SET tag = ?, color = ?, editedAt = CURDATE(), approved = 0 WHERE uuid = ?;")) {
                        updateTag.setString(1, newTag);
                        updateTag.setString(2, colorChar);
                        updateTag.setString(3, player.getUniqueId().toString());
                        updateTag.executeUpdate();
                        player.sendMessage(new ComponentBuilder("\n\nWoohoo!").color(ChatColor.GREEN).bold(true).append(" Your new custom tag, ["+newTag+"], is awaiting approval from our moderators!").create());

                        // TODO: IPC to discord bot to send a message for accepting
                    }
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage(new ComponentBuilder("Something went wrong.").color(ChatColor.RED).create());
            }
        });
    }
    private String colorNameToNumber(String name) {
        switch (name.toLowerCase()) {
            case "yellow":
                return "e";
            case "aqua":
                return "b";
            case "pink":
                return "d";
            case "white":
                return "f";
            case "gold":
                return "6";
            case "darkaqua":
                return "3";
            case "darkpurple":
                return "5";
            case "green":
                return "a";
        }
        return null;
    }
}
