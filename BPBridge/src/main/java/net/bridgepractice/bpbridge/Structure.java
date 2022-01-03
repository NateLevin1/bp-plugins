package net.bridgepractice.bpbridge;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;


public class Structure {
    int width;  // x
    int height; // y
    int length; // z
    BlockState[][][] content;
    public Location previousLocation;
    boolean isFlipped = false;
    boolean hasFlipChanged = false;
    public Structure(BlockState[][][] content) {
        width  = content[0][0].length;
        length = content[0].length;
        height = content.length;

        this.content = content;
    }
    public void place(Location loc) {
        if(previousLocation != loc || hasFlipChanged) {
            previousLocation = loc;
            hasFlipChanged = false;
            World world = loc.getWorld();
            int locX = loc.getBlockX(); // 454 with no opts
            int locY = loc.getBlockY();
            int locZ = loc.getBlockZ();
            for(int y = 0; y < height; y++) {
                for(int z = 0; z < length; z++) {
                    for(int x = 0; x < width; x++) {
                        BlockState state = content[y][z][x];
                        setBlockInNativeWorld(world, locX+x, locY+y, locZ+z, state.getTypeId(), state.getRawData());
                    }
                }
            }
        }
    }
    public void batchedPlace(Location loc, Structure toBatch, Location loc2) {
        if(previousLocation != loc || hasFlipChanged) {
            previousLocation = loc;
            hasFlipChanged = false;
            toBatch.hasFlipChanged = false;
            toBatch.previousLocation = loc2;
            World world = loc.getWorld();
            int locX = loc.getBlockX(); // 454 with no opts
            int locY = loc.getBlockY();
            int locZ = loc.getBlockZ();
            int locX2 = loc2.getBlockX();
            int locY2 = loc2.getBlockY();
            int locZ2 = loc2.getBlockZ();
            for(int y = 0; y < height; y++) {
                for(int z = 0; z < length; z++) {
                    for(int x = 0; x < width; x++) {
                        BlockState state = content[y][z][x];
                        BlockState state2 = toBatch.content[y][z][x];
                        if(state.getTypeId() != 0) {
                            setBlockInNativeWorld(world, locX+x, locY+y, locZ+z, state.getTypeId(), state.getRawData());
                        }
                        if(state2.getTypeId() != 0) {
                            setBlockInNativeWorld(world, locX2+x, locY2+y, locZ2+z, state2.getTypeId(), state2.getRawData());
                        }
                    }
                }
            }
        }
    }
    public void placeIfContentNotAir(Location loc) {
        if(previousLocation != loc || hasFlipChanged) {
            previousLocation = loc;
            hasFlipChanged = false;
            for(int y = 0; y < height; y++) {
                for(int z = 0; z < length; z++) {
                    for(int x = 0; x < width; x++) {
                        Block block = loc.clone().add(x, y, z).getBlock();
                        if(content[y][z][x].getType() != Material.AIR) {
                            block.setType(content[y][z][x].getType());
                            block.setData(content[y][z][x].getRawData());
                        }
                    }
                }
            }
        }
    }
    public void remove() {
        if(previousLocation != null) {
            World world = previousLocation.getWorld();
            for(int y = 0; y < height; y++) {
                for(int z = 0; z < length; z++) {
                    for(int x = 0; x < width; x++) {
                        setBlockInNativeWorld(world, previousLocation.getBlockX()+x, previousLocation.getBlockY()+y, previousLocation.getBlockZ()+z, 0, ((byte) 0));
                    }
                }
            }
            previousLocation = null;
        }
    }
    public void removeIfContentNotAir() {
        if(previousLocation != null) {
            for(int y = 0; y < height; y++) {
                for(int z = 0; z < length; z++) {
                    for(int x = 0; x < width; x++) {
                        if(content[y][z][x].getType() != Material.AIR)
                            previousLocation.clone().add(x, y, z).getBlock().setType(Material.AIR);
                    }
                }
            }
            previousLocation = null;
        }
    }
    public void flipX() {
        isFlipped = !isFlipped;
        hasFlipChanged = true;
        for(int y = 0; y < height; y++) {
            for(int z = 0; z < length; z++) {
                for(int x = 0; x < width/2; x++) {
                    BlockState temp = content[y][z][x];
                    content[y][z][x] = content[y][z][width - 1 - x];
                    content[y][z][width - 1 - x] = temp;
                }
            }
        }
    }
    public void switchContent(BlockState[][][] content) {
        isFlipped = false;
        hasFlipChanged = false;
        width  = content[0][0].length;
        length = content[0].length;
        height = content.length;

        this.content = content;
    }
    public void removeAndSwitchContentTo(BlockState[][][] content) {
        this.remove();
        this.switchContent(content);
    }
    public static void setBlockInNativeWorld(World world, int x, int y, int z, int blockType, byte data) {
        // see https://www.spigotmc.org/threads/methods-for-changing-massive-amount-of-blocks-up-to-14m-blocks-s.395868/
        net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) world).getHandle();
        BlockPosition bp = new BlockPosition(x, y, z);
        IBlockData ibd = net.minecraft.server.v1_8_R3.Block.getByCombinedId(blockType + (data << 12));
        nmsWorld.setTypeAndData(bp, ibd, 2);
    }
}