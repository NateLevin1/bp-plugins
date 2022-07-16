package net.bridgepractice.BridgePracticeLobby;

import com.mysql.jdbc.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EnderbuttGrant implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (p.hasPermission("group.admin")) {

                if (args.length == 0) {

                    ItemStack EnderButt = Utils.createEnderButt();
                    p.getInventory().addItem(EnderButt);

                    p.sendMessage(ChatColor.GREEN + "You have given yourself an EnderButt!");

                } else {
                    Player target = Bukkit.getPlayerExact(args[0]);

                    if (target == null) {
                        p.sendMessage(ChatColor.RED + "This player doesn't exist");

                        return true;
                    }

                    ItemStack EnderButt = Utils.createEnderButt();
                    target.getInventory().addItem(EnderButt);

                    target.sendMessage(ChatColor.GREEN + "You have been given an EnderButt!");

                }

            }else{
                p.sendMessage(ChatColor.RED + "You do not have permission!");
            }

        }

        return true;
    }

}
