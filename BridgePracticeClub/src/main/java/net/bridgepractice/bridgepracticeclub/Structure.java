package net.bridgepractice.bridgepracticeclub;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;


public class Structure {
    int width;  // x
    int height; // y
    int length; // z
    BlockState[][][] content;
    public Location previousLocation;
    boolean isFlipped = false;
    boolean hasFlipChanged = false;
    Structure(BlockState[][][] content) {
        width  = content[0][0].length;
        length = content[0].length;
        height = content.length;

        this.content = content;
    }
    private void placeUnchecked(Location loc) {
        previousLocation = loc;
        hasFlipChanged = false;
        for(int y = 0; y < height; y++) {
            for(int z = 0; z < length; z++) {
                for(int x = 0; x < width; x++) {
                    Block block = loc.clone().add(x, y, z).getBlock();
                    block.setType(content[y][z][x].getType());
                    block.setData(content[y][z][x].getRawData());
                }
            }
        }
    }
    public void place(Location loc) {
        if(previousLocation != loc || hasFlipChanged) {
            placeUnchecked(loc);
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
            for(int y = 0; y < height; y++) {
                for(int z = 0; z < length; z++) {
                    for(int x = 0; x < width; x++) {
                        previousLocation.clone().add(x, y, z).getBlock().setType(Material.AIR);
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
    public void placeAtPreviousLocation() {
        this.placeUnchecked(this.previousLocation);
    }
}
