package net.bridgepractice.BridgePracticeLobby;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinAnnounceCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = ((Player) sender);
        if (!(player.hasPermission("bridgepractice.lobby.announce"))) return true;
        LuckPerms luckPerms = BridgePracticeLobby.luckperms;
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        BridgePracticeLobby.Gadget current = BridgePracticeLobby.instance.getGadget(player);
        if (args.length == 0 || args[0] == null) {
            if (player.hasPermission("bridgepractice.lobby.announce.hide")) {
                showJoinAnnouncement(user);
                player.sendMessage(ChatColor.AQUA + "Join announcements turned on!");
            } else {
                hideJoinAnnouncement(user);
                player.sendMessage(ChatColor.AQUA + "Join announcements turned off!");
            }
        } else {
            if (args[0].equals("on")) {
                if (player.hasPermission("bridgepractice.lobby.announce.hide")) {
                    showJoinAnnouncement(user);
                    player.sendMessage(ChatColor.AQUA + "Join announcements turned on!");
                } else {
                    player.sendMessage(ChatColor.RED + "Join announcements are already turned on!");
                }
            } else if (args[0].equals("off")) {
                if (player.hasPermission("bridgepractice.announce.show")) {
                    hideJoinAnnouncement(user);
                    player.sendMessage(ChatColor.AQUA + "Join announcements turned off!");
                } else {
                    player.sendMessage(ChatColor.RED + "Join announcements are already turned off!");
                }
            }
        }
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    private void showJoinAnnouncement(User user) {
        DataMutateResult showNode = user.data().add(Node.builder("bridgepractice.lobby.announce.show")
                .value(true)
                .build());
        DataMutateResult hideNode = user.data().add(Node.builder("bridgepractice.lobby.announce.hide")
                .value(false)
                .build());
    }

    private void hideJoinAnnouncement(User user) {
        DataMutateResult showNode = user.data().add(Node.builder("bridgepractice.lobby.announce.show")
                .value(false)
                .build());
        DataMutateResult hideNode = user.data().add(Node.builder("bridgepractice.lobby.announce.hide")
                .value(true)
                .build());
    }
}
