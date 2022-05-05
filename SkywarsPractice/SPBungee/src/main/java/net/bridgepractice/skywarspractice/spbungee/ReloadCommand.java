package net.bridgepractice.skywarspractice.spbungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("ReloadBlockedCommands", "bridgepractice.moderation.chat");
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        SPBungee.instance.getLogger().info(sender.getName() + " requested to reload config!");
        sender.sendMessage(new TextComponent("§p§k|||§r §b[§2S§3P§b] §dAttempting to reload configuration..."));
        try {
            SPBungee.cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(SPBungee.instance.getDataFolder(), "config.yml"));
            sender.sendMessage(new TextComponent("§p§k|||§r §b[§2S§3P§b] §dDone!"));
        } catch (IOException e) {
            e.printStackTrace();
            sender.sendMessage(new TextComponent("§p§k|||§r §b[§2S§3P§b] §dFailed! Check console for details."));
        }
    }
}
