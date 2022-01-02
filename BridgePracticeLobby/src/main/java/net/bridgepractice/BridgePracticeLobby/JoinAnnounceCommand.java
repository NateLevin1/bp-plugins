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
    LuckPerms luckPerms = BridgePracticeLobby.luckperms;
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!(sender instanceof Player)) return true;
        Player player = ((Player) sender);
        if (!(player.hasPermission("bridgepractice.lobby.announce"))) return true;
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);

        if (args.length == 0) {
            // toggle
            if (player.hasPermission("bridgepractice.lobby.announce.show")) {
                setJoinAnnouncement(user, false);
                player.sendMessage(ChatColor.AQUA + "Join announcements turned off!");
            } else {
                setJoinAnnouncement(user, true);
                player.sendMessage(ChatColor.AQUA + "Join announcements turned on!");
            }
        } else {
            if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("enable")) {
                setJoinAnnouncement(user, true);
                player.sendMessage(ChatColor.AQUA + "Join announcements turned on!");
            } else if (args[0].equals("off") || args[0].equalsIgnoreCase("disable")) {
                setJoinAnnouncement(user, false);
                player.sendMessage(ChatColor.AQUA + "Join announcements turned off!");
            } else {
                player.sendMessage(ChatColor.RED + "Unrecognized option "+args[0]);
            }
        }
        return true;
    }

    private void setJoinAnnouncement(User user, boolean enable) {
        user.data().add(Node.builder("bridgepractice.lobby.announce.show")
                .value(enable)
                .build());
        luckPerms.getUserManager().saveUser(user);
    }
}
