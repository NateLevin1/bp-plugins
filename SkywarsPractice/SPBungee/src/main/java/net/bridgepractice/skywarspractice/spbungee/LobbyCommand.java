package net.bridgepractice.skywarspractice.spbungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class LobbyCommand extends Command {
    public LobbyCommand() {
        super("Lobby", "bridgepractice.moderation.chat", "l", "spawn", "hub");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) return;

        ProxiedPlayer player = (ProxiedPlayer) sender;
        player.chat("/lq");
    }
}
