package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Verify extends Command {
    public Verify() {
        super("Verify", null, "verifydiscord");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) return;
        if (args.length != 1) {
            sender.sendMessage(new ComponentBuilder("You must provide a code to link your Discord account. If you do not have a code, join the Discord (using /discord) and do /link in #bot-commands!").color(ChatColor.RED).create());
            return;
        }

        int code;
        try {
            code = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            sender.sendMessage(new ComponentBuilder("The code you provided doesn't seem to be a number!").color(ChatColor.RED).create());
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        boolean valid = Utils.verifyDiscordCode(code, player);
        if (valid) {
            sender.sendMessage(new ComponentBuilder("Successfully linked to your Discord account!").color(ChatColor.GREEN).create());
        } else {
            sender.sendMessage(new ComponentBuilder("You provided an invalid code!").color(ChatColor.RED).create());
        }
    }
}
