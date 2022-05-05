package net.bridgepractice.skywarspractice.spbungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.List;

public class StrikeCommand extends Command {
    public StrikeCommand() { super("Strike", "bridgepractice.moderation.chat", "strikes"); }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage(new TextComponent("§cUsage: /strike <add/remove/info> <player>"));
            return;
        }
        String mode = args[0];
        ProxiedPlayer player = SPBungee.instance.getProxy().getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(new TextComponent("§cThat player does not exist!"));
            return;
        }
        switch (mode) {
            case "add":
                if (SPBungee.chatStrikes.containsKey(player.getUniqueId())) {
                    SPBungee.chatStrikes.put(player.getUniqueId(), SPBungee.chatStrikes.get(player.getUniqueId()) + 1);
                    List<String> texts = SPBungee.chatStrikesText.get(player.getUniqueId());
                    texts.add("Strike added by staff member.");
                    SPBungee.chatStrikesText.put(player.getUniqueId(), texts);
                } else {
                    SPBungee.chatStrikes.put(player.getUniqueId(), 1);
                    List<String> texts = new ArrayList<>();
                    texts.add("Strike added by staff member.");
                    SPBungee.chatStrikesText.put(player.getUniqueId(), texts);
                }
                sender.sendMessage(new TextComponent("§6Successfully added strike to " + player.getName()));
                break;
            case "remove":
                if (SPBungee.chatStrikes.containsKey(player.getUniqueId())) {
                    SPBungee.chatStrikes.put(player.getUniqueId(), SPBungee.chatStrikes.get(player.getUniqueId()) - 1);
                    List<String> texts = SPBungee.chatStrikesText.get(player.getUniqueId());
                    texts.remove(texts.size() - 1);
                    SPBungee.chatStrikesText.put(player.getUniqueId(), texts);
                    if (SPBungee.chatStrikes.get(player.getUniqueId()) == 0) {
                        SPBungee.chatStrikes.remove(player.getUniqueId());
                        SPBungee.chatStrikesText.remove(player.getUniqueId());
                    }
                }
                sender.sendMessage(new TextComponent("§6Successfully remove last strike from " + player.getName()));
                break;
            case "info":
                if (SPBungee.chatStrikes.containsKey(player.getUniqueId())) {
                    List<String> texts = SPBungee.chatStrikesText.get(player.getUniqueId());
                    String result = "";
                    int currentPlace = 1;

                    for (String text : texts) {
                        result = result + "§6Strike "+currentPlace+"§7: §2" + text + "\n";
                        currentPlace++;
                    }

                    sender.sendMessage(new TextComponent("§5§l"+player.getName()+"'s§r§5 Strikes§5:\n" +
                            result));
                } else {
                    sender.sendMessage(new TextComponent("§5§l"+player.getName()+"'s§r§5 Strikes§5:\n" +
                            "§6No information found for this player!"));
                }
                break;
        }
        if (SPBungee.chatStrikes.containsKey(player.getUniqueId())) {
            if (SPBungee.chatStrikes.get(player.getUniqueId()) == 3) {
                // Mute player for 3 days
                SPBungee.chatStrikes.remove(player.getUniqueId());
                SPBungee.instance.getProxy().getPluginManager().dispatchCommand(SPBungee.instance.getProxy().getConsole(), "mute "+player.getName()+" 3");
                List<String> texts = SPBungee.chatStrikesText.get(player.getUniqueId());
                Utils.broadcastToPermission("bridgepractice.moderation.chat", new TextComponent("§5§l"+player.getName()+"§r §5automatically §cmuted§5 by §dCONSOLE§5:\n" +
                        "§6Strike 1§7: §2" + texts.get(0) + "\n" +
                        "§6Strike 2§7: §2" + texts.get(1) + "\n" +
                        "§6Strike 3§7: §2" + texts.get(2)));
                SPBungee.chatStrikesText.remove(player.getUniqueId());
            }
        }
    }
}
