package net.bridgepractice.skywarspractice.spbungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SPBungee extends Plugin implements Listener {

    public static Configuration cfg;
    public static Plugin instance;
    public static boolean staffWhitelistEnabled = false;
    private MOTD defaultMotd;
    private MOTD maintenanceMotd;
    public static HashMap<UUID, Integer> chatStrikes = new HashMap<>();
    public static HashMap<UUID, List<String>> chatStrikesText = new HashMap<>();
    private static HashMap<String, Boolean> cachedIpChecks = new HashMap<>();

    public void onEnable() {
        instance = this;
        defaultCfg();

        try {
            cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        File deF = new File(this.getDataFolder() + File.separator + "favicon.png");
        if(!deF.exists()) {
            try {
                InputStream is = getClass().getResourceAsStream("/favicon.png");
                assert is != null;
                BufferedImage as = ImageIO.read(is);
                File outputfile = new File(this.getDataFolder() + File.separator + "favicon.png");
                ImageIO.write(as, "png", outputfile);
            } catch(Exception io) {
                io.printStackTrace();
            }
        }

        Favicon serverIcon = null;
        try {
            serverIcon = Favicon.create(ImageIO.read(deF));
        } catch (IOException e) {
            e.printStackTrace();
        }

        defaultMotd = new MOTD("           §a>§2>§6> §4☢ §2skywars§6practice§2.ga §4☢ §6<§2<§a<§r\n                 §5[NEW] §4§k|||§d Pearl §4Practice §d§k|||§r", serverIcon);
        maintenanceMotd = new MOTD("           §a>§2>§6> §4☢ §2skywars§6practice§2.ga §4☢ §6<§2<§a<§r\n     §c× Under Maintenance. Check back later! ×", serverIcon);

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerListener(this, new ChatFilter());

        ProxyServer.getInstance().getPluginManager().registerCommand(this, new ReloadCommand());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new StrikeCommand());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new StaffWhitelistCommand());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new LobbyCommand());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(ProxyPingEvent e) {
        ServerPing ping = e.getResponse();
        ping.setFavicon(defaultMotd.fav);
        BaseComponent component = new TextComponent(defaultMotd.motd);
        if (staffWhitelistEnabled) {
            component = new TextComponent(maintenanceMotd.motd);
        }
        ping.setDescriptionComponent(component);
        ping.setPlayers(new ServerPing.Players(100, getProxy().getOnlineCount(), new ServerPing.PlayerInfo[]{new ServerPing.PlayerInfo("§2Check out §bBridge§cPractice §2at §6bridgepractice.net§2!", "§2Check out §bBridge§cPractice §2at bridgepractice.net!")}));
        e.setResponse(ping);
    }

    @EventHandler
    public void afterLogin(PostLoginEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (staffWhitelistEnabled && !player.hasPermission("group.helper")) {
            player.disconnect(new TextComponent("§cSkywars Practice is under maintenance! Please check back later!"));
        }
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent e) throws IOException, JSONException {
        String ip = e.getPlayer().getAddress().getHostString();
        boolean allowedToJoin;
        if (cachedIpChecks.containsKey(ip)) {
            allowedToJoin = cachedIpChecks.get(ip);
        } else {
            URL url = new URL("http://ip-api.com/json/"+ip+"?fields=status,proxy,hosting");
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            String resp = http.getResponseMessage();
            http.disconnect();

            JSONObject respJson = new JSONObject(resp);
            if (Objects.equals(respJson.getString("status"), "success")) {
                if (respJson.getBoolean("proxy") || respJson.getBoolean("hosting")) {
                    allowedToJoin = false;
                    cachedIpChecks.put(ip, false);
                    System.out.println(ip + " tried to login but is known to be bad!" +
                            "\nProxy: " + respJson.getBoolean("proxy") +
                            "\nHosting: " + respJson.getBoolean("hosting"));
                } else {
                    allowedToJoin = true;
                    cachedIpChecks.put(ip, true);
                }
                this.getProxy().getScheduler().schedule(this, new Runnable() {
                    @Override
                    public void run() {
                        cachedIpChecks.remove(ip);
                    }
                }, 10, TimeUnit.MINUTES);
            } else {
                allowedToJoin = true;
            }
        }

        System.out.println(ip + ": " + allowedToJoin);

        if (!allowedToJoin) {
            e.getPlayer().disconnect("§cDodgy IP detected. IP: "+ip+"\nIf you have a VPN enabled, disable it and try again.\nIf the issue persists, open a ticket on the Discord.\n\nPlease note, this data is cached for 10 minutes.");
        }
    }

    public void defaultCfg() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void setQueuing(boolean queueingEnabled) {
        Collection<ProxiedPlayer> networkPlayers = ProxyServer.getInstance().getPlayers();
        if ( networkPlayers == null || networkPlayers.isEmpty() ) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF( "SetQueueing" );
        out.writeBoolean( queueingEnabled );

        ProxiedPlayer player = (ProxiedPlayer) networkPlayers.toArray()[0];
        player.getServer().getInfo().sendData( "BungeeCord", out.toByteArray() );
    }
}
