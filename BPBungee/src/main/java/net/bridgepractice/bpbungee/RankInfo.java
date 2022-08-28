package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class RankInfo extends Command {
    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
    public RankInfo() {
        super("RankInfo", null, "rank");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = ((ProxiedPlayer) sender);
        BPBungee.instance.getProxy().getScheduler().runAsync(BPBungee.instance, ()->{
            try(PreparedStatement statement = BPBungee.connection.prepareStatement("SELECT permission, editedAt, months, approved, boughtAt FROM rankedPlayers WHERE uuid = ?;")) {
                statement.setString(1, player.getUniqueId().toString()); // uuid, set to player uuid
                ResultSet res = statement.executeQuery();
                if(!res.next()) {
                    player.sendMessage(new ComponentBuilder("You do not have a rank! ")
                            .color(ChatColor.RED)
                            .append("Click here to buy one!")
                            .color(ChatColor.AQUA).underlined(true)
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to visit our store!")))
                            .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://store.bridgepractice.net"))
                            .create());
                    return;
                }
                String type = res.getString("permission");
                Date boughtAt = res.getDate("boughtAt");

                boolean isCustom = type.equals("custom");
                Date editedAt = res.getDate("editedAt");
                int months = res.getInt("months");
                int approved = res.getInt("approved");

                if(isCustom) {
                    player.sendMessage(new ComponentBuilder("You currently have a ").color(ChatColor.GREEN).append(type).color(ChatColor.GOLD).append(" rank.").color(ChatColor.GREEN)
                            .append("\nYou bought it on ").color(ChatColor.GRAY)
                            .append(dateFormat.format(boughtAt)).color(ChatColor.GREEN)
                            .append(" for ").color(ChatColor.GRAY)
                            .append(months + " months").color(ChatColor.GREEN)
                            .append("\nYou last edited your tag on ").color(ChatColor.GRAY)
                            .append(editedAt != null ? dateFormat.format(editedAt) : "never.").color(ChatColor.GREEN)
                            .append("\nYour tag ").color(ChatColor.GRAY)
                            .append((approved == 1
                                    ? new ComponentBuilder("is currently approved!").color(ChatColor.GREEN)
                                    : (
                                            approved == 0
                                                    ? new ComponentBuilder("is currently awaiting approval. Check back soon!").color(ChatColor.YELLOW)
                                                    : new ComponentBuilder("has been denied. Use /changetag to try again.").color(ChatColor.RED).bold(true)
                                    )).create())
                            .create());
                } else {
                    player.sendMessage(new ComponentBuilder("You currently have a ").color(ChatColor.GREEN).append(type).color(ChatColor.GOLD).append(" rank.").color(ChatColor.GREEN)
                            .append("\nYou bought it on ").color(ChatColor.GRAY)
                            .append(dateFormat.format(boughtAt)).color(ChatColor.GREEN)
                            .create());
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage(new ComponentBuilder("Something went wrong.").color(ChatColor.RED).create());
            }
        });
    }
}