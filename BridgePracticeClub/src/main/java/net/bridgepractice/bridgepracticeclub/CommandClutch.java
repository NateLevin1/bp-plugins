package net.bridgepractice.bridgepracticeclub;

import net.bridgepractice.RavenAntiCheat.RavenAntiCheat;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

public class CommandClutch implements CommandExecutor {
    public static BlockState[][][] spawnContent;
    public static BlockState[][][] spawnContent2;
    public static BlockState[][][] bridgeContent;
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player!");
            return true;
        }
        Player player = (Player) sender;
        PlayerInfo oldInfo = Bridge.instance.getPlayer(player.getUniqueId());
        if(oldInfo != null && oldInfo.location == PlayerLocation.Clutch) {
            player.sendMessage("§cYou are already practicing clutching!");
            return true;
        }

        if(Bridge.disabledGames.getOrDefault("clutch", false)) {
            player.sendMessage("§cQueueing for that game has been temporarily disabled");
            return true;
        }

        boolean wasQueueNeeded = PlayerInfo.addToQueueIfNeeded(player, PlayerLocation.Clutch);
        if(wasQueueNeeded) {
            return true;
        }

        PlayerInfo.askToLeaveQueue(player);

        Variables vars = new Variables();
        vars.spawn = new Structure(spawnContent);
        vars.bridge = new Structure(bridgeContent);

        Scoreboard board = Bridge.createScoreboard("   §b§eClutch Practice    ", new String[] {
                "",
                "%mode% §fMode: %§aWaiting...",
                "",
                "%difficulty% §fDifficulty: %§eNormal",
                "",
                "%blocks% §fBlocks Plac%§fed: §a0",
                "",
                "%info% §eWalk forward!",
                "",
                "   §7bridgepractice.net  "
        });
        Bridge.setScoreboard(player, board);

        Bridge.instance.setPlayer(player.getUniqueId(), new PlayerInfo(PlayerLocation.Clutch, null, (info)->{
            // on death
            if(vars.clutchingChecker != null) {
                vars.clutchingChecker.cancel();
            }
            for(Location loc : info.changedBlocks) {
                loc.getBlock().setType(Material.AIR);
            }
            if(vars.state != State.WalkingOnGold) {
                player.sendMessage("§c✕ Mode failed!");
                nextGameMode(player, info, vars);
                player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 1);
            } else {
                new ResetBridgePlayer(player, info, false, true).run();
            }
        }, (info) -> {
            // on block place
            vars.blocksPlaced++;
            board.getTeam("blocks").setSuffix("§fed: §a" + vars.blocksPlaced);
        }, (info)->{
            // on location change
            Bridge.sendTitle(player, "", "", 0, 0, 0);
            vars.spawn.remove();
            vars.bridge.remove();
            if(vars.npc != null) {
                vars.npc.remove();
            }
            if(vars.clutchingChecker != null) {
                vars.clutchingChecker.cancel();
            }
            for(Location loc : info.changedBlocks) {
                loc.getBlock().setType(Material.AIR);
            }
        }, null, null, (info) -> {
            // on move

            double relX = player.getLocation().getX() - info.relXZ[0];
            double relZ = player.getLocation().getZ() - info.relXZ[1];
            if(vars.state == State.WaitingForWalkOnGold) {
                if(relZ > 2) {
                    vars.setState(State.WalkingOnGold);
                    vars.spawn.switchContent(spawnContent2);
                    vars.spawn.placeAtPreviousLocation();
                    vars.bridge.place(vars.bridgeLocation.clone().subtract(0, 7, 0));
                    Bridge.sendTitle(player, "", "Continue to The Bridge!", 0, 0, Integer.MAX_VALUE);
                    player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.2f, 1.3f);
                    Bridge.setBridgeInventory(player, false);
                    try {
                        player.getInventory().setHeldItemSlot(player.getInventory().first(Material.STAINED_CLAY));
                    } catch(IllegalArgumentException ignored) {}
                }
            } else if(vars.state == State.WalkingOnGold) {
                if(relZ > 10) {
                    vars.setState(State.BotChase);
                    Bridge.sendTitle(player, "", "", 0, 0, 0);
                    vars.spawn.remove();
                    Bridge.sendActionBar(player, "§e§lRun Away! §eYou're being chased, better move quickly!");
                    board.getTeam("info").setPrefix("§e Run from the");
                    board.getTeam("info").setSuffix("§e  bot!");
                    board.getTeam("mode").setSuffix("§aBot Chase");
                    vars.npc = new NPC(player, null, null, null)
                            .setLocation(vars.bridgeLocation)
                            .setBridge(Color.BLACK)
                            .showToPlayer(false)
                            .setVeloRunnable((npc)->{
                                long timeSinceLastJump = System.currentTimeMillis() - npc.lastJump;
                                boolean isOnGround = npc.npcLoc.clone().subtract(0, 0.05, 0).getBlock().getType() != Material.AIR ||
                                        npc.npcLoc.clone().subtract(-0.15, 0.05, 0).getBlock().getType() != Material.AIR ||
                                        npc.npcLoc.clone().subtract(0.15, 0.05, 0).getBlock().getType() != Material.AIR;

                                if(timeSinceLastJump > 500 && isOnGround) {
                                    npc.jump();
                                }
                                if(npc.npcLoc.getZ() < player.getLocation().getZ()+0.5) {
                                    npc.velocity.add(new Vector(0, 0, (vars.getTimeSinceLastStateChange()/100_000D)+0.03));
                                }

                                if(vars.getTimeSinceLastStateChange() > 1000 && npc.isPlayerInSight(2) && vars.state == State.BotChase) {
                                    npc.swingHand();
                                    npc.veloRunnable.cancel();

                                    int leftOrRight = Math.random() > 0.5 ? -1 : 1;
                                    int difficulty = info.locSettings.difficulty+1; // 0=easy, 1=normal, 2=hard

                                    RavenAntiCheat.emulatePlayerTakeKnockback(player);
                                    if(difficulty == 0) {
                                        player.setVelocity(new Vector(leftOrRight*(Math.random()*0.02+0.25), 0.50, 0.30));
                                    } else if(difficulty == 1) {
                                        player.setVelocity(new Vector(leftOrRight*(Math.random()*0.03+0.31), 0.49, 0.40));
                                    } else if(difficulty == 2) {
                                        player.setVelocity(new Vector(leftOrRight*(Math.random()*0.03+0.40), 0.35, Math.random()*0.05+0.4));
                                    }


                                    player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);
                                    vars.setState(State.BotChaseClutching);
                                    board.getTeam("info").setPrefix("§f Time: ");
                                    vars.clutchingChecker = (new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            boolean isPlayerOnGround = player.getLocation().subtract(0, 0.001, 0).getBlock().getType() != Material.AIR; // NOTE: this is inaccurate if shifting to edge; works fine for other cases
                                            long lastStateChange = vars.getTimeSinceLastStateChange();
                                            if((lastStateChange > 500 && isPlayerOnGround) // fast path
                                                || lastStateChange > 3000) {
                                                cancel();
                                                // win!
                                                player.sendMessage("§aYou clutched!");
                                                nextGameMode(player, info, vars);
                                                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                                return;
                                            }
                                            String secs = String.format("%.3f", (3000 - vars.getTimeSinceLastStateChange())/1000D);
                                            Bridge.sendActionBar(player, "§eTime Remaining: §f"+secs);
                                            board.getTeam("info").setSuffix("§a"+secs);
                                        }
                                    }).runTaskTimer(Bridge.instance, 0, 5);
                                }
                            }, true);
                    info.locSettings.npcId = vars.npc.npc.getId();
                    info.locSettings.onNpcHit = (i) -> {
                        player.sendMessage("§cYou cannot hit the bot in this mode!");
                    };
                    vars.npc.healthScore.setScore(999);
                    player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 0.3f, 1);
                    player.setGameMode(GameMode.SURVIVAL);
                }
            }
        }));

        PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
        vars.bridgeLocation = new Location(player.getWorld(), info.relXZ[0]+0.5, 100, info.relXZ[1]+10.5);

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(info.respawnLocation);
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setItem(0, Bridge.makeItem(Material.STAINED_CLAY, 1, "§7Difficulty: §aEasy ♟ §7(Right Click)", new String[]{"Select the §aeasy§7 difficulty"}, 5));
        inv.setItem(1, Bridge.makeItem(Material.STAINED_CLAY, 1, "§7Difficulty: §eNormal ♜ §7(Right Click)", new String[]{"Select the §enormal§7 difficulty"}, 4));
        inv.setItem(2, Bridge.makeItem(Material.STAINED_CLAY, 1, "§7Difficulty: §c§lHard ♚ §7(Right Click)", new String[]{"Select the §chard§7 difficulty"}, 14));
        inv.setHeldItemSlot(1);

        Bridge.sendTitle(player, "Walk Forward to Begin", "§7Step on the §6GOLD§7 blocks!", 0, 0, Integer.MAX_VALUE);

        player.sendMessage("§a§m----------------------------------------\n" +
                "§f \n  §e███  █     █  █  ███  ███   █   █\n" +
                "  §6█      █      █  █    █    █       ████\n" +
                "  §c███  ███ ███    █    ███   █   █\n" +
                "§f \n§a Practice clutching in §e§lrealistic§a scenarios.\n" +
                "§7 Walk on the §6gold§7 blocks to begin.\n" +
                "§f \n§a§m----------------------------------------");

        vars.spawn.place(info.respawnLocation.clone().subtract(4,4,4));

        return true;
    }

    public void nextGameMode(Player player, PlayerInfo info, Variables vars) {
        info.respawnLocation = vars.bridgeLocation;
        new ResetBridgePlayer(player, info, false, true).run();
    }

    static class Variables {
        Structure spawn;
        Structure bridge;
        NPC npc;
        State state = State.WaitingForWalkOnGold;
        Location bridgeLocation;
        long lastStateChange = 0;
        BukkitTask clutchingChecker;
        int blocksPlaced = 0;
        public void setState(State state) {
            this.state = state;
            lastStateChange = System.currentTimeMillis();
        }
        public long getTimeSinceLastStateChange() {
            return System.currentTimeMillis() - lastStateChange;
        }
    }
    enum State {
        WaitingForWalkOnGold,
        WalkingOnGold,
        BotChase,
        BotChaseClutching
    }
}
