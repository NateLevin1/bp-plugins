package net.bridgepractice.bpbridge;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

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
    protected void setAmountOfBlocks(int amountOfBlocks) {
        this.amountOfBlocks = amountOfBlocks;
    }
    protected void setAmountOfGaps(int amountOfGaps) {
        this.amountOfGaps = amountOfGaps;
    }


    // # Events
    // Any subclass MUST implement the following events, or bad things will happen:
    protected abstract void onPlayerJoinImpl(Player player);
    public final void onPlayerJoin(Player player) { // formerly `addPlayer`
        if(!player.isOnline()) {
            // we tried to add a player to the game who is offline - that shouldn't be possible
            Utils.sendDebugErrorWebhook("Tried to add player "+player.getName()+" to game but they were offline. "+Utils.getGameDebugInfo(world.getName()));
            return;
        }
        allPlayers.add(player);
        onPlayerJoinImpl(player);
    }
    protected abstract void onPlayerLeaveImpl(Player player);
    public final void onPlayerLeave(Player player) {
        boolean wasSuccessful = allPlayers.remove(player);
        if(!wasSuccessful) {
            // player was not in the game but they left
            Utils.sendDebugErrorWebhook("Player "+player.getName()+" was not in the game but onPlayerLeave was called. "+Utils.getGameDebugInfo(world.getName()));
            return;
        }
        onPlayerLeaveImpl(player);
    }
    protected abstract void startImpl();
    public final void start() {
        state = State.Playing;
        for(Player player : allPlayers) {
            if(!player.isOnline()) {
                Utils.sendDebugErrorWebhook("Starting game with the offline player '"+player.getName()+"'!"+Utils.getGameDebugInfo(world.getName()));
            }
            player.getInventory().setContents(
                    inventories.get(player.getUniqueId()).getContents()
            );
        }
        startImpl();
    }
    public abstract void onPlayerDeath(Player player);

    // Any subclass MAY implement the following events (it is not required, though often recommended)
    public void onPlayerBowCharge(PlayerInteractEvent event, Player player) {}
    public void onPlayerChat(Player player, String message) {}
    public void onPlayerBlockPlace(BlockPlaceEvent event, Player player) {}
    public void onPlayerMove(PlayerMoveEvent event, Player player) {}
    public void onPlayerHealthChange(Player player) {}
    public void onPlayerHitByPlayer(Player hit, Player hitter, double damage) {}


    // # Variables
    protected final ArrayList<Player> allPlayers = new ArrayList<>();
    protected final String gameType;
    protected final World world;
    protected final String map;
    protected final boolean shouldCountAsStats;
    protected String dashes = StringUtils.repeat("-", 64);
    private final HashMap<UUID, Inventory> inventories = new HashMap<>();
    private final HashMap<UUID, Integer> arrowLocations = new HashMap<>();
    private final HashMap<UUID, BukkitTask> arrowRecharges = new HashMap<>();
    private int amountOfBlocks = 64;
    private int amountOfGaps = 8;

    protected enum State {
        Queueing,
        Playing,
        Finished
    }
    protected State state = State.Queueing;


    // # Methods
    public final void endGame() {
        (new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : world.getPlayers()) {
                    BPBridge.connectPlayerToLobby(player);
                }
                // unload world
                BPBridge.instance.unloadWorld(world.getName());
            }
        }).runTaskLater(BPBridge.instance, 3*20);
    }
    private final HashMap<UUID, Long> glyphTimes = new HashMap<>();
    public final void onPlayerGlyph(Player player) {
        long lastGlyph = glyphTimes.getOrDefault(player.getUniqueId(), 0L);
        if(System.currentTimeMillis() - lastGlyph > 10 * 1000) {
            glyphTimes.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage("§eYou displayed your §bSkill Glyph§e!");
            for(Player p : allPlayers) {
                p.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
            }
        } else {
            player.sendMessage("§cYou must wait §e10s§c between glyph usages!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
        }
    }
    protected void loadPlayerHotbar(Player player, boolean isOnRed) {
        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = BPBridge.connection.prepareStatement("SELECT hotbarSword, hotbarBow, hotbarPickaxe, hotbarBlocksOne, hotbarBlocksTwo, hotbarGoldenApple, hotbarArrow, hotbarGlyph FROM players WHERE uuid=?;")) {
                    Inventory inv = BPBridge.instance.getServer().createInventory(null, InventoryType.PLAYER);
                    statement.setString(1, player.getUniqueId().toString()); // uuid
                    ResultSet res = statement.executeQuery();
                    if(!res.next()) {
                        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                    }
                    inv.setItem(res.getInt("hotbarSword"), Utils.getSword());
                    inv.setItem(res.getInt("hotbarBow"), Utils.getBow());
                    inv.setItem(res.getInt("hotbarPickaxe"), Utils.getPickaxe());
                    inv.setItem(res.getInt("hotbarBlocksOne"), Utils.getBlocks(isOnRed, amountOfBlocks));
                    inv.setItem(res.getInt("hotbarBlocksTwo"), Utils.getBlocks(isOnRed, amountOfBlocks));
                    inv.setItem(res.getInt("hotbarGoldenApple"), Utils.getGapple(amountOfGaps));
                    int arrow = res.getInt("hotbarArrow");
                    inv.setItem(arrow, Utils.getArrow());
                    arrowLocations.put(player.getUniqueId(), arrow);
                    inv.setItem(res.getInt("hotbarGlyph"), Utils.getGlyph());
                    inventories.put(player.getUniqueId(), inv);
                } catch (SQLException e) {
                    e.printStackTrace();
                    BukkitRunnable run = new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.kickPlayer("§c§lUh oh!§r§c There was an error getting your bridge hotbar!\nPlease check the Discord, and open a ticket if the issue persists.");
                        }
                    };
                    run.runTaskLater(BPBridge.instance, 0);
                }
            }
        }).runTaskAsynchronously(BPBridge.instance);
    }
    public void rechargeArrow(Player player) {
        arrowRecharges.put(player.getUniqueId(), (new BukkitRunnable() {
            long startTime = -1;
            final float regenTime = 3.5f;
            @Override
            public void run() {
                if(!player.isOnline()) {
                    this.cancel();
                    return;
                }
                if(startTime == -1) {
                    startTime = System.currentTimeMillis();
                }
                float timeSince = (System.currentTimeMillis() - startTime) / 1000f;
                if(timeSince > regenTime) {
                    if(state == State.Playing) {
                        player.getInventory().setItem(arrowLocations.get(player.getUniqueId()), Utils.getArrow());
                    }
                    this.cancel();
                    arrowRecharges.remove(player.getUniqueId());
                    player.setExp(0);
                    return;
                }
                player.setExp(1 - (timeSince / regenTime));
                player.setLevel((int) (regenTime - timeSince));
            }
        }).runTaskTimer(BPBridge.instance, 0, 2));
    }
    protected void resetArrowRecharge(Player player) {
        BukkitTask arrowTask = arrowRecharges.get(player.getUniqueId());
        if(arrowTask != null) {
            arrowTask.cancel();
            arrowRecharges.remove(player.getUniqueId());
        }
        player.setLevel(0);
        player.setExp(0);
    }
    protected void resetPlayer(Player player) {
        PlayerInventory currentPlayerInv = player.getInventory();
        Inventory inv = inventories.get(player.getUniqueId());
        currentPlayerInv.setContents(inv.getContents());
        (new BukkitRunnable() {
            @Override
            public void run() {
                // reset the arrow every time since it disappears often
                for(int i = 0; i < inv.getSize(); i++) {
                    if(inv.getItem(i) != null && inv.getItem(i).getType() == Material.ARROW) {
                        currentPlayerInv.setItem(i, Utils.getArrow());
                    }
                }
            }
        }).runTaskLater(BPBridge.instance, 4);


        // reset these items so players can continue to hold right click after being reset
        if(currentPlayerInv.getItemInHand().getType() == Material.GOLDEN_APPLE) {
            currentPlayerInv.setItem(currentPlayerInv.getHeldItemSlot(), Utils.getGapple(amountOfGaps));
        } else if(currentPlayerInv.getItemInHand().getType() == Material.BOW) {
            currentPlayerInv.setItem(currentPlayerInv.getHeldItemSlot(), Utils.getBow());
        }

        resetArrowRecharge(player);
        // clear potion effects
        for(PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
}
