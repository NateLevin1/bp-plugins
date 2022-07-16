package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;

public class Menu {
    private final Inventory inventory;
    private final HashMap<Integer, MenuItemClick> onClicks = new HashMap<>();
    public final ArrayList<Material> draggables = new ArrayList<>();
    private final boolean singleUse;
    private final String name;
    Menu(String name, int rows, boolean singleUse, MenuItem... items) {
        this.singleUse = singleUse;
        this.name = name;
        inventory = BridgePracticeLobby.instance.getServer().createInventory(null, rows * 9, name);
        for(MenuItem item : items) {
            inventory.setItem(item.index, item.item);
            onClicks.put(item.index, item.onClick);
            item.parentMenu = this;
        }
        menus.put(name, this);
    }
    public void addItem(MenuItem item) {
        inventory.setItem(item.index, item.item);
        onClicks.put(item.index, item.onClick);
        item.parentMenu = this;
    }
    public void addDraggableItem(MenuItem item) {
        inventory.setItem(item.index, item.item);
        draggables.add(item.item.getType());
        item.parentMenu = this;
    }
    public void removeItem(int index) {
        getInventory().clear(index);
    }
    public void runOnClick(int index, Player player) {
        MenuItemClick run = onClicks.get(index);
        if(run != null) {
            try {
                run.run(player, this);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public boolean doesHaveOnClick(int index) {
        return onClicks.containsKey(index);
    }
    public Inventory getInventory() {
        return inventory;
    }
    public void allowForGarbageCollection() {
        if(!singleUse)
            return;
        Menu.menus.remove(name);
    }

    public static HashMap<String, Menu> menus = new HashMap<>();
}
