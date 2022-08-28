package net.bridgepractice.bpbungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Lobby extends Command {
    public Lobby() {
        super("Lobby", null, "l", "spawn");
    }
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("This command can only be run by a player!").color(ChatColor.RED).create());
            return;
        }
        ProxiedPlayer p = (ProxiedPlayer) sender;
        if(args.length == 0) {
            if(p.getServer().getInfo().getName().equals("singleplayer")) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Commands"); // the channel could be whatever you want
                out.writeUTF("lobby"); // this data could be whatever you want
                out.writeUTF(p.getUniqueId().toString());

                p.getServer().getInfo().sendData("bp:messages", out.toByteArray());
            } else {
                p.connect(ProxyServer.getInstance().getServerInfo("lobby"));
            }
        } else if (args[0] == null) {
            if(p.getServer().getInfo().getName().equals("singleplayer")) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Commands"); // the channel could be whatever you want
                out.writeUTF("lobby"); // this data could be whatever you want
                out.writeUTF(p.getUniqueId().toString());

                p.getServer().getInfo().sendData("bp:messages", out.toByteArray());
            } else {
                p.connect(ProxyServer.getInstance().getServerInfo("lobby"));
            }
        } else {
            String lobbyName = args[0];
            String serverName;
            switch(lobbyName) {
                case "":
                case "hub":
                    serverName = "lobby";
                    break;
                case "singleplayer":
                    serverName = "singleplayer";
                    break;
                case "multiplayer":
                    serverName = "multiplayer_lobby";
                    break;
                default:
                    p.sendMessage(new ComponentBuilder("Unknown lobby '" + lobbyName + "'").color(ChatColor.RED).create());
                    return;
            }
            ServerInfo server = ProxyServer.getInstance().getServerInfo(serverName);
            if(server == null) {
                p.sendMessage(new ComponentBuilder("That server is not available right now!").color(ChatColor.RED).create());
                return;
            }
            p.connect(server);
        }
    }
}