package net.bridgepractice.bridgepracticeclub;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Scoreboard;

public class CommandSpawn implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo oldInfo = Bridge.instance.getPlayer(player.getUniqueId());
            if(oldInfo != null && oldInfo.location == PlayerLocation.Spawn) {
                player.sendMessage("§cYou are already at spawn!");
                return true;
            }

            Bridge.instance.setPlayer(player.getUniqueId(), new PlayerInfo(PlayerLocation.Spawn, null, null, null, null, null, null, null));

            player.getInventory().clear();
            // clear potion effects
            for(PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setExp(0);
            player.setLevel(0);

            (new BukkitRunnable() {
                @Override
                public void run() {
                    if(Bridge.instance.getPlayer(player.getUniqueId()).location == PlayerLocation.Spawn) {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("GetPlayerPlayingTime");
                        player.sendPluginMessage(Bridge.instance, "BungeeCord", out.toByteArray());
                    }
                }
            }).runTaskLater(Bridge.instance, 3);

            Scoreboard board = Bridge.createScoreboard(" §b§lBridge §c§lPractice ", new String[]{
                    "",
                    "  Players Online: §a" + Bridge.instance.getServer().getOnlinePlayers().size(),
                    "",
                    "  Your XP: §a" + Bridge.getPlayerXP(player),
                    "",
                    "  Playing Time:",
                    "%playing_time%  ",
                    "",
                    "   §ebridgepractice.net  "
            });
            Bridge.setScoreboard(player, board);

            Team npcTeam = board.registerNewTeam("npcs");
            npcTeam.setPrefix("§8[NPC] ");
            for(EntityPlayer npc : Bridge.instance.getAllNpcs()) {
                npcTeam.addEntry(npc.getName());
            }
            npcTeam.setNameTagVisibility(NameTagVisibility.NEVER);

            player.setGameMode(GameMode.ADVENTURE);
            if(Bridge.instance.playerArrowRegenerations.get(player.getUniqueId()) != null) {
                Bridge.instance.playerArrowRegenerations.get(player.getUniqueId()).cancel();
            }
            player.teleport(new Location(player.getWorld(), 0.5, 100, 0.5, 90, 0));
            player.getInventory().setItem(8, Bridge.makeItem(Material.BED, 1, "§cReturn to Main Lobby §7(Right Click)", new String[0],-1));

            Bridge.instance.showPlayerNPCs(player);


            return true;
        } else {
            sender.sendMessage("You must be a player!");
        }
        return false;
    }
}
