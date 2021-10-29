package net.bridgepractice.bpreport;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CommandReport implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("§cYou must be a player to use this command");
            return true;
        }
        Player player = ((Player) sender);
        if(args.length == 0) {
            player.sendMessage("§cYou need to provide a player to report!");
            return true;
        }
        String reportedName = args[0];

        if(args.length == 2) {
            // check if it was from a click
            String reason = args[1];
            switch(reason) {
                case "cheating":
                    BPReport.report(player, reportedName, BPReport.ReportReason.Cheating);
                    player.closeInventory();
                    return true;
                case "chat":
                    BPReport.report(player, reportedName, BPReport.ReportReason.Chat);
                    player.closeInventory();
                    return true;
                case "alt":
                    BPReport.report(player, reportedName, BPReport.ReportReason.Alt);
                    player.closeInventory();
                    return true;
                default:
                    break;
            }
        }
        int inventoryLines = 5;
        Inventory inventory = BPReport.instance.getServer().createInventory(null, inventoryLines*9, "Reporting "+reportedName);
        inventory.setItem(19, Utils.makeItem(Material.GOLD_SWORD, 1, "Cheating", new String[]{reportedName+" is using hacks or","disallowed modifications."}, -1));
        inventory.setItem(22, Utils.makeItem(Material.WEB, 1, "Chat Abuse/Scam", new String[]{reportedName+" is being overly toxic","or scamming people in chat"}, -1));
        inventory.setItem(25, Utils.makeItem(Material.SKULL_ITEM, 1, "Alting", new String[]{reportedName+" is alting to","avoid a ban"}, 3));
        player.openInventory(inventory);
        return true;
    }
}