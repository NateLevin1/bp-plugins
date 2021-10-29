package net.bridgepractice.bpnick;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.HashMap;

public class NickManager {
    private static final HashMap<String, NickedPlayer> nickedPlayers = new HashMap<>();
    public static void addNickedPlayer(ProxiedPlayer player, String nickname) {
        nickedPlayers.put(player.getName(), new NickedPlayer(nickname, player.getName()));
    }
    public static void removeNickedPlayer(ProxiedPlayer player) {
        nickedPlayers.remove(player.getName());
    }
    public static boolean isPlayerNicked(ProxiedPlayer player) {
        return isPlayerNicked(player.getName());
    }
    public static NickedPlayer getPlayerNick(ProxiedPlayer player) {
        return getPlayerNick(player.getName());
    }
    public static boolean isPlayerNicked(String name) {
        return nickedPlayers.containsKey(name);
    }
    public static NickedPlayer getPlayerNick(String name) {
        return nickedPlayers.get(name);
    }
    public static Collection<NickedPlayer> getAllNickedPlayers() {
        return nickedPlayers.values();
    }
}
