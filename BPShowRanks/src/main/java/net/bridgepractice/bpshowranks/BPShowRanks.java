package net.bridgepractice.bpshowranks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BPShowRanks extends JavaPlugin implements Listener {
    public static LuckPerms luckPerms;
    @Override
    public void onEnable() {
        luckPerms = LuckPermsProvider.get();

        getServer().getPluginManager().registerEvents(this, this);
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        User luckPermsUser = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        String rankedName = luckPermsUser.getCachedData().getMetaData().getPrefix() + player.getName();
        player.setDisplayName(rankedName);
        player.setPlayerListName(rankedName);
        player.setCustomName(rankedName);
    }
}
