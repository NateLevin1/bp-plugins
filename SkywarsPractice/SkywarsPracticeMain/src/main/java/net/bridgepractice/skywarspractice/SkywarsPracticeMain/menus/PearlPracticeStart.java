package net.bridgepractice.skywarspractice.SkywarsPracticeMain.menus;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PearlPracticeStart
  implements Listener {
  private static Inventory inv;
  private static final ItemStack hard = Utils.createGuiItem(Material.STAINED_CLAY, DyeColor.RED, "§6§k|||§4 §lHARD §6§k|||", "§a1 block wide platform");
  private static final ItemStack medium = Utils.createGuiItem(Material.STAINED_CLAY, DyeColor.YELLOW, "§6§k|||§e §lMEDIUM §6§k|||", "§a3 block wide platform");
  private static final ItemStack easy = Utils.createGuiItem(Material.STAINED_CLAY, DyeColor.GREEN, "§6§k|||§2 §lEASY §6§k|||", "§a5 block wide platform");
  private static final ItemStack hardGlass = Utils.createGuiItem(Material.STAINED_GLASS_PANE, DyeColor.RED, " ", "");
  private static final ItemStack mediumGlass = Utils.createGuiItem(Material.STAINED_GLASS_PANE, DyeColor.YELLOW, " ", "");
  private static final ItemStack easyGlass = Utils.createGuiItem(Material.STAINED_GLASS_PANE, DyeColor.GREEN, " ", "");

  public static void pearlPracticeStartGUI(Player player) {
    inv = Bukkit.createInventory(null, 27, "Pearl Practice");

    initializeItems();

    openInventory(player);
  }

  public static void initializeItems() {
    int[] easyGlassLocations = { 0, 1, 2, 9, 11, 18, 19, 20 };
    int[] mediumGlassLocations = { 3, 4, 5, 12, 14, 21, 22, 23 };
    int[] hardGlassLocations = { 6, 7, 8, 15, 17, 24, 25, 26 };

    for (int i : easyGlassLocations) {
      inv.setItem(i, easyGlass);
    }

    for (int i : mediumGlassLocations) {
      inv.setItem(i, mediumGlass);
    }

    for (int i : hardGlassLocations) {
      inv.setItem(i, hardGlass);
    }

    inv.setItem(10, easy);
    inv.setItem(13, medium);
    inv.setItem(16, hard);
  }

  public static void openInventory(Player player) {
    player.openInventory(inv);
  }


  @EventHandler
  public static void onInventoryClick(InventoryClickEvent e) {
    if (!e.getInventory().equals(inv))
      return;
    e.setCancelled(true);

    ItemStack clickedItem = e.getCurrentItem();


    if (clickedItem == null || clickedItem.getType().equals(Material.AIR))
      return;
    Player player = (Player)e.getWhoClicked();

    if (clickedItem.isSimilar(hard)) {
      player.chat("/pearl hard");
      player.closeInventory();
    } else if (clickedItem.isSimilar(medium)) {
      player.chat("/pearl medium");
      player.closeInventory();
    } else if (clickedItem.isSimilar(easy)) {
      player.chat("/pearl easy");
      player.closeInventory();
    }
  }


  @EventHandler
  public static void onInventoryClick(InventoryDragEvent e) {
    if (e.getInventory().equals(inv))
      e.setCancelled(true);
  }
}