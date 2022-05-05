package net.bridgepractice.skywarspractice.SkywarsPracticeMain;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Utils {
  public static boolean isLocationInLocation(Location loc, Location pos1, Location pos2) {
    double x1 = pos1.getX();
    double z1 = pos1.getZ();

    double x2 = pos2.getX();
    double z2 = pos2.getZ();

    double xP = loc.getX();
    double zP = loc.getZ();

    return ((x1 < xP && xP < x2) || (x1 > xP && xP > x2 && z1 < zP && zP < z2) || (z1 > zP && zP > z2));
  }

  public static void sendTitle(Player player, String titleText, String subtitleText, int fadeIn, int fadeOut, int duration) {
    IChatBaseComponent chatTitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + titleText + "\"}");
    IChatBaseComponent chatSubtitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitleText + "\"}");

    PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, chatTitle);
    PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, chatSubtitle);
    PacketPlayOutTitle length = new PacketPlayOutTitle(fadeIn, duration, fadeOut);



    (((CraftPlayer)player).getHandle()).playerConnection.sendPacket((Packet)length);


    (((CraftPlayer)player).getHandle()).playerConnection.sendPacket((Packet)title);
    (((CraftPlayer)player).getHandle()).playerConnection.sendPacket((Packet)subtitle);
    (((CraftPlayer)player).getHandle()).playerConnection.sendPacket((Packet)length);
  }

  public static void resetBlocks(List<Block> blocksPlaced) {
    for (Block block : blocksPlaced) {
      block.getLocation().getBlock().setType(Material.AIR);
    }
  }

  public static void sendPlayerToSpawn(Player player) {
    player.setFallDistance(0.0F);
    player.setHealth(20.0D);
    player.setGameMode(GameMode.ADVENTURE);
    player.teleport(Main.spawn);
  }


  public static void sendWebhookSync(JsonObject object) {
    try {
      String discordWebhook = "https://discord.com/api/webhooks/879108049489514506/tpuJCqR_TbUn1tzUyFGTU7OBdUFl4oYqyQ4AYcL__X7MsMhke5dr0xwCPOF1nNxx-Z5u";
      URL url = new URL(discordWebhook);
      URLConnection con = url.openConnection();
      HttpsURLConnection req = (HttpsURLConnection)con;
      req.setRequestMethod("POST");
      req.setDoOutput(true);
      byte[] out = object.toString().getBytes(StandardCharsets.UTF_8);
      req.setFixedLengthStreamingMode(out.length);
      req.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      req.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15");
      req.connect();
      OutputStream os = req.getOutputStream();
      os.write(out);
      os.flush();
      int responseCode = req.getResponseCode();
      if (responseCode < 200 || responseCode >= 300) {
          System.out.println(responseCode + " " + req.getResponseMessage());
      }
      req.disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public static String prettifyNumber(float num) {
    String s = String.valueOf(Math.ceil((num / 1000.0F * 8.0F)) / 8.0D);
    return padWithZeroes(s);
  }
  public static String padWithZeroes(String s) {
    String[] dec = s.split("\\.");
    if (dec.length == 1)
      return s + ".000";
    if (dec[1].length() == 1)
      return s + "00";
    if (dec[1].length() == 2) {
      return s + "0";
    }
    return s;
  }


  public static void setSquare(final Material material, Location location1, Location location2) {
    final World world = location1.getWorld();
    final int highestX = Math.max(location2.getBlockX(), location1.getBlockX());
    final int lowestX = Math.min(location2.getBlockX(), location1.getBlockX());

    final int highestY = Math.max(location2.getBlockY(), location1.getBlockY());
    final int lowestY = Math.min(location2.getBlockY(), location1.getBlockY());

    final int highestZ = Math.max(location2.getBlockZ(), location1.getBlockZ());
    final int lowestZ = Math.min(location2.getBlockZ(), location1.getBlockZ());

    (new BukkitRunnable() {
        int lastRun = 0;

        public void run() {
          HashMap<Chunk, HashSet<Block>> chunkSplitter = new HashMap<>();
          for (int x = lowestX; x <= highestX; x++) {
            for (int z = lowestZ; z <= highestZ; z++) {
              for (int y = lowestY; y <= highestY; y++) {
                Location location = new Location(world, x, y, z);
                Chunk chunk = location.getChunk();
                Block block = location.getBlock();
                if (chunkSplitter.containsKey(chunk)) {

                  HashSet<Block> blockSet = chunkSplitter.get(chunk);
                  if (material != block.getType()) {
                    blockSet.add(block);
                    chunkSplitter.replace(chunk, blockSet);
                  }

                } else {

                  HashSet<Block> blockSet = new HashSet<>();
                  if (material != block.getType()) {
                    blockSet.add(block);
                    chunkSplitter.put(chunk, blockSet);
                  }
                }
              }
            }
          }
          for (HashSet<Block> entry : chunkSplitter.values()) {

            (new BukkitRunnable()
              {
                public void run()
                {
                  for (Block block : entry)
                  {
                    block.setType(material);
                  }
                }
              }).runTaskLater((Plugin)Main.instance, (this.lastRun + 3));
            this.lastRun++;
          }
        }
      }).runTaskAsynchronously((Plugin)Main.instance);
  }

  public static void setSquare(Block blockToSet, Location location1, Location location2) {
    final World world = location1.getWorld();
    final int highestX = Math.max(location2.getBlockX(), location1.getBlockX());
    final int lowestX = Math.min(location2.getBlockX(), location1.getBlockX());

    final int highestY = Math.max(location2.getBlockY(), location1.getBlockY());
    final int lowestY = Math.min(location2.getBlockY(), location1.getBlockY());

    final int highestZ = Math.max(location2.getBlockZ(), location1.getBlockZ());
    final int lowestZ = Math.min(location2.getBlockZ(), location1.getBlockZ());

    (new BukkitRunnable() {
      int lastRun = 0;

      public void run() {
        HashMap<Chunk, HashSet<Block>> chunkSplitter = new HashMap<>();
        for (int x = lowestX; x <= highestX; x++) {
          for (int z = lowestZ; z <= highestZ; z++) {
            for (int y = lowestY; y <= highestY; y++) {
              Location location = new Location(world, x, y, z);
              Chunk chunk = location.getChunk();
              Block block = location.getBlock();
              if (chunkSplitter.containsKey(chunk)) {

                HashSet<Block> blockSet = chunkSplitter.get(chunk);
                if (blockToSet.getType() != block.getType()) {
                  blockSet.add(block);
                  chunkSplitter.replace(chunk, blockSet);
                }

              } else {

                HashSet<Block> blockSet = new HashSet<>();
                if (blockToSet.getType() != block.getType()) {
                  blockSet.add(block);
                  chunkSplitter.put(chunk, blockSet);
                }
              }
            }
          }
        }
        for (HashSet<Block> entry : chunkSplitter.values()) {

          (new BukkitRunnable()
          {
            public void run()
            {
              for (Block block : entry)
              {
                block.setType(blockToSet.getType());
                block.setData(blockToSet.getData());
              }
            }
          }).runTaskLater(Main.instance, (this.lastRun + 3));
          this.lastRun++;
        }
      }
    }).runTaskAsynchronously(Main.instance);
  }

  public static ItemStack createGuiItem(Material material, String name, String... lore) {
    ItemStack item = new ItemStack(material, 1);
    ItemMeta meta = item.getItemMeta();


    meta.setDisplayName(name);


    meta.setLore(Arrays.asList(lore));

    item.setItemMeta(meta);

    return item;
  }

  public static ItemStack createGuiItem(Material material, DyeColor color, String name, String... lore) {
    ItemStack item = new ItemStack(material, 1, (short)color.getData());
    ItemMeta meta = item.getItemMeta();


    meta.setDisplayName(name);


    meta.setLore(Arrays.asList(lore));

    item.setItemMeta(meta);

    return item;
  }
}