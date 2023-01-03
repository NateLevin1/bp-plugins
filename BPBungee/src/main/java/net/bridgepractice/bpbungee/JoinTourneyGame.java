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

import java.util.concurrent.TimeUnit;

public class JoinTourneyGame extends Command {
    public JoinTourneyGame() { super("JoinTourneyGame", null, "jointourney"); }


    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) return;
        joinTourneyGame((ProxiedPlayer) sender);
    }

    public static void joinTourneyGame(ProxiedPlayer player) {
        if (!BPBungee.isTourneyRunning) {
            player.sendMessage(new ComponentBuilder("No tourney is running right now! Check the Discord for upcoming tourneys!").color(ChatColor.RED).create());
            return;
        }
        if (BPBungee.isTourneyGameRunning) {
            player.sendMessage(new ComponentBuilder("A tourney game is already running! Check the Discord for the live score!").color(ChatColor.RED).create());
            return;
        }

        if (!BPBungee.validTourneyPlayers.contains(player.getUniqueId())) {
            player.sendMessage(new ComponentBuilder("You are not in the tourney! Join the Discord to be in the next one!").color(ChatColor.RED).create());
            return;
        }
        if (!BPBungee.tourneyPlayersNotPlayedYet.contains(player.getUniqueId())) {
            player.sendMessage(new ComponentBuilder("You have already played the right amount of games for this session. Please wait until everyone else has to join the next game.").color(ChatColor.RED).create());
            return;
        }
        if (BPBungee.playersInGame.contains(player.getUniqueId())) {
            player.sendMessage(new ComponentBuilder("We are already finding you a match! Please sit tight!").color(ChatColor.RED).create());
            return;
        }

        // Now we know it's a completely valid player

        BPBungee.playersInGame.add(player.getUniqueId());
        player.sendMessage(new ComponentBuilder("Finding match... (This may take a while)").color(ChatColor.GREEN).create());

        if (BPBungee.playersInGame.size() == 2) {
            ProxiedPlayer p1 = BPBungee.instance.getProxy().getPlayer(BPBungee.playersInGame.get(0));
            // p2 == player

            BPBungee.tourneyPlayersNotPlayedYet.remove(p1.getUniqueId());
            BPBungee.tourneyPlayersNotPlayedYet.remove(player.getUniqueId());

            Utils.addGamePlayed(p1);
            Utils.addGamePlayed(player);

            startTourneyGame(p1, player);
        }
    }

    public static void startTourneyGame(ProxiedPlayer p1, ProxiedPlayer p2) {
        BPBungee.isTourneyGameRunning = true;
        ServerInfo multiplayerServer = ProxyServer.getInstance().getServerInfo("multiplayer_1");
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("StartPrivateGame");
        out.writeUTF("tourney");
        out.writeUTF("");
        out.writeInt(2); // num of players, only ever 2 for now
        out.writeUTF(p1.getName());
        out.writeUTF(p2.getName());

        Utils.addGamePlayed(p1);
        Utils.addGamePlayed(p2);

        if(multiplayerServer.getPlayers().size() == 0) {
            // if there are no players online we need to connect them so we can send a message
            p1.connect(ProxyServer.getInstance().getServerInfo("multiplayer_1"));
            p2.connect(ProxyServer.getInstance().getServerInfo("multiplayer_1"));
            BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> p1.getServer().getInfo().sendData("bp:messages", out.toByteArray()), 1000, TimeUnit.MILLISECONDS);
        } else {
            multiplayerServer.sendData("bp:messages", out.toByteArray());
        }
    }
}
