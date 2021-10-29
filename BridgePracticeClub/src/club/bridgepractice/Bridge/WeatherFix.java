package club.bridgepractice.Bridge;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class WeatherFix extends BukkitRunnable {
    @Override
    public void run() {
        World world = Bridge.instance.world;
        world.setTime(1000L);
        world.setStorm(false);
    }
}
