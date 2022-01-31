package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import static net.bridgepractice.BridgePracticeLobby.BridgePracticeLobby.connection;

public class StatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = ((Player) sender);
        if (args.length == 0 || args[0] == null) {
            showStats(player, player);
        } else if (player.hasPermission("group.godlike")) {
            showStats(player, args[0], null);
        } else {
            player.sendMessage(ChatColor.RED + "Getting stats of other players is only available to " + ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "GODLIKE" + ChatColor.DARK_PURPLE + "]" + ChatColor.RED + " and above!");
        }
        return true;
    }

    public static void showStats(Player sender, Player player) {
        showStats(sender, player.getName(), player.getUniqueId().toString());
    }

    public static void showStats(Player sender, String playerName, String playerUuid) {
        MenuItem wing = new MenuItem(2, 1, Utils.makeItem(Material.CLAY_BRICK, "§eWing Practice", "§8Singleplayer", ""), null);
        MenuItem bypass = new MenuItem(2, 2, Utils.makeItem(Material.SUGAR, "§eBypass Practice", "§8Singleplayer", ""), null);
        MenuItem prebow = new MenuItem(3, 1, Utils.makeItem(Material.ARROW, "§ePrebow Practice", "§8Singleplayer", ""), null);
        MenuItem bot = new MenuItem(3, 2, Utils.makeItem(Material.STONE_SWORD, "§eBot 1v1", "§8Singleplayer", ""), null);
        MenuItem clutch = new MenuItem(4, 1, Utils.makeDyed(Material.RAW_FISH, DyeColor.MAGENTA, "§eClutch Practice", "§8Singleplayer", ""), null);
        MenuItem unranked = new MenuItem(2, 6, Utils.makeItem(Material.IRON_SWORD, "§eBridge Duel", "§8Multiplayer", ""), null);
        MenuItem pvp = new MenuItem(2, 7, Utils.makeItem(Material.IRON_BOOTS, "§eBridge PvP 1v1", "§8Multiplayer", ""), null);

        MenuItem star = new MenuItem(3, 4, Utils.makeItem(Material.NETHER_STAR, "§eOther Stats", ""), null);

        Menu stats = new Menu(playerName + "'s Stats", 6, true,
                new MenuItem(1, 4, Utils.makeItem(Material.BEACON, "§5"+playerName+"'s Statistics", "§7View "+playerName+"'s statistics", "§7across the network"), null),

                wing,
                bypass,
                prebow,
                bot,
                clutch,

                unranked,
                pvp,

                star
        );
        sender.openInventory(stats.getInventory());

        (new BukkitRunnable() {
            @Override
            public void run() {
                String realUuid = null;
                if(playerUuid == null) {
                    try {
                        realUuid = Utils.getUuidFromNameSync(playerName);
                    } catch (IOException e) {
                        (new BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.closeInventory();
                                sender.sendMessage("§cUnknown player '"+playerName+"'");
                            }
                        }).runTask(BridgePracticeLobby.instance);
                    }
                } else {
                    realUuid = playerUuid;
                }
                try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid=?;")) {
                    statement.setString(1, realUuid); // uuid
                    ResultSet res = statement.executeQuery();
                    if(!res.next()) {
                        throw new SQLException("Did not get a row from the database. Player name: " + playerName + " Player UUID: " + realUuid);
                    }
                    Inventory inv = stats.getInventory();

                    DecimalFormat decimalFormatter = new DecimalFormat("#.###");
                    decimalFormatter.setRoundingMode(RoundingMode.CEILING);

                    double wingPB = res.getFloat("wingPB") * 0.001;
                    inv.setItem(wing.index, Utils.addLore(wing.item, "§7 - §fPersonal Best: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(wingPB)), ""));

                    double startGame = res.getFloat("bypassStartPB") * 0.001;
                    Utils.addLore(bypass.item, "§7 - §fStart Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(startGame)));
                    double earlyGame = res.getFloat("bypassEarlyPB") * 0.001;
                    Utils.addLore(bypass.item, "§7 - §fEarly Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(earlyGame)));
                    double middleGame = res.getFloat("bypassMiddlePB") * 0.001;
                    Utils.addLore(bypass.item, "§7 - §fMiddle Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(middleGame)));
                    double lateGame = res.getFloat("bypassLatePB") * 0.001;
                    inv.setItem(bypass.index, Utils.addLore(bypass.item, "§7 - §fLate Game: " + (res.wasNull() ? "§cN/A" : "§a" + prettifyTime(lateGame)), ""));

                    int prebowHits = res.getInt("prebowHits");
                    inv.setItem(prebow.index, Utils.addLore(prebow.item, "§7 - §fAll Time Hits: §a" + prebowHits, ""));

                    int botWinStreak = res.getInt("botWinStreak");
                    Utils.addLore(bot.item, "§7 - §fWin Streak: §a" + botWinStreak);
                    int botWins = res.getInt("botWins");
                    inv.setItem(bot.index, Utils.addLore(bot.item, "§7 - §fTotal Wins: §a" + botWins));

                    int clutchesTotal = res.getInt("clutchesTotal");
                    inv.setItem(clutch.index, Utils.addLore(clutch.item, "§7 - §fTotal Clutches: §a"+clutchesTotal, ""));

                    int xp = res.getInt("xp");
                    Utils.addLore(star.item, "§7 - §fXP: §a" + xp);
                    Date firstLogin = res.getDate("firstLogin");
                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                    inv.setItem(star.index, Utils.addLore(star.item, "§7 - §fFirst Login: §a" + formatter.format(firstLogin), ""));

                    {
                        int curWs = res.getInt("unrankedCurrentWinStreak");
                        int allTimeWs = res.getInt("unrankedAllTimeWinStreak");
                        int wins = res.getInt("unrankedWins");
                        int losses = res.getInt("unrankedLosses");
                        inv.setItem(unranked.index, Utils.addLore(unranked.item, "§7 - §fCurrent Winstreak: §a" + curWs,
                                "§7 - §fBest Winstreak: §a" + allTimeWs,
                                "§7 - §fTotal Wins: §a" + wins,
                                "§7 - §fTotal Losses: §a" + losses,
                                "§7 - §fWin/Loss Ratio: §a" + (losses != 0 ? decimalFormatter.format(wins / ((double) losses)) : (wins == 0 ? "§cN/A" : "Infinity!"))));
                    }

                    {
                        int curWs = res.getInt("pvpCurrentWinStreak");
                        int allTimeWs = res.getInt("pvpAllTimeWinStreak");
                        int wins = res.getInt("pvpWins");
                        int losses = res.getInt("pvpLosses");
                        inv.setItem(pvp.index, Utils.addLore(pvp.item, "§7 - §fCurrent Winstreak: §a" + curWs,
                                "§7 - §fBest Winstreak: §a" + allTimeWs,
                                "§7 - §fTotal Wins: §a" + wins,
                                "§7 - §fTotal Losses: §a" + losses,
                                "§7 - §fWin/Loss Ratio: §a" + (losses != 0 ? decimalFormatter.format(wins / ((double) losses)) : (wins == 0 ? "§cN/A" : "Infinity!")),
                                ""));
                    }
                } catch (SQLException throwables) {
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.closeInventory();
                            sender.sendMessage("§cThe player '"+playerName+"' has never logged on!");
                        }
                    }).runTask(BridgePracticeLobby.instance);
                }
            }
        }).runTaskAsynchronously(BridgePracticeLobby.instance);
    }

    public static String prettifyTime(double time) {
        return String.format("%.3f", time);
    }
}
