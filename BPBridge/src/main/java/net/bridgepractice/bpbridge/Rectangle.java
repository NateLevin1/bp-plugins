package net.bridgepractice.bpbridge;

import org.bukkit.Location;

public class Rectangle {
    private final int x;
    private final int y;
    private final int z;
    private final int xWidth;
    private final int yHeight;
    private final int zLength;
    public Rectangle(int x, int yTop, int z, int xWidth, int yHeight, int zLength) {
        this.x = x;
        this.y = yTop;
        this.z = z;
        this.xWidth = xWidth;
        this.yHeight = yHeight;
        this.zLength = zLength;
    }
    public boolean isInBounds(Location loc) {
        return (loc.getBlockX() >= x && loc.getBlockX() <= x + xWidth)
                && (loc.getY() <= y && loc.getY() > y - yHeight)
                && (loc.getBlockZ() >= z && loc.getBlockZ() <= z + zLength);
    }
    public static Rectangle fromMap(int startX, int startZ, int endX, int endZ) {
        return new Rectangle(startX, 99, startZ, endX-startX, 20, endZ-startZ);
    }
    public static Rectangle fromMap(int startX) {
        return new Rectangle(startX, 99, -20, startX*-2, 20, 40);
    }
}
