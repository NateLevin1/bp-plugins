package net.bridgepractice.bridgepracticeclub;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class ResetBridgePlayer extends BukkitRunnable {
    PlayerInfo info;
    Player player;
    Location respawnLocation;
    boolean settingsInsteadOfGlyph;
    boolean setToSurvival = false;
    public boolean destroyBlocks = true;
    ResetBridgePlayer(Player player, PlayerInfo info, boolean settingsInsteadOfGlyph) {
        this.info = info;
        this.respawnLocation = info.respawnLocation;
        this.player = player;
        this.settingsInsteadOfGlyph = settingsInsteadOfGlyph;
    }
    ResetBridgePlayer(Player player, PlayerInfo info, boolean settingsInsteadOfGlyph, boolean setToSurvival) {
        this(player, info, settingsInsteadOfGlyph);
        this.setToSurvival = setToSurvival;
    }
    @Override
    public void run() {
        if(Bridge.instance.getPlayer(player.getUniqueId()).location != info.location) {
            // the player probably went to lobby - don't respawn them
            return;
        }
        if(setToSurvival) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        player.teleport(respawnLocation);
        player.getInventory().clear();
        player.setExp(0);
        player.setLevel(0);
        player.setHealth(20);
        Bridge.setBridgeInventory(player, settingsInsteadOfGlyph);
        if (Bridge.instance.playerArrowRegenerations.get(player.getUniqueId()) != null) {
            Bridge.instance.playerArrowRegenerations.get(player.getUniqueId()).cancel();
        }
        // clear potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // delete changed blocks
        if(destroyBlocks) {
            for(Location loc : info.changedBlocks) {
                Block block = loc.getBlock();
                if(block.getType() == Material.STAINED_CLAY) // don't destroy blocks part of Structures
                    block.setType(Material.AIR);
            }
            info.changedBlocks.clear();
        }
    }
}
