package net.bridgepractice.skywarspractice.spbungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.concurrent.TimeUnit;

public class StaffWhitelistCommand extends Command {
    public StaffWhitelistCommand() { super("StaffWhitelist", "group.admin", "swhitelist", "staffw"); }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(new TextComponent("§cUsage: /staffwhitelist <enable/disable>"));
            return;
        }

        switch (args[0]) {
            case "enable":
                SPBungee.staffWhitelistEnabled = true;
                Utils.log(sender.getName()+" has enabled the whitelist!");
                SPBungee.setQueuing(false);
                SPBungee.instance.getProxy().broadcast(new TextComponent("§4[ALERT] §7Scheduled maintenance soon. Queuing has been disabled."));
                SPBungee.instance.getProxy().getScheduler().schedule(SPBungee.instance, new Runnable() {
                    @Override
                    public void run() {
                        SPBungee.instance.getProxy().broadcast(new TextComponent("§4[ALERT] §7Maintenance commencing..."));
                        kickAllWithoutPermision("group.helper");
                    }
                }, 3, TimeUnit.MINUTES);
                break;
            case "disable":
                SPBungee.staffWhitelistEnabled = false;
                SPBungee.setQueuing(true);
                Utils.log(sender.getName()+" has disabled the whitelist!");
                break;
            default:
                sender.sendMessage(new TextComponent("§cUsage: /whitelist <enable/disable>"));
                break;
        }
    }

    private static void kickAllWithoutPermision(String permission) {
        for(ProxiedPlayer player : SPBungee.instance.getProxy().getPlayers()) {
            if(!player.hasPermission(permission)) {
                player.disconnect(new TextComponent("§cSkywars Practice has gone under maintenance! Please check back later!"));
            }
        }
    }
}
