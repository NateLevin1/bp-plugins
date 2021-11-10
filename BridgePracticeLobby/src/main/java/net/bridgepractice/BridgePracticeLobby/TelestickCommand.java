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

                ItemStack empty = new ItemStack(Material.AIR);
                player.getInventory().setBoots(empty);


            } else {
                player.setFoodLevel(20);

                ItemStack telestick = Utils.makeItem(Material.BLAZE_ROD, "§6Telestick §7(Right Click!)", "§7Click me to §eteleport","§7speed boost for §a3s§7.");
                BridgePracticeLobby.instance.setGadget(player, new BridgePracticeLobby.Gadget(current.fourthSlotItem, telestick));
                player.getInventory().setHeldItemSlot(5);

                ItemStack toeProtection = Utils.getUnbreakable(Utils.makeItem(Material.LEATHER_BOOTS, "§6Toe Protection", "§7Toe protection just in case of a hard fall"));
                LeatherArmorMeta toeProtMeta = (LeatherArmorMeta) toeProtection.getItemMeta();
                toeProtMeta.setColor(Color.ORANGE);
                toeProtection.setItemMeta(toeProtMeta);
                player.getInventory().setBoots(toeProtection);
            }
            BridgePracticeLobby.instance.giveGadgets(player, player.getInventory());
            return true;
        }
}
