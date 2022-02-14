package net.bridgepractice.skywarspractice.SkywarsPracticeMain;

import com.google.gson.JsonObject;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class Utils {
    public static boolean isLocationInLocation(Location loc, Location pos1, Location pos2) {
        double x1 = pos1.getX();
        double z1 = pos1.getZ();

        double x2 = pos2.getX();
        double z2 = pos2.getZ();

        double xP = loc.getX();
        double zP = loc.getZ();

        return (x1 < xP && xP < x2) || (x1 > xP && xP > x2) && (z1 < zP && zP < z2) || (z1 > zP && zP > z2);
    }

    public static void sendTitle(Player player, String titleText, String subtitleText, int fadeIn, int fadeOut, int duration) {
        IChatBaseComponent chatTitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + titleText + "\"}");
        IChatBaseComponent chatSubtitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitleText + "\"}");

        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, chatTitle);
        PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, chatSubtitle);
        PacketPlayOutTitle length = new PacketPlayOutTitle(fadeIn, duration, fadeOut);

        // For some reason if we don't send this length packet then the first time we try to send a title to a player
        // after they have disconnected the title does not show up.
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);

        // send the actual title
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(title);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(subtitle);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);
    }

    public static void resetBlocks(List<Block> blocksPlaced) {
        for (Block block : blocksPlaced) {
            block.getLocation().getBlock().setType(Material.AIR);
        }
    }

    public static void sendPlayerToSpawn(Player player) {
        player.setFallDistance(0F);
        player.setHealth(20);
        player.teleport(Main.spawn);
    }

    public static void sendWebhookSync(JsonObject object) {
        // https://stackoverflow.com/a/35013372/13608595
        try {
            String discordWebhook = "https://discord.com/api/webhooks/879108049489514506/tpuJCqR_TbUn1tzUyFGTU7OBdUFl4oYqyQ4AYcL__X7MsMhke5dr0xwCPOF1nNxx-Z5u";
            URL url = new URL(discordWebhook);
            URLConnection con = url.openConnection();
            HttpsURLConnection req = (HttpsURLConnection) con;
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
            if(responseCode < 200 || responseCode >= 300) {
                Main.instance.getLogger().severe(responseCode + " " + req.getResponseMessage());
            }
            req.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String prettifyNumber(float num) {
        // this is a *horrible* solution but it works
        String s = String.valueOf(Math.ceil(num / 1000f * 8f) / 8f);
        return padWithZeroes(s);
    }
    public static String padWithZeroes(String s) {
        String[] dec = s.split("\\.");
        if(dec.length == 1) {
            return s + ".000";
        } else if(dec[1].length() == 1) {
            return s + "00";
        } else if(dec[1].length() == 2) {
            return s + "0";
        } else {
            return s;
        }
    }
}
