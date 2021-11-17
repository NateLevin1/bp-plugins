package net.bridgepractice.bridgepracticeclub;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ArrowRegenerate extends BukkitRunnable {
    Player player;
    long startTime;
    float regenTime;
    ArrowRegenerate(Player p) {
        player = p;
        startTime = System.currentTimeMillis();
        regenTime = Bridge.instance.getPlayer(player.getUniqueId()).location == PlayerLocation.Prebow ? 0.5f : 3.5f;
        Bridge.instance.playerArrowRegenerations.put(player.getUniqueId(), this);
    }
    @Override
    public void run() {
        if(!player.isOnline()) {
            this.cancel();
            return;
        }

        float timeSince = (System.currentTimeMillis() - startTime) / 1000f;
        if(timeSince > regenTime) {
            Bridge.giveArrow(player);
            this.cancel();
            Bridge.instance.playerArrowRegenerations.put(player.getUniqueId(), null);
        }
        player.setTotalExperience((int) timeSince);
        player.setExp(timeSince / regenTime);
        player.setLevel((int) (regenTime - timeSince));
    }
}
