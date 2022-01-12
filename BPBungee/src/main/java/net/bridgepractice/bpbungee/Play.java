package net.bridgepractice.bpbungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;


public class Play extends Command {
    Play() {
        super("Play", null, "q");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("This command can only be run by a player!").color(ChatColor.RED).create());
            return;
        }
        ProxiedPlayer player = ((ProxiedPlayer) sender);
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("You did not provide a mode!").color(ChatColor.RED).bold(true).append(new ComponentBuilder("\nAvailable Modes:").color(ChatColor.AQUA).bold(false).create()).append(new ComponentBuilder("\n - Bridge").color(ChatColor.YELLOW).bold(false).create()).append(new ComponentBuilder("\n - PvP_Duel").color(ChatColor.YELLOW).bold(false).create()).create());
            return;
        }
        switch(args[0].toLowerCase()) {
            case "duels_bridge_duel": // -> This is from Hypixel
            case "unranked":
            case "bridge":
            case "bridge_duel":
            case "duel": {
                BPBungee.instance.requestGame("unranked", player);
                break;
            }
            case "pvpduel":
            case "pvp_duel":
            case "pvp": {
                BPBungee.instance.requestGame("pvp", player);
                break;
            }
            case "ifavailable": {
                if(args.length < 3) return;
                String worldName = args[1];
                MultiplayerMode multiplayerMode = MultiplayerMode.valueOf(args[2]);
                if(BPBungee.instance.isWorldQueueing(worldName, multiplayerMode)) {
                    BPBungee.instance.sendIntentToJoinGame(worldName, multiplayerMode, player);
                } else {
                    player.sendMessage(new ComponentBuilder("That game is unavailable!").color(ChatColor.RED).create());
                }
                break;
            }
            default:
                sender.sendMessage(new ComponentBuilder("Unknown mode \""+args[0]+"\"").color(ChatColor.RED).bold(true).append(new ComponentBuilder("\nAvailable Modes:").color(ChatColor.AQUA).bold(false).create()).append(new ComponentBuilder("\n - Bridge").color(ChatColor.YELLOW).bold(false).create()).append(new ComponentBuilder("\n - PvP_Duel").color(ChatColor.YELLOW).bold(false).create()).create());
                break;
        }
    }
}
