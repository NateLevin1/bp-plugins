package net.bridgepractice.bpshowranksinnametag;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class Main extends JavaPlugin implements Listener {
    public static LuckPerms luckPerms;
    @Override
    public void onEnable() {
        luckPerms = LuckPermsProvider.get();

        getServer().getPluginManager().registerEvents(this, this);
    }
    @EventHandler()
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        User luckPermsUser = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        String prefix = getPrefix(luckPermsUser, player);

        (new BukkitRunnable() {
            @Override
            public void run() {
                List<Player> players = player.getWorld().getPlayers();
                players.add(player);
                Scoreboard psb = player.getScoreboard();
                for(Player p : players) {
                    // show to other players
                    Scoreboard sb = p.getScoreboard();
                    if(sb != null) {
                        addEntry(sb, prefix, player);
                    }

                    // show other players to the joining player
                    User lpu = luckPerms.getPlayerAdapter(Player.class).getUser(p);
                    String pre = getPrefix(lpu, p);
                    addEntry(psb, pre, p);
                }
            }
        }).runTaskLater(this, 5);
    }

    private static void addEntry(Scoreboard sb, String prefix, Player player) {
        Team team = sb.getTeam(prefix);
        if(team == null) {
            team = sb.registerNewTeam(prefix);
            team.setPrefix(prefix);
        }
        team.addEntry(player.getName());
    }


    public static void addAllPlayers(Player player) {
        List<Player> players = player.getWorld().getPlayers();
        players.add(player);
        Scoreboard psb = player.getScoreboard();
        for(Player p : players) {
            // show other players to the joining player
            User lpu = luckPerms.getPlayerAdapter(Player.class).getUser(p);
            String pre = getPrefix(lpu, p);
            addEntry(psb, pre, p);
        }
    }

    private static String getPrefix(User luckPermsUser, Player player) {
        String prefix = luckPermsUser.getCachedData().getMetaData().getPrefix();
        assert prefix != null;
        if(prefix.length() > 16) {
            if(player.hasPermission("group.godlike")) {
                prefix = "§d[GODLIKE] ";
            } else if(player.hasPermission("group.legend")) {
                prefix = "§c[LEGEND] ";
            } else {
                prefix = "§f";
            }
        }
        return prefix;
    }
}
