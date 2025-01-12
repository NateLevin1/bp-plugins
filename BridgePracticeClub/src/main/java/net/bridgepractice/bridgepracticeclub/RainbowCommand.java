package net.bridgepractice.bridgepracticeclub;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class RainbowCommand implements CommandExecutor {
    HashMap<UUID, Long> startTimes = new HashMap<>();
    String[] colors = {"c", "6", "e", "a", "b", "9", "d"};
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = ((Player) sender);
        if(System.currentTimeMillis() - startTimes.getOrDefault(player.getUniqueId(), 0L) < 5*60*1000) {
            player.sendMessage("§cYou must wait 5 minutes between each use!");
            return true;
        }
        startTimes.put(player.getUniqueId(), System.currentTimeMillis());
        (new BukkitRunnable() {
            @Override
            public void run() {
                startTimes.remove(player.getUniqueId());
            }
        }).runTaskLater(Bridge.instance, 5*60*20);
        StringBuilder message = new StringBuilder();
        int i = 0;
        for(String letter : String.join(" ", args).split("")) {
            message.append("§").append(colors[i]).append(letter);
            i++;
            if(i == colors.length) {
                i = 0;
            }
        }
        player.chat(message.toString());
        return true;
    }
}
