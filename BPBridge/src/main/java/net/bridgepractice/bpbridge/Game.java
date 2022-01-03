package net.bridgepractice.bpbridge;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;

public abstract class Game {
    protected Game(String gameType, World world, String map, boolean shouldCountAsStats) {
        this.gameType = gameType;
        this.world = world;
        this.map = map;
        this.shouldCountAsStats = shouldCountAsStats;
    }


    // # Queries
    public boolean isPlayerInGame(Player player) {
        return allPlayers.contains(player);
    }
    public String getGameType() {
        return gameType;
    }
    public String getMap() {
        return map;
    }
    public boolean isPlaying() {
        return state == State.Playing;
    }
    public boolean hasFinished() {
        return state == State.Finished;
    }
    public boolean isQueueing() {
        return state == State.Queueing;
    }
    public boolean hasQueued() {
        return state != State.Queueing;
    }
    public boolean canPlaceBlocksAtLoc(Location loc) {
        return true;
    }
    public boolean cannotPlaceBlocks(Location loc, Player player) {
        return false;
    }
    public boolean cannotBreakBlock(Block block, Location loc, Player player) {
        return true;
    }
    public boolean canPlayerTakeDamage(Player player) {
        return true;
    }


    // # Events
    // Any subclass MUST implement the following events, or bad things will happen:
    protected abstract void onPlayerJoinImpl(Player player);
    public final void onPlayerJoin(Player player) { // formerly `addPlayer`
        allPlayers.add(player);
        onPlayerJoinImpl(player);
    }
    protected abstract void onPlayerLeaveImpl(Player player);
    public final void onPlayerLeave(Player player) {
        allPlayers.remove(player);
        onPlayerLeaveImpl(player);
    }
    protected abstract void startImpl();
    public final void start() {
        state = State.Playing;
        startImpl();
    }
    public abstract void onPlayerDeath(Player player);

    // Any subclass MAY implement the following events (it is not required, though often recommended)
    public void onPlayerBowCharge(PlayerInteractEvent event, Player player) {}
    public void onPlayerChat(Player player, String message) {}
    public void onPlayerBlockPlace(BlockPlaceEvent event, Player player) {}
    public void onPlayerMove(PlayerMoveEvent event, Player player) {}
    public void onPlayerHealthChange(Player player) {}
    public void onPlayerGlyph(Player player) {} // TODO: implement here
    public void rechargeArrow(Player player) {}
    public void onPlayerHitByPlayer(Player hit, Player hitter, double damage) {}

    // TODO: refactor to remove gameFinished etc
    // TODO: fix the other TODOs

    // # Variables
    protected final ArrayList<Player> allPlayers = new ArrayList<>();
    protected final String gameType;
    protected final World world;
    protected final String map;
    protected final boolean shouldCountAsStats;

    protected enum State {
        Queueing,
        Playing,
        Finished
    }
    protected State state = State.Queueing;


    // # Methods
    public void endGame() {
        for(Player player : world.getPlayers()) {
            BPBridge.connectPlayerToLobby(player);
        }
        // unload world
        BPBridge.instance.unloadWorld(world.getName());
    }
}
