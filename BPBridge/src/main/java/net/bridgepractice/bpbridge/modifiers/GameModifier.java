package net.bridgepractice.bpbridge.modifiers;

import net.bridgepractice.bpbridge.GameInfo;
import org.bukkit.entity.Player;

public interface GameModifier {
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

    default void onBeforeStart(GameInfo gameInfo) {}
    default void onPlayerKilledByPlayer(Player player, Player killer, GameInfo gameInfo) {}
    default void onPlayerHitByPlayer(Player playerHit, Player damager, double damage) {}
}
