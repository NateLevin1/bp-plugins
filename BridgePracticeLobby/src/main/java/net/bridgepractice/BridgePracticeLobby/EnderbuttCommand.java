package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class EnderbuttCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = ((Player) sender);
        BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(player);
        if (current.fifthSlotItem.getType() == Material.ENDER_PEARL) {
            // unrod
            BridgePracticeLobby.instance.setGadget(player, null);
            BridgePracticeLobby.instance.giveGadgets(player, player.getInventory());
        } else {
            giveGadget(player);
        }
        return true;
    }

    public static void giveGadget(Player p){
        BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(p);
        ItemStack enderbutt = Utils.makeItem(Material.ENDER_PEARL, "§5EnderButt", "§7Shoot your shot and", "§7teleport anywhere!");
        BridgePracticeLobby.instance.setGadget(p, new BridgePracticeLobby.Gadget(current.fourthSlotItem, enderbutt));
        p.getInventory().setHeldItemSlot(5);
        BridgePracticeLobby.instance.giveGadgets(p, p.getInventory());
    }
}
