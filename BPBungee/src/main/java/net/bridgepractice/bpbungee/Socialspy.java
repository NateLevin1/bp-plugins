package net.bridgepractice.bpbungee;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Socialspy extends Command {
    LuckPerms luckPerms = BPBungee.luckPerms;
    public Socialspy() {
        super("Socialspy", "bridgepractice.moderation.chat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = ((ProxiedPlayer) sender);
        User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
        if (args.length == 0 || args[0] == null) {
            if (player.hasPermission("bridgepractice.moderation.socialspy")) {
                setPrivateMessages(user, false);
                player.sendMessage(new ComponentBuilder("Socialspy turned off!").color(ChatColor.AQUA).create());
            } else {
                setPrivateMessages(user, true);
                player.sendMessage(new ComponentBuilder("Socialspy turned on!").color(ChatColor.AQUA).create());
            }
        } else {
            if (args[0].equals("on")) {
                if (!(player.hasPermission("bridgepractice.moderation.socialspy"))) {
                    setPrivateMessages(user, true);
                    player.sendMessage(new ComponentBuilder("Socialspy turned on!").color(ChatColor.AQUA).create());
                } else {
                    player.sendMessage(new ComponentBuilder("Socialspy is already turned on!").color(ChatColor.RED).create());
                }
            } else if (args[0].equals("off")) {
                if (player.hasPermission("bridgepractice.moderation.socialspy")) {
                    setPrivateMessages(user, false);
                    player.sendMessage(new ComponentBuilder("Socialspy turned off!").color(ChatColor.AQUA).create());
                } else {
                    player.sendMessage(new ComponentBuilder("Socialspy is already turned off!").color(ChatColor.RED).create());
                }
            }
        }
        luckPerms.getUserManager().saveUser(user);
    }

    private void setPrivateMessages(User user, boolean enable) {
        DataMutateResult showNode = user.data().add(Node.builder("bridgepractice.moderation.socialspy")
                .value(enable)
                .build());
        luckPerms.getUserManager().saveUser(user);
    }
}

