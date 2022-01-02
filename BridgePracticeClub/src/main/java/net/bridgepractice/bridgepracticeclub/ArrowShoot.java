package net.bridgepractice.bridgepracticeclub;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ArrowShoot extends BukkitRunnable {
    long[] time;
    Player player;
    ArrowShoot(long[] time, Player player) {
        this.time = time;
        this.player = player;
    }
    @Override
    public void run() {
        if(time[0] != 0) { // make sure the player is bridging
            Location spawnLocation;
            Vector vec;
            PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
            if(info.locSettings.isBridgingLeft) {
                spawnLocation = new Location(Bridge.instance.world, info.winBox.relXZ[0]-4, 101, info.winBox.relXZ[1]+25);
                vec = new Vector(1, spawnLocation.distance(player.getLocation())/7, 0);
            } else {
                spawnLocation = new Location(Bridge.instance.world, info.winBox.relXZ[0]+9, 101, info.winBox.relXZ[1]+25);
                vec = new Vector(-1, spawnLocation.distance(player.getLocation())/7, 0);
            }
            Arrow arrow = Bridge.instance.world.spawnArrow(spawnLocation, player.getLocation().toVector().subtract(spawnLocation.toVector()).add(vec), 3, 0);
            arrow.setMetadata("NO_DAMAGE", new FixedMetadataValue(Bridge.instance, true));
            arrow.setMetadata("INTENDED_FOR", new FixedMetadataValue(Bridge.instance, player.getUniqueId()));
        }
    }
}
