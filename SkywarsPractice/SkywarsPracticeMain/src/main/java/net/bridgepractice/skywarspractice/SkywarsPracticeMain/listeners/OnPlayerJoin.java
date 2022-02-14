package net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners;

import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Main;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.Utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OnPlayerJoin implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);

        e.setJoinMessage("§7[§a+§7] " + player.getDisplayName() + "§7 joined the server!");

        Utils.sendPlayerToSpawn(player);

        PlayerInventory pli = player.getInventory();
        pli.clear();

        pli.setHelmet(new ItemStack(Material.AIR));
        pli.setChestplate(new ItemStack(Material.AIR));
        pli.setLeggings(new ItemStack(Material.AIR));
        pli.setBoots(new ItemStack(Material.AIR));

        // if they don't have a db row then create it
        try(PreparedStatement statement = Main.connection.prepareStatement("INSERT IGNORE INTO skywarsPlayers(uuid, firstLogin) VALUES (?, ?);"); /* insert if doesn't exist */) {
            statement.setString(1, player.getUniqueId().toString()); // uuid, set to player uuid
            statement.setDate(2, new Date(new java.util.Date().getTime())); // firstLogin, set to today's date
            statement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
