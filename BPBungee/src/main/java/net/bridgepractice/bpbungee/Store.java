package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;

public class Store extends Command {
    public Store() {
        super("Store");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(new ComponentBuilder("\nClick to open the store:").color(ChatColor.GREEN).create());
        sender.sendMessage(new ComponentBuilder("https://store.bridgepractice.net/").color(ChatColor.AQUA).underlined(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://store.bridgepractice.net/")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to open the store!"))).create());
    }
}
