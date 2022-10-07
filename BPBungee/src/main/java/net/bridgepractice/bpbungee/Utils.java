package net.bridgepractice.bpbungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static String getUuidFromNameSync(String name) throws IOException {
        ProxiedPlayer possiblyOnlinePlayer = BPBungee.instance.getProxy().getPlayer(name);
        if(possiblyOnlinePlayer != null) {
            return possiblyOnlinePlayer.getUniqueId().toString();
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

    public static void playSound(ProxiedPlayer player, String sound) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlaySound");
        out.writeUTF(sound);
        out.writeUTF(player.getUniqueId().toString());
        player.getServer().getInfo().sendData("bp:messages", out.toByteArray());
    }

    public static void broadcastToPermission(String permission, BaseComponent... components) {
        for(ProxiedPlayer player : BPBungee.instance.getProxy().getPlayers()) {
            if(player.hasPermission(permission)) {
                player.sendMessage(components);
            }
        }
    }

    public static void sendWebhook(JsonObject object, CommandSender playerToSendErrorsTo, String webhookURL) {
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
            // https://stackoverflow.com/a/35013372/13608595
            try {
                URL url = new URL(webhookURL);
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
                    if(playerToSendErrorsTo != null) {
                        playerToSendErrorsTo.sendMessage(responseCode + " " + req.getResponseMessage());
                    }
                }
                req.disconnect();
            } catch (IOException e) {
                if(playerToSendErrorsTo != null) {
                    playerToSendErrorsTo.sendMessage(e.getMessage());
                }
                e.printStackTrace();
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    public static void rankedChatToDiscord(String uuid, String message, String playerName, CommandSender playerToSendErrorsTo) {
        JsonObject webhook = new JsonObject();
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        JsonObject author = new JsonObject();
        webhook.add("embeds", embeds);
        embeds.add(embed);
        embed.add("author", author);
        author.addProperty("name", playerName + ": " + message);
        author.addProperty("icon_url", "https://crafatar.com/renders/head/" + uuid + "?overlay=true&size=64");
        embed.addProperty("color", Integer.valueOf(16769280));
        sendWebhook(webhook, playerToSendErrorsTo, BPBungee.chatWebhook);
    }

    public static void sendPunishmentWebhook(boolean isBan, boolean isWarn, String punishmentVerb, String reason, int length, String bannerName, String bannerUuid, String bannedName, CommandSender playerToSendErrorsTo) {
        JsonObject webhook = new JsonObject();
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        JsonObject author = new JsonObject();
        JsonArray fields = new JsonArray();
        webhook.add("embeds", embeds);
        embeds.add(embed);

        embed.add("author", author);
        author.addProperty("name", "Player "+(punishmentVerb.substring(0, 1).toUpperCase() + punishmentVerb.substring(1)));
        author.addProperty("icon_url", "https://crafatar.com/renders/head/" + bannerUuid + "?overlay=true&size=64");

        embed.addProperty("title", bannerName+" "+punishmentVerb+" `"+bannedName+"`");

        embed.addProperty("color", isBan ? 0xff0033 : 0x00c58e); // used to be 39c2ff

        embed.add("fields", fields);

        if(reason != null) {
            JsonObject reasonField = new JsonObject();
            reasonField.addProperty("name", "Reason");
            reasonField.addProperty("value", reason);
            reasonField.addProperty("inline", true);
            fields.add(reasonField);
        }

        if(!isWarn) {
            JsonObject lengthField = new JsonObject();
            lengthField.addProperty("name", "Length");
            lengthField.addProperty("value", length + " days");
            lengthField.addProperty("inline", true);
            fields.add(lengthField);
        }

        JsonObject thumbnail = new JsonObject();
        embed.add("thumbnail", thumbnail);
        thumbnail.addProperty("url", "https://minotar.net/armor/bust/" + bannedName + "/64");

        embed.addProperty("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        Utils.sendWebhook(webhook, playerToSendErrorsTo, BPBungee.punishmentWebhook);
    }

    public static String getMuteReason(ProxiedPlayer player) {
        try (PreparedStatement statement = BPBungee.connection.prepareStatement("SELECT muteReason FROM players WHERE uuid=?;")) {
            statement.setString(1, player.getUniqueId().toString());
            ResultSet res = statement.executeQuery();
            if (!res.next())
                throw new SQLException("Did not get a row from the database. Player Name: " + player.getName() + ". Player UUID: " + player.getUniqueId());
            return res.getString(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§cOh! Something went wrong fetching your reason from our database. Please open a ticket on the discord!");
            return "Chat Infraction";
        }
    }

    public static BaseComponent[] getBanMessage(int daysLeft, String reason, boolean isIpBan) {
        return new ComponentBuilder()
                .append(new ComponentBuilder((isIpBan ? "Your IP is" : "You are")+" temporarily banned for ").bold(true).color(ChatColor.RED).create())
                .append(new ComponentBuilder(daysLeft+" days").color(ChatColor.WHITE).create())
                .append(new ComponentBuilder(" from this server!\n\n").bold(false).color(ChatColor.RED).create())
                .append(new ComponentBuilder("Reason: ").color(ChatColor.GRAY).create())
                .append(new ComponentBuilder(reason).color(ChatColor.WHITE).create())
                .append(new ComponentBuilder("\n\nTo appeal your ban, join the Discord and open a ticket.").color(ChatColor.WHITE).create())
                .append(new ComponentBuilder("\n\nJoin the Discord by going to ").color(ChatColor.GOLD).create())
                .append(new ComponentBuilder("https://bridgepractice.net/discord").color(ChatColor.AQUA).underlined(true).create())
                .append(new ComponentBuilder("\nThen accept the rules, go to #support, and click the \"Support\" button.").underlined(false).color(ChatColor.GOLD).create())
                .create();
    }

    public static String getGameModeName(MultiplayerMode mode) {
        switch(mode) {
            case pvp:
                return "Bridge PvP 1v1";
            case unranked:
                return "Bridge Duel";
            case nobridge:
                return "NoBridge Duel";
            default:
                return "Unknown";
        }
    }

    public static void log(BaseComponent[] components, String permission) {
        broadcastToPermission(permission, new ComponentBuilder("|||").color(ChatColor.DARK_AQUA).obfuscated(true).append(" ").obfuscated(false).appendLegacy("§3[§bB§cP§3] ").append(components).create());
    }
    public static void log(String message) {
        log(new ComponentBuilder(message).create(), "bridgepractice.moderation.chat");
    }
    public static void log(BaseComponent[] components) {
        log(components, "bridgepractice.moderation.chat");
    }
}
