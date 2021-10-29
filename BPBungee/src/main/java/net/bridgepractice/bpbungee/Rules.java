package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;

public class Rules extends Command {
    public Rules() {
        super("Rules");
    }
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(new ComponentBuilder("\nBridgePractice Rules").color(ChatColor.YELLOW).bold(true).create());
        sender.sendMessage(new ComponentBuilder("You will be banned immediately without warning for any of the following:").color(ChatColor.GOLD).italic(true).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Cheating").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Using ").color(ChatColor.GRAY).create()).append(new ComponentBuilder("disallowed modifications").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bView disallowed modifications"))).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://hypixel.net/threads/guide-allowed-modifications.345453/")).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Repeatedly spamming chat with advertisements or offensive language").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Alting to avoid bans").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Purposely altering your ping").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Boosting, i.e. queueing games with an alt").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder("You will be warned, then muted/banned for any of the following:").color(ChatColor.GOLD).italic(true).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Using offensive language such as but not limited to racism, derogatory slurs and hate speech in chat").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Being overly toxic in chat").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Advertising in chat").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder(" • ").color(ChatColor.RED).append(new ComponentBuilder("Abusing ghost blocks").color(ChatColor.GRAY).create()).create());
        sender.sendMessage(new ComponentBuilder("\nBans and mutes are subject to moderator's opinions.").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("When in doubt, use common sense").color(ChatColor.GOLD).bold(true).create());
    }
}
