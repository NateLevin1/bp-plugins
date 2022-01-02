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
    protected Game(String gameType, World world, String map, boolean shouldCountAsStats, Player startingPlayer) {
        this.gameType = gameType;
        this.world = world;
        this.map = map;
        this.shouldCountAsStats = shouldCountAsStats;
        allPlayers.add(startingPlayer);
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
    public boolean hasStarted() {
        return state == State.Playing;
    }
    public boolean hasGameFinished() {
        return state == State.Finished;
    }
    public boolean isQueueing() {
        return state == State.Queueing;
    }
    public boolean hasQueued() {
        return state == State.Playing || state == State.Finished;
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
    public void onPlayerBowCharge(PlayerInteractEvent event, Player player) {}
    public void onPlayerChat(Player player, String message) {}
    public void onPlayerBlockPlace(BlockPlaceEvent event, Player player) {}
    public void onPlayerMove(PlayerMoveEvent event, Player player) {}
    public void onPlayerHealthChange(Player player) {}
    public void onPlayerGlyph(Player player) {}
    public void rechargeArrow(Player player) {}
    public void onPlayerHitByPlayer(Player hit, Player hitter, double damage) {}
    public void onPlayerDeath(Player player) {}


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
}
