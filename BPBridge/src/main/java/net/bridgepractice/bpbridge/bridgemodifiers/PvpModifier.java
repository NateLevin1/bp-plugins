package net.bridgepractice.bpbridge.bridgemodifiers;

import net.bridgepractice.bpbridge.games.BridgeBase;
import net.bridgepractice.bpbridge.Utils;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PvpModifier implements BridgeModifier {
    private int lastRandNum = -1;
    @Override
    public String getGameType() {
        return "pvp";
    }
    @Override
    public String getPrettyGameType() {
        return "PvP";
    }
    @Override
    public void sendIntroMessage(Player player) {
        player.sendMessage("§f§l                     PvP 1v1");
        player.sendMessage("\n§e  You will spawn in a randomly selected position.");
        player.sendMessage("\n§e       Kill your opponent 5 times to win!");
    }


    @Override
    public int getGameLengthMinutes() {
        return 5;
    }
    @Override
    public String getCustomStatistic() {
        return "%damage%§fDamage Dealt: %§a0§c" + Utils.hearts(1);
    }
    @Override
    public int getAmountOfBlocks() {
        return 5;
    }
    @Override
    public int getAmountOfGaps() {
        return 1;
    }
    @Override
    public int getCountdownTime() {
        return 3;
    }
    @Override
    public String getNameForScore() {
        return "Kill";
    }
    @Override
    public boolean shouldUseCages() {
        return false;
    }
    @Override
    public boolean shouldResetPlayerOnDeath() {
        return false;
    }
    @Override
    public boolean shouldShowTitleOnScore() {
        return false;
    }

    HashMap<UUID, Double> playerDamages = new HashMap<>();

    @Override
    public void onPlayerHitByPlayer(Player playerHit, Player damager, double damage) {
        double newDamageAmount = playerDamages.getOrDefault(damager.getUniqueId(), 0D)+(playerHit.getHealth() - damage < 0 ? playerHit.getHealth() : damage);
        playerDamages.put(damager.getUniqueId(), newDamageAmount);
        damager.getScoreboard().getTeam("damage").setSuffix("§a"+Math.round(newDamageAmount)+"§c" + Utils.hearts(1));
    }
    @Override
    public void onBeforeStart(BridgeBase bridgeBase) {
        randomizeSpawnLocs(bridgeBase);
    }
    @Override
    public void onPlayerKilledByPlayer(Player player, Player killer, BridgeBase bridgeBase) {
        randomizeSpawnLocs(bridgeBase);
        Location redBlockLoc = bridgeBase.getRedSpawnLoc().clone().subtract(0, 1,0);
        Location blueBlockLoc = bridgeBase.getBlueSpawnLoc().clone().subtract(0, 1,0);
        if(redBlockLoc.getBlock().getType() == Material.AIR) {
            redBlockLoc.getBlock().setType(Material.STAINED_CLAY);
            redBlockLoc.getBlock().setData(DyeColor.RED.getWoolData());
        }
        if(blueBlockLoc.getBlock().getType() == Material.AIR) {
            blueBlockLoc.getBlock().setType(Material.STAINED_CLAY);
            blueBlockLoc.getBlock().setData(DyeColor.BLUE.getWoolData());
        }
        if(killer != null) {
            bridgeBase.onPlayerScore(killer, bridgeBase.getTeamOfPlayer(killer));
        } else {
            String team = bridgeBase.getTeamOfPlayer(player).equals("red") ? "blue" : "red";
            bridgeBase.onPlayerScore(bridgeBase.getMemberOfTeam(team), team);
        }
        for(Location loc : bridgeBase.getBlocksPlaced()) {
            loc.getBlock().setType(Material.AIR);
        }
        bridgeBase.clearBlocksPlaced();
    }


    private void randomizeSpawnLocs(BridgeBase bridgeBase) {
        int randNum = ThreadLocalRandom.current().nextInt(3); // 0, 1, or 2
        while(randNum == lastRandNum) {
            randNum = ThreadLocalRandom.current().nextInt(3); // 0, 1, or 2
        }
        World world = bridgeBase.getWorld();
        bridgeBase.setRedSpawnLoc(getRedSpawnLoc(bridgeBase.getMap(), randNum, world));
        bridgeBase.setBlueSpawnLoc(getBlueSpawnLoc(bridgeBase.getMap(), randNum, world));
        lastRandNum = randNum;
    }

    public static Location getRedSpawnLoc(String map, int randNum, World world) {
        switch(map) {
            case "developedatlantis":
                return randNum == 2 ? new Location(world, -0.5, 100, -0.5, 90, 0) : (randNum == 1 ? new Location(world, 15.5, 100, 0.5, 94, 0) : new Location(world, 39.5, 104, 3.5, 130, 0));
            case "developedgalaxy":
                return randNum == 2 ? new Location(world, 15.5, 100, 1.5, 120, 0) : (randNum == 1 ? new Location(world, -3.5, 100, -6.5, 59, 0) : new Location(world, 26.5, 93, -2.5, 135, 0));
            case "developedsorcery":
                return randNum == 2 ? new Location(world, 14.5, 100, 7, 90, 0) : (randNum == 1 ? new Location(world, 2.5, 100, 1.5, 94, 0) : new Location(world, 20.5, 100, 0.5, 90, 0));
            case "developedstumped":
                return randNum == 2 ? new Location(world, 1.5, 100, -5.5, 66, 0) : (randNum == 1 ? new Location(world, 18.5, 100, 0.5, 99, 0) : new Location(world, 17, 100, 3.5, 68, 0));
        }
        return null;
    }
    public static Location getBlueSpawnLoc(String map, int randNum, World world) {
        switch(map) {
            case "developedatlantis":
                return randNum == 2 ? new Location(world, -15.5, 100, -0.5, -90, 0) : (randNum == 1 ? new Location(world, 4.5, 100, -0.5, -84, 0) : new Location(world, 32.5, 104, -2.5, -50, 0));
            case "developedgalaxy":
                return randNum == 2 ? new Location(world, 5.5, 100, -4.5, -60, 0) : (randNum == 1 ? new Location(world, -15.5, 100, 0.5, -119, 0) : new Location(world, 18.5, 93, -10.5, -45, 0));
            case "developedsorcery":
                return randNum == 2 ? new Location(world, 4.5, 100, 7, -90, 0) : (randNum == 1 ? new Location(world, -9.5, 100, 0.5, -86, 0) : new Location(world, 7.5, 100, 0.5, -90, 0));
            case "developedstumped":
                return randNum == 2 ? new Location(world, -13.5, 100, -0.5, -109, 0) : (randNum == 1 ? new Location(world, 5.5, 100, -1.5, -81, 0) : new Location(world, 3.5, 100, 8, -90, 0));
        }
        return null;
    }
}
