package net.bridgepractice.bpbridge.bridgemodifiers;

import net.bridgepractice.bpbridge.games.BridgeBase;
import org.bukkit.entity.Player;

public interface BridgeModifier {
    String getGameType();
    String getPrettyGameType();
    void sendIntroMessage(Player player);

    default int getAmountOfBlocks() { return 64; }
    default int getAmountOfGaps() { return 8; }
    default String getCustomStatistic() { return "%goals%§fGoals: %§a0"; }
    default int getGameLengthMinutes() { return 15; }
    default int getCountdownTime() { return 5; }
    default String getNameForScore() { return "Goal"; }

    default boolean shouldUseCages() { return true; }
    default boolean shouldShowTitleOnScore() { return true; }
    default boolean shouldResetPlayerOnDeath() { return true; }

    default void onBeforeStart(BridgeBase bridgeBase) {}
    default void onPlayerKilledByPlayer(Player player, Player killer, BridgeBase bridgeBase) {}
    default void onPlayerHitByPlayer(Player playerHit, Player damager, double damage) {}
}
