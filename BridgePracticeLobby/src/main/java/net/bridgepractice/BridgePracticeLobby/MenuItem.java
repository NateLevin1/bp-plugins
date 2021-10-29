package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MenuItem {
    MenuItemClick onClick;
    ItemStack item;
    int index;
    public Menu parentMenu;
    MenuItem(int row, int column, ItemStack item, MenuItemClick onClick) {
        this.index = row * 9 + column;
        this.item = item;
        this.onClick = onClick;
    }
    MenuItem(int index, ItemStack item, MenuItemClick onClick) {
        this.index = index;
        this.item = item;
        this.onClick = onClick;
    }
    public static MenuItem close(int row, int column) {
        return new MenuItem(row, column, Utils.makeItem(Material.BARRIER, "§cClose"), (player, menu)->{
            menu.allowForGarbageCollection();
            player.getOpenInventory().close();
        });
    }
    public static MenuItem back(int row, int column, Menu goBackTo) {
        return new MenuItem(row, column, Utils.makeItem(Material.ARROW, "§aGo Back", "§7To "+goBackTo.getInventory().getTitle()), (player, menu)-> {
            menu.allowForGarbageCollection();
            player.openInventory(goBackTo.getInventory());
        });
    }
    public static MenuItem blocker(int row, int column) {
        return new MenuItem(row, column, Utils.makeDyed(Material.STAINED_GLASS_PANE, DyeColor.GRAY, " "), (player, menu) -> {});
    }
}

interface MenuItemClick {
    void run(Player player, Menu menu);
}