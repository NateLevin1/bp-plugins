package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static net.bridgepractice.BridgePracticeLobby.BridgePracticeLobby.connection;

public class CoinCommands implements CommandExecutor {
    String line = "§b§m------------------------------";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c/coins can only be used by players");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(line);
            player.sendMessage("§Coins Help");
            player.sendMessage(line);
            player.sendMessage("§c/coins give <amount> <player>");
            player.sendMessage("§c/coins remove <amount> <player>");
            player.sendMessage("§c/coins reset <player>");
            player.sendMessage(line);

        } else {
            if (args[0].equalsIgnoreCase("give")) {
                if (args.length == 1 || (args.length == 2 && (player.hasPermission("group.mod") || player.isOp()))) {
                    player.sendMessage("§cCorrect usage: /coins give <amount> <player>");
                } else if (args.length == 3) {
                    if (player.hasPermission("group.mod") || player.isOp()) {
                        Player c = Bukkit.getPlayerExact(args[2]);
                        if (c != null) {
                            try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET coins=? WHERE uuid=?;")) {
                                statement.setInt(1, Integer.parseInt(args[1])); // set coin amount
                                statement.setString(2, c.getUniqueId().toString()); // uuid, set to player uuid
                                statement.executeUpdate();
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                                if (player.isOnline()) {
                                    player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your current amount of coins!");
                                }
                            }
                            player.sendMessage("§aSuccessfully added " + args[1] + " §acoins to " + c.getDisplayName() + "§a.");
                        } else {
                            player.sendMessage("§cCannot find player " + args[2] + "!");
                        }
                    }
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length == 1 || (args.length == 2 && (player.hasPermission("group.mod") || player.isOp()))) {
                    player.sendMessage("§cCorrect usage: /coins remove <amount> <player>");
                    }
                } else if (args.length == 3) {
                    if (player.hasPermission("group.mod") || player.isOp()) {
                        Player c = Bukkit.getPlayerExact(args[2]);
                        if (c != null) {
                            try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET coins=? WHERE uuid=?;")) {
                                statement.setString(2, c.getUniqueId().toString()); // uuid, set to player uuid
                                statement.setInt(1, Integer.parseInt(args[1])); // set coin amount
                                statement.executeUpdate();
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                                if (player.isOnline()) {
                                    player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your current amount of coins!");
                                }
                            }
                            player.sendMessage("§aSuccessfully removed " + args[1] + " §acoins from " + c.getDisplayName() + "§a.");
                        } else {
                            player.sendMessage("§cCannot find player " + args[2] + "!");
                        }
                    }
                } else if (args[0].equalsIgnoreCase("reset")) {
            if (args.length == 1) {
                player.sendMessage("§cCorrect usage: /coins reset <player>");
            } else if (args.length == 2) {
                if (player.hasPermission("group.mod") || player.isOp()) {
                    Player c = Bukkit.getPlayerExact(args[1]);
                    if (c != null) {
                        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET coins=0 WHERE uuid=?;")) {
                            statement.setString(1, c.getUniqueId().toString()); // uuid, set to player uuid
                            statement.executeUpdate();
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                            if (player.isOnline()) {
                                player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your current amount of coins!");
                            }
                        }
                        player.sendMessage("§aSuccessfully reset " + c.getDisplayName() + "§a's coins.");
                    } else {
                        player.sendMessage("§cCannot find player " + args[1] + "!");
                    }
                }
            }
        }
    }
        return false;
}
}
