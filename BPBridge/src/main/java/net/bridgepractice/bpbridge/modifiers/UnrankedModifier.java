package net.bridgepractice.bpbridge.modifiers;

import org.bukkit.entity.Player;

public class UnrankedModifier implements GameModifier {
    @Override
    public String getGameType() {
        return "unranked";
    }
    @Override
    public String getPrettyGameType() {
        return "Unranked";
    }
    @Override
    public void sendIntroMessage(Player player) {
        player.sendMessage("§f§l                 The Bridge Duel");
        player.sendMessage("\n§e            Cross the bridge to score goals.");
        player.sendMessage("§e      Knock off your opponent to gain a clear path.");
        player.sendMessage("\n§e      First player to score 5 goals wins!");
    }
}
