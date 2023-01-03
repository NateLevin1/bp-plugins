package net.bridgepractice.bpbridge.bridgemodifiers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.bridgepractice.bpbridge.BPBridge;
import net.bridgepractice.bpbridge.Maps;
import net.bridgepractice.bpbridge.Utils;
import net.bridgepractice.bpbridge.games.BridgeBase;
import org.bukkit.entity.Player;

public class TourneyModifier implements BridgeModifier{
    @Override
    public String getGameType() { return "tourney"; }

    @Override
    public String getPrettyGameType() { return "Tournament"; }

    @Override
    public void sendIntroMessage(Player player) {
        player.sendMessage("§f§l                 The Bridge Duel");
        player.sendMessage("\n§e            Cross the bridge to score goals.");
        player.sendMessage("§e      Knock off your opponent to gain a clear path.");
        player.sendMessage("\n§e      First player to score 5 goals wins!");
    }

    @Override
    public void onBeforeStart(BridgeBase bridgeBase) {
        System.out.println("Started tourney game! WORK OR YOU'RE GAY");
        Utils.sendToTourneyCommandQueue("ngame", bridgeBase.getMemberOfTeam("red").getUniqueId().toString()+"|"+bridgeBase.getMemberOfTeam("blue").getUniqueId().toString()+"|"+ Maps.humanReadableMapName(bridgeBase.getMap()));
    }
}
