package net.bridgepractice.skywarspractice.SkywarsPracticeMain;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;

public class Utils {
    public static boolean isPlayerInLocation(Player player, Location pos1, Location pos2) {
        Location loc = player.getLocation();
        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();
        int ax = pos1.getBlockX();
        int ay = pos1.getBlockY();
        int az = pos1.getBlockZ();
        int bx = pos2.getBlockX();
        int by = pos2.getBlockY();
        int bz = pos2.getBlockZ();
        return px >= ax && px <= bx
                && py >= ay && py <= by
                && pz >= az && pz <= bz;
    }

    public static void sendTitle(Player player, String titleText, String subtitleText, int fadeIn, int fadeOut, int duration) {
        IChatBaseComponent chatTitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + titleText + "\"}");
        IChatBaseComponent chatSubtitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitleText + "\"}");

        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, chatTitle);
        PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, chatSubtitle);
        PacketPlayOutTitle length = new PacketPlayOutTitle(fadeIn, duration, fadeOut);

        // For some reason if we don't send this length packet then the first time we try to send a title to a player
        // after they have disconnected the title does not show up.
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);

        // send the actual title
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(title);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(subtitle);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);
    }

    public static void resetBlocks(List<Block> blocksPlaced, List<Block> blocksBroken) {
        for (Block block : blocksPlaced) {
            block.getLocation().getBlock().setType(Material.AIR);
        }
        for (Block block : blocksBroken) {
            block.getLocation().getBlock().setType(block.getType());
        }
    }

    public static void sendPlayerToSpawn(Player player) {
        player.setFallDistance(0F);
        player.setHealth(20);
        player.teleport(Main.spawn);
    }

}
