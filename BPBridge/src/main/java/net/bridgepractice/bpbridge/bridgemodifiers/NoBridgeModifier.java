package net.bridgepractice.bpbridge.bridgemodifiers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class NoBridgeModifier implements BridgeModifier {

    @Override
    public String getGameType() {
        return "nobridge";
    }

    @Override
    public String getPrettyGameType() {
        return "The §m Bridge ";
    }

    @Override
    public void sendIntroMessage(Player player) {
        player.sendMessage("§f§l                      The §m Bridge ");
        player.sendMessage("\n§e                       Where is The Bridge?");
        player.sendMessage("\n§e               First player to score 5 goals wins!");
    }

    @Override
    public int getCountdownTime() {
        return 3;
    }

}
