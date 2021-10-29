package net.bridgepractice.bpbridge;

import org.bukkit.World;

public class JoiningPlayer {
    private String mapName;
    private final World worldToJoin;
    private final String worldName;
    private String gameType = null;
    private boolean isJoiningGame = false;
    private boolean isJoiningPrivateGame = false;

    public JoiningPlayer(String mapName, World worldToJoin, String gameType) {
        this.mapName = mapName;
        this.worldToJoin = worldToJoin;
        this.worldName = worldToJoin.getName();
        this.gameType = gameType;
    }
    public JoiningPlayer(String worldName) {
        this.worldName = worldName;
        this.worldToJoin = BPBridge.instance.getServer().getWorld(worldName);
        this.isJoiningGame = true;
    }
    public static JoiningPlayer newWithPrivate(String mapName, World worldToJoin, String gameType) {
        JoiningPlayer joiningPlayer = new JoiningPlayer(mapName, worldToJoin, gameType);
        joiningPlayer.isJoiningPrivateGame = true;
        return joiningPlayer;
    }
    public String getMapName() {
        return mapName;
    }
    public World getWorld() {
        return worldToJoin;
    }
    public String getWorldName() {
        return worldName;
    }
    public boolean isJoiningPrivateGame() {
        return isJoiningPrivateGame;
    }
    public boolean isJoiningGame() {
        return isJoiningGame;
    }
    public String getGameType() {
        return gameType;
    }
}
