package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class PlayerRider implements CommandExecutor{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = ((Player) sender);
        BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(player);
        if(current.fifthSlotItem.getType() == Material.LEASH) {
            // unrod
            BridgePracticeLobby.instance.setGadget(player, null);
            BridgePracticeLobby.instance.giveGadgets(player, player.getInventory());
        } else {
            giveGadget(player);
        }
        return true;
    }
    public static void giveGadget(Player player) {
        BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(player);
        ItemStack lead = Utils.makeItem(Material.LEASH, "§aRide-A-Player", "§7Right click to", "§ride a player!", "", "§eClick to select");
        BridgePracticeLobby.instance.setGadget(player, new BridgePracticeLobby.Gadget(current.fourthSlotItem, lead));
        player.getInventory().setHeldItemSlot(5);
        BridgePracticeLobby.instance.giveGadgets(player, player.getInventory());
    }
}