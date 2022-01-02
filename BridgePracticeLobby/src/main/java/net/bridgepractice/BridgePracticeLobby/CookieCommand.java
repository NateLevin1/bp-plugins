package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CookieCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = ((Player) sender);
        BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(player);
        if(current.fifthSlotItem.getType() == Material.COOKIE) {
            // uncookie
            BridgePracticeLobby.instance.setGadget(player, null);
            BridgePracticeLobby.instance.giveGadgets(player, player.getInventory());
        } else {
            giveGadget(player);
        }
        return true;
    }
    public static void giveGadget(Player player) {
        BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(player);
        ItemStack cookieItem = Utils.makeItem(Material.COOKIE, "§aMagical Cookie §7(Eat me!)", "§7Eat to gain speed and","§7jump boost for §a5s§7.");
        BridgePracticeLobby.instance.setGadget(player, new BridgePracticeLobby.Gadget(current.fourthSlotItem, cookieItem));
        player.setFoodLevel(19);
        player.getInventory().setHeldItemSlot(5);
        BridgePracticeLobby.instance.giveGadgets(player, player.getInventory());
    }
}
