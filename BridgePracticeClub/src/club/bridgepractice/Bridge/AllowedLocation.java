package club.bridgepractice.Bridge;

public class AllowedLocation {
    int xStart, xEnd, zStart, zEnd, yStart, yEnd;
    int[] relXZ;
    AllowedLocation(int[] relXZ, int xStart, int xEnd, int zStart, int zEnd) {
        this.relXZ = relXZ;
        this.zStart = zStart;
        this.zEnd = zEnd;
        this.xStart = xStart;
        this.xEnd = xEnd;
    }
    AllowedLocation(int[] relXZ, int xStart, int xEnd, int zStart, int zEnd, int yStart, int yEnd) {
        this.relXZ = relXZ;
        this.zStart = zStart;
        this.zEnd = zEnd;
        this.xStart = xStart;
        this.xEnd = xEnd;
        this.yStart = yStart;
        this.yEnd = yEnd;
    }
}
