package net.bridgepractice.bridgepracticeclub;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

public class TimeUpdater extends BukkitRunnable {
    long[] time;
    Scoreboard board;
    Player player;
    TimeUpdater(long[] time, Scoreboard board, Player player) {
        this.time = time;
        this.board = board;
        this.player = player;
    }
    @Override
    public void run() {
        if((System.currentTimeMillis()-time[0]) / 1000 > 180) {
            player.sendMessage("§cYou were sent to spawn because you were AFK!");
            player.chat("/spawn");
            this.cancel();
            return;
        }
        board.getTeam("time").setPrefix("§e "+Bridge.prettifyNumber(System.currentTimeMillis()-time[0]));

    }
}
