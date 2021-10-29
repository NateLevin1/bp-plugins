package net.bridgepractice.bpnick.commands;

import net.bridgepractice.bpnick.NickManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Unnick extends Command {
    public Unnick() {
        super("Unnick", "bridgepractice.command.nick");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        NickManager.removeNickedPlayer(((ProxiedPlayer) sender));
    }
}
