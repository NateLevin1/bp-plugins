package net.bridgepractice.bpreport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static void sendWebhookSync(JsonObject object) {
        // https://stackoverflow.com/a/35013372/13608595
        try {
            URL url = new URL(BPReport.discordWebhook);
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
                BPReport.instance.getLogger().severe(responseCode + " " + req.getResponseMessage());
            }
            req.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static ItemStack makeItem(Material material, int amount, String itemName, String[] lore, int blockModifier) {
        List<String> loreList = new ArrayList<>();
        for(String line : lore) {
            loreList.add("§7" + line);
        }
        ItemStack item;
        if(blockModifier == -1) {
            item = new ItemStack(material, amount);
        } else {
            item = new ItemStack(material, amount, (byte) blockModifier);
        }
        ItemMeta itemItemMeta = item.getItemMeta();
        if(itemName != null) {
            itemItemMeta.setDisplayName("§r" + itemName);
        }
        itemItemMeta.setLore(loreList);
        item.setItemMeta(itemItemMeta);
        return item;
    }
    public static String getUuidFromName(String name) throws IOException {
        Player possiblyExistingPlayer = BPReport.instance.getServer().getPlayer(name);
        if(possiblyExistingPlayer != null) {
            return possiblyExistingPlayer.getUniqueId().toString();
        }
        return getJSON("https://api.mojang.com/users/profiles/minecraft/" + name).get("id").getAsString().replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5");
    }
    public static JsonObject getJSON(String urlString) throws IOException {
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        JsonElement parsed = new JsonParser().parse(in);
        if(!parsed.isJsonObject()) {
            throw new IOException("Did not get a JSON object back");
        }
        JsonObject result = parsed.getAsJsonObject();
        in.close();
        return result;
    }
}
