package club.bridgepractice.Bridge;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class PlayerInfo {
    PlayerLocation location;
    int position;
    Location respawnLocation;
    PlayerInfoHandler onDeath;
    PlayerInfoHandler onBlockChange;
    PlayerInfoHandler onLocationChange;
    PlayerInfoHandler onWin;
    PlayerInfoHandler onMove;
    BowShootHandler onBowShoot;
    SettingsMenu settingsMenu;

    public static class LocSettings {
        public int clayColor = 11;
        public int height = 99;
        public boolean isBridgingLeft = true;
        public int bowsInARow = 0;
        public Long warned = null;
        public boolean shouldHit;
        public String mode = "bypassStartPB";
        public BukkitTask arrowTask;
        public int npcId = 0;
        public PlayerInfoHandler onNpcHit;
        public boolean isSprintingHit = false;
    }

    LocSettings locSettings = new LocSettings();
    int arrowLoc = -1;
    // this holds the location of the block, you can assume it used to be air
    ArrayList<Location> changedBlocks = new ArrayList<>();
    AllowedLocation[] allowedPlacing = {};
    AllowedLocation winBox;
    AllowedLocation allowedBox;
    AllBreak[] allowedBreaking;
    public int[] relXZ;
    static HashMap<PlayerLocation, ArrayList<UUID>> queues = new HashMap<>();
    static HashMap<UUID, PlayerLocation> playerQueue = new HashMap<>();

    PlayerInfo(PlayerLocation loc) {
        location = loc;
    }
    PlayerInfo(PlayerLocation loc, SettingsMenu settingsMenu, PlayerInfoHandler onDeath, PlayerInfoHandler onBlockChange, PlayerInfoHandler onLocationChange, PlayerInfoHandler onWin, BowShootHandler onBowShoot, PlayerInfoHandler onMove) {
        location = loc;
        this.onDeath = onDeath;
        this.onBlockChange = onBlockChange;
        this.onLocationChange = onLocationChange;
        this.onWin = onWin;
        this.settingsMenu = settingsMenu;
        this.onBowShoot = onBowShoot;
        this.onMove = onMove;

        position = getPosition(loc);
        switch(loc) {
            case Spawn:
                respawnLocation = new Location(Bridge.instance.world, 0.5, 98, 0.5);
                break;
            case Wing:
                relXZ = new int[]{97 + (position * 19), -2};
                respawnLocation = new Location(Bridge.instance.world, relXZ[0] + 0.5, 100, relXZ[1] + 0.5);
                allowedPlacing = new AllowedLocation[]{new AllowedLocation(relXZ, 0, 9, 0, 5), new AllowedLocation(relXZ, 5, 11, 4, 23)};
                winBox = new AllowedLocation(relXZ, 4, 11, 23, 32, 90, 93);
                allowedBox = new AllowedLocation(relXZ, -5, 15, -5, 35, 80, 120);
                allowedBreaking = new AllBreak[]{new AllBreak(Material.STAINED_CLAY, 11), new AllBreak(Material.STAINED_CLAY, 14), new AllBreak(Material.STAINED_CLAY, 0)};
                break;
            case Prebow:
                respawnLocation = new Location(Bridge.instance.world, (97 + (position * 20)) + 0.5, 108, -138.5);
                allowedPlacing = new AllowedLocation[]{};
                break;
            case Bypass:
                relXZ = new int[]{-235 + (position * 20), -140};
                respawnLocation = new Location(Bridge.instance.world, relXZ[0] + 0.5, 93, relXZ[1] + 0.5);
                allowedBox = new AllowedLocation(relXZ, -8, 8, -12, 57, 80, 120);
                allowedPlacing = new AllowedLocation[]{new AllowedLocation(relXZ, -4, 4, -1, 47)};
                winBox = new AllowedLocation(relXZ, -2, 2, 52, 55, 85, 89);
                break;
            case BridgeBot:
                relXZ = new int[]{-132 - (position * 30), -5};
                respawnLocation = new Location(Bridge.instance.world, relXZ[0] + 0.5, 96.3, relXZ[1] + 0.5);
                allowedPlacing = new AllowedLocation[]{new AllowedLocation(relXZ, -8, 8, 3, 51)};
                allowedBreaking = new AllBreak[]{new AllBreak(Material.STAINED_CLAY, 11), new AllBreak(Material.STAINED_CLAY, 14), new AllBreak(Material.STAINED_CLAY, 0)};
                break;
        }
    }

    public static int getPosition(PlayerLocation loc) {
        ArrayList<Integer> positions = new ArrayList<>();
        for(PlayerInfo info : Bridge.instance.getAllPlayerInfos()) {
            if(info.location == loc) {
                positions.add(info.position);
            }
        }
        Collections.sort(positions);
        int position = -1;
        for(int pos : positions) {
            if(pos == position + 1) {
                position = pos;
            } else {
                break;
            }
        }
        position += 1;
        return position;
    }

    public static boolean addToQueueIfNeeded(Player player, PlayerLocation loc) {
        int position = getPosition(loc);
        switch(loc) {
            case Bypass:
                if(position > 15) {
                    return addToQueue(player, loc);
                }
                break;
            case Prebow:
            case BridgeBot:
                if(position > 3) {
                    return addToQueue(player, loc);
                }
                break;
            case Wing:
                if(position > 7) {
                    return addToQueue(player, loc);
                }
                break;
        }
        return false;
    }

    private static boolean addToQueue(Player player, PlayerLocation loc) {
        // add to queue and cancel
        PlayerLocation l = playerQueue.get(player.getUniqueId());
        if(l != null) {
            if(l == loc) {
                player.sendMessage("§cYou are already queued for " + Bridge.getPlayerReadableLocation(l) + "!");
                return true;
            } else {
                removeFromQueue(player, true);
            }
        }
        playerQueue.put(player.getUniqueId(), loc);
        queues.get(loc).add(player.getUniqueId());
        player.sendMessage("§aYou were added to the " + Bridge.getPlayerReadableLocation(loc) + " queue!");
        announcePlace(player, loc);
        return true;
    }

    public static void removeFromQueue(Player player, boolean tellPlayer) {
        PlayerLocation l = playerQueue.get(player.getUniqueId());
        ArrayList<UUID> locQ = queues.get(l);
        if(l != null) {
            int indexOfRemove = locQ.indexOf(player.getUniqueId());
            boolean wasRemoveSuccessful = locQ.remove(player.getUniqueId());
            playerQueue.remove(player.getUniqueId());
            if(wasRemoveSuccessful && indexOfRemove != locQ.size()) {
                announceChangeInPlaces(l);
            }
            if(wasRemoveSuccessful && tellPlayer) {
                player.sendMessage("§6You were removed from the " + Bridge.getPlayerReadableLocation(l) + " queue!");
            }
        }
    }
    public static void askToLeaveQueue(Player player) {
        PlayerLocation l = playerQueue.get(player.getUniqueId());
        if(l != null) {
            player.sendMessage("\n§6§lHey!§r§6 You entered a different gamemode but are still in the " + Bridge.getPlayerReadableLocation(l) + " queue.");
            player.spigot().sendMessage(new ComponentBuilder("§eClick here if you want to leave the queue.").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/leavequeue")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("§bClick to leave the queue.")})).create());
        }
    }

    public static void announceChangeInPlaces(PlayerLocation loc) {
        ArrayList<UUID> playersInQueue = PlayerInfo.queues.get(loc);
        for(UUID inQueueUuid : playersInQueue) {
            Player inQueuePlayer = Bridge.instance.getServer().getPlayer(inQueueUuid);
            PlayerInfo.announcePlace(inQueuePlayer, loc);
        }
    }

    public static void announcePlace(Player player, PlayerLocation loc) {
        int playersInFront = queues.get(loc).indexOf(player.getUniqueId());
        if(playersInFront == 0) {
            player.sendMessage("§b§lYou are next in queue!");
        } else if(playersInFront == 1) {
            player.sendMessage("§aThere is §b1§a player ahead of you.");
        } else {
            player.sendMessage("§aThere are §b" + (playersInFront) + "§a players ahead of you.");
        }
    }

    public static void nextInQueue(PlayerLocation loc, String command) {
        ArrayList<UUID> playersInQueue = PlayerInfo.queues.get(loc);
        if(playersInQueue != null && playersInQueue.size() > 0) {
            Player playerToAdd = Bridge.instance.getServer().getPlayer(playersInQueue.remove(0));
            playerQueue.remove(playerToAdd.getUniqueId());
            playerToAdd.chat("/" + command);
            announceChangeInPlaces(loc);
        }
    }
}

interface BowShootHandler {
    void call(PlayerInfo info, Arrow arrow, ItemStack bow);
}

class AllBreak {
    Material mat;
    int data;
    AllBreak(Material mat, int data) {
        this.mat = mat;
        this.data = data;
    }
}