package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static net.bridgepractice.bpbungee.BPBungee.connection;

public class Coins extends Command {
    public Coins() {
        super("coins", "group.admin");
    }

    protected final BaseComponent[] line = new ComponentBuilder("------------------------------").color(ChatColor.AQUA).strikethrough(true).create();

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new ComponentBuilder()
                    .append(line)
                    .append(new ComponentBuilder("\n").create())
                    .append(new ComponentBuilder("Coins Help\n").strikethrough(false).color(ChatColor.RED).create())
                    .append(line)
                    .append(new ComponentBuilder("\n").create())
                    .append(new ComponentBuilder("/coins give <amount> <player>\n").strikethrough(false).color(ChatColor.RED).create())
                    .append(new ComponentBuilder("/coins remove <amount> <player>\n").color(ChatColor.RED).create())
                    .append(new ComponentBuilder("/coins reset <player>\n").color(ChatColor.RED).create())
                    .append(line)
                    .create());
            return;
        }

        String targetUUID;

        if (args[0].equals("give")) {
            if (args.length <= 2) {
                sender.sendMessage(new ComponentBuilder("Correct usage: /coins give <amount> <player>").color(ChatColor.RED).create());
                return;
            }

            targetUUID = getUUID(sender, args[2]);
            if (targetUUID == null) return;

            giveCoins(sender, targetUUID, Integer.parseInt(args[1]));
            sender.sendMessage(new ComponentBuilder("Successfully added ").color(ChatColor.GREEN)
                    .append(args[1])
                    .append(new ComponentBuilder(" coins to ").color(ChatColor.GREEN).create())
                    .append(new ComponentBuilder(BPBungee.instance.getProxy().getPlayer(args[2]) == null ? args[2] : BPBungee.instance.getProxy().getPlayer(args[2]).getDisplayName()).create())
                    .append(new ComponentBuilder(".").color(ChatColor.GREEN).create())
                    .create());
            return;
        }

        if (args[0].equals("remove")) {
            if (args.length <= 2) {
                sender.sendMessage(new ComponentBuilder("Correct usage: /coins remove <amount> <player>").color(ChatColor.RED).create());
                return;
            }
            targetUUID = getUUID(sender, args[2]);
            if (targetUUID == null) return;

            removeCoins(sender, targetUUID, Integer.parseInt(args[1]));
            sender.sendMessage(new ComponentBuilder("Successfully removed ").color(ChatColor.GREEN)
                    .append(new ComponentBuilder(args[1]).color(ChatColor.GREEN).create())
                    .append(new ComponentBuilder(" coins to ").color(ChatColor.GREEN).create())
                    .append(new ComponentBuilder(BPBungee.instance.getProxy().getPlayer(args[2]) == null ? args[2] : BPBungee.instance.getProxy().getPlayer(args[2]).getDisplayName()).create())
                    .append(new ComponentBuilder(".").color(ChatColor.GREEN).create())
                    .create());
            return;
        }

        if (args[0].equals("reset")) {
            if (args.length == 1) {
                sender.sendMessage(new ComponentBuilder("Correct usage: /coins reset <player>").color(ChatColor.RED).create());
                return;
            }

            targetUUID = getUUID(sender, args[1]);
            if (targetUUID == null) return;

            resetCoins(sender, targetUUID);
            sender.sendMessage(new ComponentBuilder("Successfully reset ").color(ChatColor.GREEN)
                    .append(new ComponentBuilder(BPBungee.instance.getProxy().getPlayer(args[1]) == null ? args[1] : BPBungee.instance.getProxy().getPlayer(args[1]).getDisplayName()).create())
                    .append(new ComponentBuilder(args[1].endsWith("s") ? "' coins." : "'s coins.").color(ChatColor.GREEN).create())
                    .create());
            return;
        }

        sender.sendMessage(new ComponentBuilder("'").color(ChatColor.RED)
                .append(new ComponentBuilder(args[0]).color(ChatColor.RED).create())
                .append(new ComponentBuilder("' is not recognized!").color(ChatColor.RED).create())
                .create());
    }

    protected void giveCoins(CommandSender sender, String targetUUID, int amount) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET coins=? WHERE uuid=?;")) {
            statement.setInt(1, amount); // set coin amount
            statement.setString(2, targetUUID); // uuid, set to player uuid
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
            sender.sendMessage(new ComponentBuilder("SQL error thrown: " + exception.getMessage()).color(ChatColor.RED).create());
        }
    }

    protected void removeCoins(CommandSender sender, String targetUUID, int amount) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET coins=? WHERE uuid=?;")) {
            statement.setInt(1, amount); // set coin amount
            statement.setString(2, targetUUID); // uuid, set to player uuid
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
            sender.sendMessage(new ComponentBuilder("SQL error thrown: " + exception.getMessage()).color(ChatColor.RED).create());
        }
    }

    protected void resetCoins(CommandSender sender, String targetUUID) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET coins=0 WHERE uuid=?;")) {
            statement.setString(1, targetUUID); // uuid, set to player uuid
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
            sender.sendMessage(new ComponentBuilder("SQL error thrown: " + exception.getMessage()).color(ChatColor.RED).create());
        }
    }
    protected String getUUID(CommandSender sender, String target) {
        try {
            return Utils.getUuidFromNameSync(target);
        } catch (IOException exception) {
            sender.sendMessage(new ComponentBuilder("x '").color(ChatColor.RED)
                    .append(new ComponentBuilder(target).color(ChatColor.RED).create())
                    .append(new ComponentBuilder("' is not a valid username!").color(ChatColor.RED).create())
                    .create());
            return null;
        }
    }
}
