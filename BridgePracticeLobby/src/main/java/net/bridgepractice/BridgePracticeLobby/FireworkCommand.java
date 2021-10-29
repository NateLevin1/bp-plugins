package net.bridgepractice.BridgePracticeLobby;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FireworkCommand implements CommandExecutor {
    HashMap<UUID, Long> startTimes = new HashMap<>();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = ((Player) sender);
        if(System.currentTimeMillis() - startTimes.getOrDefault(player.getUniqueId(), 0L) < 15*1000) {
            player.sendMessage("§cYou must wait 15 seconds between launching fireworks!");
            return true;
        }
        startTimes.put(player.getUniqueId(), System.currentTimeMillis());
        (new BukkitRunnable() {
            @Override
            public void run() {
                startTimes.remove(player.getUniqueId());
            }
        }).runTaskLater(BridgePracticeLobby.instance, 15*20);

        player.sendMessage("§eLaunched a firework!");
        // actually launch the firework
        Firework fw = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.setPower(2);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.RED).build());
        fw.setFireworkMeta(fwm);
        (new BukkitRunnable() {
            @Override
            public void run() {
                fw.detonate();
            }
        }).runTaskLater(BridgePracticeLobby.instance, 40);
        return true;
    }
}
