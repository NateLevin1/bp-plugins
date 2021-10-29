package net.bridgepractice.bpnick.commands;

import net.bridgepractice.bpnick.NickManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Nick extends Command {
    public Nick() {
        super("Nick", "bridgepractice.command.nick");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        String nick = "Cheetahh";
        NickManager.addNickedPlayer(((ProxiedPlayer) sender), nick);
        sender.sendMessage("Successfully nicked as "+nick+"!");
    }
}
