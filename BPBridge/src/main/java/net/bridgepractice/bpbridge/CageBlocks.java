package net.bridgepractice.bpbridge;

import org.bukkit.block.BlockState;

public class CageBlocks {
    private final BlockState[][][] blue;
    private final BlockState[][][] red;
    CageBlocks(BlockState[][][] blue, BlockState[][][] red) {
        this.blue = blue;
        this.red = red;
    }

    public BlockState[][][] getBlue() {
        return blue;
    }

    public BlockState[][][] getRed() {
        return red;
    }
}
