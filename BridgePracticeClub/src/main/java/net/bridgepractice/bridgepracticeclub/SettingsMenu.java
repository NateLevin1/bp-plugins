package net.bridgepractice.bridgepracticeclub;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;


public class SettingsMenu {
    HashMap<Integer, Entry> items;
    String title;
    HashMap<String, ArrayList<Entry>> groups = new HashMap<>();
    public interface OnClick {
        void call(ItemStack item, String groupName);
    }
    public static class Entry {
        int index;
        ItemStack item;
        ItemStack enchantedItem;
        String group;
        boolean selected;

        Entry(int index, ItemStack item, String group, boolean selected) {
            this.index = index;
            this.item = item;
            this.enchantedItem = item.clone();
            ItemMeta im = this.enchantedItem.getItemMeta();
            im.setDisplayName("§r§e"+im.getDisplayName().substring(2));
            this.enchantedItem.setItemMeta(im);
            this.enchantedItem = Bridge.getEnchanted(this.enchantedItem);
            this.group = group;
            this.selected = selected;
        }
        Entry(int row, int column, ItemStack item, String group, boolean selected) {
            this(row*9+column, item, group, selected);
        }
    }
    OnClick onClick;
    Inventory inventory;
    SettingsMenu(Entry[] items, int inventoryLines, String title, OnClick onItemClick) {
        this.items = new HashMap<>();
        this.title = title;
        this.onClick = onItemClick;

        this.inventory = Bridge.instance.getServer().createInventory(null, inventoryLines*9, this.title);
        for(Entry item : items) {
            this.groups.putIfAbsent(item.group, new ArrayList<>());
            this.groups.get(item.group).add(item);

            if(item.selected) {
                this.inventory.setItem(item.index, item.enchantedItem);
            } else {
                this.inventory.setItem(item.index, item.item);
            }

            this.items.put(item.index, item);

        }
    }
}
