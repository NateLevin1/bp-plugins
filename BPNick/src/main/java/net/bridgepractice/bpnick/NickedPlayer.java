package net.bridgepractice.bpnick;

public class NickedPlayer {
    private final String originalName;
    private final String nickName;
    public NickedPlayer(String originalName, String nickName) {
        this.originalName = originalName;
        this.nickName = nickName;
    }
    public String getOriginalName() {
        return originalName;
    }
    public String getNickName() {
        return nickName;
    }
}
