package net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import net.bridgepractice.RavenAntiCheat.RavenAntiCheat;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.menus.PearlPracticeStart;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PearlPractice implements CommandExecutor, Listener {
  public static HashMap<UUID, Boolean> allowedToThrowPearl = new HashMap<>();
  public static HashMap<UUID, Boolean> isRandomSlot = new HashMap<>();
  public static HashMap<UUID, Boolean> velocitySetTaskRun = new HashMap<>();
  private static final ItemStack menuItem = Utils.createGuiItem(Material.EMERALD, "§5Settings", "§aChange your Pearl Practice settings!");
  private static final ItemStack randomSlotItem = Utils.createGuiItem(Material.EYE_OF_ENDER, "§5Enable Random Item Slot", "§aMake the pearl go to a random spot in your hotbar!");
  private static final ItemStack randomSlotItemEnchanted = Utils.createGuiItem(Material.EYE_OF_ENDER, "§5Disable Random Item Slot", "§aMake the pearl go to a random spot in your hotbar!");

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      return true;
    }
    Player player = (Player) sender;
    if (!Main.instance.queueingEnabled) {
      player.sendMessage("§cQueueing is currently disabled!");
      return true;
    }
    if (Main.pearlPracticeQueue.contains(player.getUniqueId())) {
      player.sendMessage("§cYou are already in the Pearl Practice queue!");
    } else if (args.length != 0) {
      int lastplace = (Main.pearlPracticeQueue.toArray()).length;
      Main.pearlPracticeQueue.add(lastplace, player.getUniqueId());
      String difficulty = args[0];
      boolean b = (Objects.equals(difficulty, "medium") || Objects.equals(difficulty, "hard"));
      if (Main.playersInPearlPractice.containsKey(player.getUniqueId())) {
        if (Main.playersInPearlPractice.get(player.getUniqueId()).contains(":")) {
          String mapname = Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[0];
          if (b) {
            Main.playersInPearlPractice.put(player.getUniqueId(), difficulty);
          } else {
            Main.playersInPearlPractice.put(player.getUniqueId(), "easy");
          }
          start(mapname, player, true);
        }
      } else {
        if (b) {
          Main.playersInPearlPractice.put(player.getUniqueId(), difficulty);
        } else {
          Main.playersInPearlPractice.put(player.getUniqueId(), "easy");
        }
        String placeMessage = "§aThere are §b" + Main.pearlPracticeQueue.indexOf(player.getUniqueId()) + "§a players ahead of you.";
        if (Integer.toString(Main.pearlPracticeQueue.indexOf(player.getUniqueId())).equals("0")) {
          if (Main.availablePearlPracticeMaps.isEmpty()) {
            player.sendMessage("§b§lYou are next in queue!");
          }
          PlayerMoveEvent pme = new PlayerMoveEvent(player, player.getLocation(), player.getLocation());
          Bukkit.getPluginManager().callEvent(pme);
        } else if (placeMessage.equals("§aThere are §b1§a players ahead of you.")) {
          player.sendMessage("§aThere is §b1§a player ahead of you.");
        } else {
          player.sendMessage(placeMessage);
        }
      }
    } else {
      PearlPracticeStart.pearlPracticeStartGUI(player);
    }
    if (randomSlotItemEnchanted.getEnchantments().containsKey(Enchantment.ARROW_INFINITE)) {
      randomSlotItemEnchanted.removeEnchantment(Enchantment.ARROW_INFINITE);
    }
    randomSlotItemEnchanted.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
    return true;
  }

  public static void start(String mapname, Player player, boolean inGame) {
    String difficulty;
    if (!Main.playersInPearlPractice.containsKey(player.getUniqueId())) {
      Main.playersInPearlPractice.put(player.getUniqueId(), "easy");
    }
    if (Main.playersInPearlPractice.get(player.getUniqueId()).contains(":")) {
      difficulty = Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[1];
    } else {
      difficulty = Main.playersInPearlPractice.get(player.getUniqueId());
    }

    setStartIsland(mapname, true);

    switch (mapname) {
      case "pearlone":
        player.teleport(new Location(Bukkit.getWorld("skywars"), -30.5D, 85.1D, -66.5D, 180.0F, 0.0F));
        break;
      case "pearltwo":
        player.teleport(new Location(Bukkit.getWorld("skywars"), -43.5D, 85.1D, -66.5D, 180.0F, 0.0F));
        break;
      case "pearlthree":
        player.teleport(new Location(Bukkit.getWorld("skywars"), -56.5D, 85.1D, -66.5D, 180.0F, 0.0F));
        break;
      case "pearlfour":
        player.teleport(new Location(Bukkit.getWorld("skywars"), -69.5D, 85.1D, -66.5D, 180.0F, 0.0F));
        break;
    }

    PlayerInventory playerInventory = player.getInventory();
    playerInventory.clear();
    Main.availablePearlPracticeMaps.remove(mapname);
    Main.pearlPracticeQueue.remove(player.getUniqueId());
    Main.playersInPearlPractice.put(player.getUniqueId(), mapname + ":" + difficulty + ":waiting");
    player.setGameMode(GameMode.ADVENTURE);

    playerInventory.setItem(8, menuItem);
    playerInventory.setItem(7, randomSlotItem);
    if (isRandomSlot.containsKey(player.getUniqueId())) {
      playerInventory.setItem(7, randomSlotItemEnchanted);
    }

    if (!inGame) {
      player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0F, 1.0F);
      player.sendMessage(ChatColor.GOLD + "When you're ready... " + ChatColor.GREEN + "GO!");
    }
  }

  public static void gameP2(final String mapname, final Player player) {
    setStartIsland(mapname, false);
    String[] data = Main.playersInPearlPractice.get(player.getUniqueId()).split(":");
    final String difficulty = data[1];
    data[2] = "ingame";

    Main.playersInPearlPractice.put(player.getUniqueId(), String.join(":", data));

    PlayerInventory playerInventory = player.getInventory();
    playerInventory.clear();
    ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
    pearl.setAmount(1);
    playerInventory.setItemInHand(pearl);

    BukkitRunnable hitPlayerOff = new BukkitRunnable()
      {
        public void run() {
          if (velocitySetTaskRun.containsKey(player.getUniqueId())) {
            this.cancel();
            velocitySetTaskRun.remove(player.getUniqueId());
          }
          if (!Objects.equals(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[2], "ingame")) {
            this.cancel();
            return;
          }
          int leftOrRight = (Math.random() > 0.5D) ? -1 : 1;
          double hitDistanceModifier = Objects.equals(difficulty, "easy") ? 1.2D : 1.0D;

          RavenAntiCheat.emulatePlayerTakeKnockback(player);

          player.setVelocity(new Vector((leftOrRight) * hitDistanceModifier * (Math.random() * 0.04D + 0.45D), 0.5D, (leftOrRight) * (Math.random() * 0.055D + 0.45D)));

          String[] data = Main.playersInPearlPractice.get(player.getUniqueId()).split(":");
          data[2] = "launched";

          Main.playersInPearlPractice.put(player.getUniqueId(), String.join(":", data));

          if (isRandomSlot.containsKey(player.getUniqueId())) {
            playerInventory.clear();
            Random random = new Random();
            ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
            pearl.setAmount(1);
            playerInventory.setItem(random.nextInt(10), pearl);
          }

          player.playSound(player.getLocation(), Sound.HURT_FLESH, 1.0F, 1.0F);
          EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
          entityPlayer.playerConnection.sendPacket(new PacketPlayOutAnimation(entityPlayer, 1));

          switch (mapname) {
            case "pearlone":
              if (Objects.equals(difficulty, "easy")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -33.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -29.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2); break;
              }  if (Objects.equals(difficulty, "medium")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -32.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -30.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2);
              }
              break;
            case "pearltwo":
              if (Objects.equals(difficulty, "easy")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -46.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -42.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2); break;
              }  if (Objects.equals(difficulty, "medium")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -45.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -43.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2);
              }
              break;
            case "pearlthree":
              if (Objects.equals(difficulty, "easy")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -59.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -55.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2); break;
              }  if (Objects.equals(difficulty, "medium")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -58.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -56.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2);
              }
              break;
            case "pearlfour":
              if (Objects.equals(difficulty, "easy")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -72.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -68.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2); break;
              }  if (Objects.equals(difficulty, "medium")) {
                Location pos1 = new Location(Bukkit.getWorld("skywars"), -71.0D, 84.0D, -71.0D);
                Location pos2 = new Location(Bukkit.getWorld("skywars"), -69.0D, 84.0D, -119.0D);
                Utils.setSquare(Material.GRASS, pos1, pos2);
              }
              break;
          }

          allowedToThrowPearl.put(player.getUniqueId(), true);

          new BukkitRunnable() {
            @Override
            public void run() {
              if (!Objects.equals(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[2], "launched")) {
                this.cancel();
                return;
              }
              PearlPractice.lose(Main.playersInPearlPractice.get(player.getUniqueId()).split(":")[0], player);
            }
          }.runTaskLater(Main.instance, 40);

          cancel();
        }
      };

    hitPlayerOff.runTaskLater(Main.instance, 65L);
  }

  public static void win(final String mapname, final Player player) {
    velocitySetTaskRun.put(player.getUniqueId(), true);
    allowedToThrowPearl.remove(player.getUniqueId());
    String[] data = Main.playersInPearlPractice.get(player.getUniqueId()).split(":");
    String difficulty = data[1];
    data[2] = "won";
    setStartIsland(mapname, true);

    Main.playersInPearlPractice.put(player.getUniqueId(), String.join(":", data));

    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0F, 1.0F);

    switch (difficulty) {
      case "easy":
        Utils.sendTitle(player, "§aYou clutched! §e+1xp", "", 10, 10, 20);
        Main.givePlayerXP(player, 1);
        break;
      case "medium":
        Utils.sendTitle(player, "§aYou clutched! §e+3xp", "", 10, 10, 20);
        Main.givePlayerXP(player, 3);
        break;
      case "hard":
        Utils.sendTitle(player, "§aYou clutched! §e+5xp", "", 10, 10, 20);
        Main.givePlayerXP(player, 5);
        break;
    }
    removePearlsFromPlayer(player);

    new BukkitRunnable() {
      @Override
      public void run() {
        resetMap(mapname);
        start(mapname, player, true);
      }
    }.runTaskLater(Main.instance, 37);
  }

  public static void lose(String mapname, Player player) {
    velocitySetTaskRun.put(player.getUniqueId(), true);
    allowedToThrowPearl.remove(player.getUniqueId());
    setStartIsland(mapname, true);
    resetMap(mapname);
    removePearlsFromPlayer(player);

    start(mapname, player, true);
  }

  public static void disconnect(Player player, String mapname) {
    velocitySetTaskRun.put(player.getUniqueId(), true);
    allowedToThrowPearl.remove(player.getUniqueId());
    setStartIsland(mapname, true);
    resetMap(mapname);

    removePearlsFromPlayer(player);

    Main.availablePearlPracticeMaps.add(mapname);
    Main.playersInPearlPractice.remove(player.getUniqueId());
    Main.pearlPracticeQueue.remove(player.getUniqueId());

    player.getInventory().clear();

    if (player.isOnline()) {
      Utils.sendPlayerToSpawn(player);
    }
  }

  @EventHandler
  public static void onPlayerInteract(PlayerInteractEvent e) {
    if (e.getItem() != null &&
      e.getItem().isSimilar(menuItem) && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
      PearlPracticeStart.pearlPracticeStartGUI(e.getPlayer());
    }
    Player player = e.getPlayer();
    if (e.getItem() != null &&
            e.getItem().isSimilar(randomSlotItem) && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
      player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
      player.getInventory().setItem(7, randomSlotItemEnchanted);
      isRandomSlot.put(player.getUniqueId(), true);
    }
    if (e.getItem() != null &&
            e.getItem().isSimilar(randomSlotItemEnchanted) && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
      player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
      player.getInventory().setItem(7, randomSlotItem);
      isRandomSlot.remove(player.getUniqueId());
    }

    if(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
      Player p = e.getPlayer();
      if(p.getItemInHand() != null) {
        if(p.getItemInHand().getType().equals(Material.ENDER_PEARL) && !allowedToThrowPearl.containsKey(p.getUniqueId())) {
          PearlPractice.lose(Main.playersInPearlPractice.get(p.getUniqueId()).split(":")[0], p);
          p.sendMessage("§cYou threw the pearl too early!");
          e.setCancelled(true);
        }
      }
    }
  }

  public static void resetMap(String mapname) {
    Location s1Pos1, s1Pos2, s2Pos1, s2Pos2;
    switch (mapname) {
      case "pearlone":
        s1Pos1 = new Location(Bukkit.getWorld("skywars"), -33.0D, 84.0D, -119.0D);
        s1Pos2 = new Location(Bukkit.getWorld("skywars"), -32.0D, 84.0D, -71.0D);

        s2Pos1 = new Location(Bukkit.getWorld("skywars"), -30.0D, 84.0D, -119.0D);
        s2Pos2 = new Location(Bukkit.getWorld("skywars"), -29.0D, 84.0D, -71.0D);
        break;
      case "pearltwo":
        s1Pos1 = new Location(Bukkit.getWorld("skywars"), -46.0D, 84.0D, -119.0D);
        s1Pos2 = new Location(Bukkit.getWorld("skywars"), -45.0D, 84.0D, -71.0D);

        s2Pos1 = new Location(Bukkit.getWorld("skywars"), -43.0D, 84.0D, -119.0D);
        s2Pos2 = new Location(Bukkit.getWorld("skywars"), -42.0D, 84.0D, -71.0D);
        break;
      case "pearlthree":
        s1Pos1 = new Location(Bukkit.getWorld("skywars"), -59.0D, 84.0D, -119.0D);
        s1Pos2 = new Location(Bukkit.getWorld("skywars"), -58.0D, 84.0D, -71.0D);

        s2Pos1 = new Location(Bukkit.getWorld("skywars"), -56.0D, 84.0D, -119.0D);
        s2Pos2 = new Location(Bukkit.getWorld("skywars"), -55.0D, 84.0D, -71.0D);
        break;
      case "pearlfour":
        s1Pos1 = new Location(Bukkit.getWorld("skywars"), -72.0D, 84.0D, -119.0D);
        s1Pos2 = new Location(Bukkit.getWorld("skywars"), -71.0D, 84.0D, -71.0D);

        s2Pos1 = new Location(Bukkit.getWorld("skywars"), -69.0D, 84.0D, -119.0D);
        s2Pos2 = new Location(Bukkit.getWorld("skywars"), -68.0D, 84.0D, -71.0D);
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + mapname);
    }

    Utils.setSquare(Material.AIR, s1Pos1, s1Pos2);
    Utils.setSquare(Material.AIR, s2Pos1, s2Pos2);
  }

  public static void setStartIsland(String mapname, boolean shown) {
    if (shown) {
      Location qPos1, qPos2, rPos1, rPos2;
      switch (mapname) {
        case "pearlone":
          qPos1 = new Location(Bukkit.getWorld("skywars"), -33, 84, -69);
          qPos2 = new Location(Bukkit.getWorld("skywars"), -29, 84, -65);

          rPos1 = new Location(Bukkit.getWorld("skywars"), -33, 84, -70);
          rPos2 = new Location(Bukkit.getWorld("skywars"), -29, 84, -70);
          break;
        case "pearltwo":
          qPos1 = new Location(Bukkit.getWorld("skywars"), -46, 84, -69);
          qPos2 = new Location(Bukkit.getWorld("skywars"), -42, 84, -65);

          rPos1 = new Location(Bukkit.getWorld("skywars"), -46, 84, -70);
          rPos2 = new Location(Bukkit.getWorld("skywars"), -42, 84, -70);
          break;
        case "pearlthree":
          qPos1 = new Location(Bukkit.getWorld("skywars"), -59, 84, -69);
          qPos2 = new Location(Bukkit.getWorld("skywars"), -55, 84, -65);

          rPos1 = new Location(Bukkit.getWorld("skywars"), -59, 84, -70);
          rPos2 = new Location(Bukkit.getWorld("skywars"), -55, 84, -70);
          break;
        case "pearlfour":
          qPos1 = new Location(Bukkit.getWorld("skywars"), -72, 84, -69);
          qPos2 = new Location(Bukkit.getWorld("skywars"), -68, 84, -65);

          rPos1 = new Location(Bukkit.getWorld("skywars"), -72, 84, -70);
          rPos2 = new Location(Bukkit.getWorld("skywars"), -68, 84, -70);
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + mapname);
      }

      Utils.setSquare(Material.QUARTZ_BLOCK, qPos1, qPos2);
      Block redWool = Bukkit.getWorld("skywars").getBlockAt(rPos1);
      redWool.setType(Material.WOOL);
      redWool.setData(DyeColor.RED.getData());
      Utils.setSquare(redWool, rPos1, rPos2);
    } else {
      Location islandPos1, islandPos2;
      switch (mapname) {
        case "pearlone":
          islandPos1 = new Location(Bukkit.getWorld("skywars"), -33, 84, -70);
          islandPos2 = new Location(Bukkit.getWorld("skywars"), -29, 84, -65);
          break;
        case "pearltwo":
          islandPos1 = new Location(Bukkit.getWorld("skywars"), -46, 84, -70);
          islandPos2 = new Location(Bukkit.getWorld("skywars"), -42, 84, -65);
          break;
        case "pearlthree":
          islandPos1 = new Location(Bukkit.getWorld("skywars"), -59, 84, -70);
          islandPos2 = new Location(Bukkit.getWorld("skywars"), -55, 84, -65);
          break;
        case "pearlfour":
          islandPos1 = new Location(Bukkit.getWorld("skywars"), -72, 84, -70);
          islandPos2 = new Location(Bukkit.getWorld("skywars"), -68, 84, -65);
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + mapname);
      }

      Utils.setSquare(Material.AIR, islandPos1, islandPos2);
    }
  }

  private static void removePearlsFromPlayer(Player player) {
    Bukkit.getWorld("skywars").getEntities().forEach(entity -> {
      if(entity instanceof EnderPearl) {
        EnderPearl ep = (EnderPearl) entity;
        if (ep.getShooter() instanceof Player) {
          Player shooter = (Player) ep.getShooter();
          if (shooter.getUniqueId() == player.getUniqueId()) {
            entity.remove();
          }
        }
      }
    });
  }
}