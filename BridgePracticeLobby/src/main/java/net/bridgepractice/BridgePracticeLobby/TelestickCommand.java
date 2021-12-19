package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;


public class TelestickCommand implements CommandExecutor{
        @Override
        public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
            if(!(sender instanceof Player)) return true;
            Player player = ((Player) sender);
            BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(player);
            if(current.fifthSlotItem.getType() == Material.BLAZE_ROD) {
                // unrod
                BridgePracticeLobby.instance.setGadget(player, null);
            } else {
                ItemStack telestick = Utils.makeItem(Material.BLAZE_ROD, "§6Telestick §7(Click!)", "§7Click to §eteleport§7 in the", "§7direction you're looking!");
                BridgePracticeLobby.instance.setGadget(player, new BridgePracticeLobby.Gadget(current.fourthSlotItem, telestick));
                player.getInventory().setHeldItemSlot(5);

                ItemStack wizardBoots = Utils.getUnbreakable(Utils.makeItem(Material.LEATHER_BOOTS, "§6Wizard Boots", "§eMagical Boots§7 that leave", "§7behind trails of fire!"));
                LeatherArmorMeta wizardBootMeta = (LeatherArmorMeta) wizardBoots.getItemMeta();
                wizardBootMeta.setColor(Color.fromRGB(255, 60, 0));
                wizardBoots.setItemMeta(wizardBootMeta);
                player.getInventory().setBoots(wizardBoots);
            }
            BridgePracticeLobby.instance.giveGadgets(player, player.getInventory());
            return true;
        }
}
