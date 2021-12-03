package net.bridgepractice.bpemergency;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.sql.*;
import java.util.UUID;

public class BPEmergency extends Plugin implements Listener {
    private static final BaseComponent[] message = new ComponentBuilder("Due to hackers attempting to crash the server,\nall players with less than 100 xp have been\nblocked from joining the server.").color(ChatColor.RED).bold(true).append("\n\nPlease try joining again in a few hours, and\njoin our discord for updates\n(go to https://bridgepractice.net/discord).").bold(true).color(ChatColor.YELLOW).append("\n\nWe are extremely sorry for this inconvenience,\nand we are actively trying to solve the issue.").color(ChatColor.WHITE).create();
    public static boolean enabled = false;
    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        openConnection();
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new EmergencyCommand());
    }
    // db related things
    String host = "localhost";
    String port = "3306";
    String database = "bridge";
    String username = "mc";
    String password = "mcserver";
    static Connection connection;

    public void openConnection() {
        try {
            if(connection != null && !connection.isClosed()) {
                return;
            }
            // NOTE: If something around this are fails, something is different between my host machine and the machine
            //       this is running on. Getting rid of the `characterEncoding` query parameter may help, but other
            //       solutions are likely.
            connection = DriverManager.getConnection("jdbc:mysql://"
                            + this.host + ":" + this.port + "/" + this.database + "?characterEncoding=latin1&autoReconnect=true",
                    this.username, this.password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @EventHandler
    public void onLogin(LoginEvent event) {
        if(!enabled) return;
        UUID uuid = event.getConnection().getUniqueId();
        try(PreparedStatement statement = connection.prepareStatement("SELECT xp FROM players WHERE uuid = ?;")) {
            statement.setString(1, uuid.toString());
            ResultSet res = statement.executeQuery();
            if(res.next()) {
                int xp = res.getInt("xp");
                if(xp <= 100) {
                    event.setCancelled(true);
                    event.setCancelReason(message);
                } // else let them play!
            } else {
                event.setCancelled(true);
                event.setCancelReason(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            event.setCancelled(true);
            event.setCancelReason(message);
        }
    }
}