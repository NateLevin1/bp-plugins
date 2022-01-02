package net.bridgepractice.bpbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class Emotes extends Command {
    public Emotes() {
        super("Emotes", null, "emojis");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(
                new ComponentBuilder("\nAvailable Emotes:").color(ChatColor.YELLOW).bold(true).append("\n - ").color(ChatColor.GRAY).bold(false)
                        .append(":eyes:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§f⌊●_●⌋").append("\n - ").color(ChatColor.GRAY)
                        .append(":snowman:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§b☃§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":star:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§e★§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":smile:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§e☻§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":frown:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§9☹§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":x:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§c✕§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":cross:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§c✕§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":y:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§a✔§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":check:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§a✔§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":tm:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§b™§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":cry:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§9（>﹏<）§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":why:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§6(｢•-•)｢ why?§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":sunglasses:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§68§e)§f").append("\n - ").color(ChatColor.GRAY)
                        .append(":sunglasses2:").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§6B§e)§f").append("\n - ").color(ChatColor.GRAY)
                        .append("<3").color(ChatColor.WHITE).append(" ➡ ").color(ChatColor.DARK_GREEN)
                        .append("§c❤§f")
                        .create());
    }
}
