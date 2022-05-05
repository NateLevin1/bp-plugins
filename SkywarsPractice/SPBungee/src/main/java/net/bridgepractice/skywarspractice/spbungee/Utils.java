package net.bridgepractice.skywarspractice.spbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class Utils {
    public static void broadcastToPermission(String permission, BaseComponent... components) {
        for(ProxiedPlayer player : SPBungee.instance.getProxy().getPlayers()) {
            if(player.hasPermission(permission)) {
                player.sendMessage(components);
            }
        }
    }

    public static void log(BaseComponent[] components, String permission) {
        broadcastToPermission(permission, new ComponentBuilder("|||").color(ChatColor.DARK_AQUA).obfuscated(true).append(" ").obfuscated(false).appendLegacy("§b[§2S§3P§b] §d").append(components).create());
    }
    public static void log(String message) {
        log(new ComponentBuilder(message).create(), "bridgepractice.moderation.chat");
    }
    public static void log(BaseComponent[] components) {
        log(components, "bridgepractice.moderation.chat");
    }
}
