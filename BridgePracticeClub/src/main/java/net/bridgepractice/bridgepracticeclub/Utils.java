package net.bridgepractice.bridgepracticeclub;

import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class Utils {
    private static String discordWebhook = "https://discord.com/api/webhooks/879108049489514506/tpuJCqR_TbUn1tzUyFGTU7OBdUFl4oYqyQ4AYcL__X7MsMhke5dr0xwCPOF1nNxx-Z5u";

    public static void sendWebhookSync(JsonObject object) {
        // https://stackoverflow.com/a/35013372/13608595
        try {
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
                Bridge.instance.getLogger().severe(responseCode + " " + req.getResponseMessage());
            }
            req.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}