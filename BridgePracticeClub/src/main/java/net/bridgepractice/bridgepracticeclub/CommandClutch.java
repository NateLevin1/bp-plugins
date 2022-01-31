package net.bridgepractice.bridgepracticeclub;

import net.bridgepractice.RavenAntiCheat.RavenAntiCheat;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class CommandClutch implements CommandExecutor {
    public static BlockState[][][] spawnContent;
    public static BlockState[][][] spawnContent2;
    public static BlockState[][][] bridgeContent;
    public static BlockState[][][] bridgeDevelopedContent;
    public static BlockState[][][] bridgeBypassContent;
    public static ItemStack difficultyItemEasy = Bridge.makeItem(Material.STAINED_CLAY, 1, "§7Difficulty: §aEasy ♟ §7(Right Click)", new String[]{"Select the §aeasy§7 difficulty"}, 5);
    public static ItemStack difficultyItemNormal = Bridge.makeItem(Material.STAINED_CLAY, 1, "§7Difficulty: §eNormal ♜ §7(Right Click)", new String[]{"Select the §enormal§7 difficulty"}, 4);
    public static ItemStack difficultyItemHard = Bridge.makeItem(Material.STAINED_CLAY, 1, "§7Difficulty: §c§lHard ♚ §7(Right Click)", new String[]{"Select the §chard§7 difficulty"}, 14);
    public static ItemStack singleHitItem = Bridge.makeItem(Material.STICK, 1, "§aSingle Hit§2/Double Hit §7(Right Click)", new String[]{"The bot will hit you one time.","Right click to so it will","hit you two times."}, -1);
    public static ItemStack doubleHitItem = Bridge.makeItem(Material.BLAZE_ROD, 1, "§2Single Hit/§aDouble Hit §7(Right Click)", new String[]{"The bot will hit you two times.","Right click to so it will","hit you one time."}, -1);
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
                "%attempt% §fAttempt: %§a0§7/5",
                "",
                "%info% §eWalk forward!",
                "",
                "   §7bridgepractice.net  "
        });
        Bridge.setScoreboard(player, board);

        Bridge.instance.setPlayer(player.getUniqueId(), new PlayerInfo(PlayerLocation.Clutch, null, (info)->{
            // on death
            if(vars.state == State.Dying || vars.state == State.Winning) return;

            if(vars.state != State.WalkingOnGold) {
                vars.setState(State.Dying);
                player.sendMessage("§c✕ Mode failed!");
                Bridge.sendTitle(player, "", "§c✕ Mode failed!", 5, 5, 13);
                player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 1);
                vars.tasks.add((new BukkitRunnable() {
                    @Override
                    public void run() {
                        nextGameMode(player, vars, info);
                    }
                }).runTaskLater(Bridge.instance, 13));
            } else {
                new ResetBridgePlayer(player, info, false, true).run();
            }
        }, (info) -> {
            // on block place
            vars.blocksPlaced++;
            board.getTeam("blocks").setSuffix("§fed: §a" + vars.blocksPlaced);
        }, (info)->{
            // on location change
            reset(player, vars, info);
            Bridge.givePlayerXP(player, vars.xpGained);
            try(PreparedStatement statement = Bridge.connection.prepareStatement("UPDATE players SET clutchDifficulty = ?, clutchDoubleHit = ? WHERE uuid=?;")) {
                statement.setInt(1, info.locSettings.difficulty); // clutchDifficulty
                statement.setBoolean(2, info.locSettings.doubleHit); // clutchDoubleHit
                statement.setString(3, player.getUniqueId().toString()); // uuid, set to player uuid
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                player.sendMessage("§c§lUh oh!§r§c Something went wrong syncing your settings to our database. Please open a ticket on the discord!");
            }
        }, null, null, (info) -> {
            // on move

            if(player.getLocation().getY() < 93) {
                info.onDeath.call(info);
            }

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
                    nextAttempt(player, vars);
                    Bridge.setBridgeInventory(player, false);
                    try {
                        player.getInventory().setHeldItemSlot(player.getInventory().first(Material.STAINED_CLAY));
                    } catch(IllegalArgumentException ignored) {}
                }
            } else if(vars.state == State.WalkingOnGold) {
                if(relZ > 10) {
                    Bridge.sendTitle(player, "", "", 0, 0, 0);
                    beginBotChase(player, vars, info, false, vars.bridgeLocation, HitDirection.Random, 1);
                }
            }
        }));

        PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
        vars.bridgeLocation = new Location(player.getWorld(), info.relXZ[0]+0.5, 100, info.relXZ[1]+10.5);

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(info.respawnLocation);
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setItem(0, difficultyItemEasy);
        inv.setItem(1, Bridge.getEnchanted(difficultyItemNormal.clone()));
        inv.setItem(2, difficultyItemHard);
        inv.setItem(4, singleHitItem);
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

        // load settings from DB
        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT clutchDifficulty, clutchDoubleHit FROM players WHERE uuid=?;")) {
                    statement.setString(1, player.getUniqueId().toString()); // uuid
                    ResultSet res = statement.executeQuery();
                    if(!res.next()) {
                        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                    }
                    int difficulty = res.getInt(1);
                    boolean doubleHit = res.getBoolean(2);
                    if(difficulty != 0 || doubleHit) {
                        info.locSettings.difficulty = difficulty;
                        info.locSettings.doubleHit = doubleHit;
                        selectDifficulty(player, difficulty);
                        PlayerInventory inv = player.getInventory();
                        inv.setHeldItemSlot(difficulty+1);
                        if(doubleHit) {
                            inv.setItem(4, doubleHitItem);
                        }
                        player.sendMessage("§a✔ §7Successfully loaded your settings!");
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
                }
            }
        }).runTaskAsynchronously(Bridge.instance);

        return true;
    }

    private void nextGameMode(Player player, Variables vars, PlayerInfo info) {
        MiniMode gameMode;
        boolean isNewMode;
        if(vars.attempt < vars.maxAttempts) {
            // still attempting, do not go to new gamemode
            nextAttempt(player, vars);
            gameMode = vars.curGameMode;
            isNewMode = false;
        } else {
            // go to new gamemode
            vars.attempt = 1;
            updateAttempt(player, vars);
            // ensure that the gamemode is different every time
            do {
                gameMode = MiniMode.values()[ThreadLocalRandom.current().nextInt(0, MiniMode.values().length)];
            } while(gameMode == vars.curGameMode);
            vars.curGameMode = gameMode;
            isNewMode = true;
        }

        reset(player, vars, info);
        info.respawnLocation = vars.bridgeLocation;
        new ResetBridgePlayer(player, info, false, true).run();
        try {
            player.getInventory().setHeldItemSlot(player.getInventory().first(Material.STAINED_CLAY));
        } catch(IllegalArgumentException ignored) {}
        boolean botOpposite;
        Location npcSpawnLoc;
        String modeName;
        HitDirection hitDirection = HitDirection.Random;
        double hitDistanceModifier = 1;
        switch(gameMode) {
            case Flat:
            {
                player.teleport(vars.bridgeLocation.clone().add(0, 0, 5));
                botOpposite = false;
                vars.bridge = new Structure(bridgeContent);
                npcSpawnLoc = vars.bridgeLocation;
                modeName = "Bot Chase";
                break;
            }
            case Developed: {
                player.teleport(vars.bridgeLocation.clone().add(0, -2, 6));
                botOpposite = true;
                vars.bridge = new Structure(bridgeDevelopedContent);
                npcSpawnLoc = vars.bridgeLocation.clone().add(0,-1,31);
                modeName = "Dev. Bot Chase";
                break;
            }
            case Bypass1:
            case Bypass2: {
                if(gameMode == MiniMode.Bypass1) {
                    player.teleport(vars.bridgeLocation.clone().add(1, -3, 8));
                    hitDirection = HitDirection.Left;
                } else {
                    player.teleport(vars.bridgeLocation.clone().add(-1, -3, 8));
                    hitDirection = HitDirection.Right;
                }

                botOpposite = false;
                vars.bridge = new Structure(bridgeBypassContent);
                npcSpawnLoc = vars.bridgeLocation.clone().add(0,0,3);
                modeName = "Bypass Chase";
                hitDistanceModifier = 0.5;
                break;
            }
            default: {
                player.sendMessage("§c§lUh Oh!§c Your clutch game tried to start a mode that doesn't exist! Please report this to the discord! (gm="+gameMode+")");
                player.chat("/l");
                return;
            }
        }
        if(isNewMode) {
            Bridge.sendTitle(player, "", "§eRun Forward!", 5, 5, 30);
            player.sendMessage("§a§l➡ §aNext Mode: §b"+modeName);
        }
        Bridge.sendActionBar(player, "§e§lRun "+(botOpposite ? "Forward" : "Away")+"! §e"+(botOpposite ? "Run towards the bot to fight it" : "You're being chased, better move quickly")+"!");
        Scoreboard board = player.getScoreboard();
        board.getTeam("info").setPrefix("§e Run " + (botOpposite ? "towards" : "from the"));
        board.getTeam("info").setSuffix("§e "+(botOpposite ? "the " : "")+"bot!");
        board.getTeam("mode").setSuffix("§a"+modeName);
        beginBotChase(player, vars, info, botOpposite, npcSpawnLoc, hitDirection, hitDistanceModifier);

        if(vars.attempt == vars.maxAttempts) {
            vars.tasks.add((new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage("§e§l⚠ Final attempt for this mode! ⚠");
                    player.playSound(player.getLocation(), Sound.FIREWORK_LARGE_BLAST2, 1.5f, 0.8f);
                }
            }).runTaskLater(Bridge.instance, 20));
        }
    }
    enum HitDirection {
        Left,
        Right,
        Random
    }
    private void beginBotChase(Player player, Variables vars, PlayerInfo info, boolean botOpposite, Location npcSpawnLoc, HitDirection hitDirection, double hitDistanceModifier) {
        Scoreboard board = player.getScoreboard();
        int zDir = botOpposite ? -1 : 1;
        vars.setState(State.BotChase);
        vars.bridge.place(vars.bridgeLocation.clone().subtract((int)(vars.bridge.width/3), 7, 0));
        vars.spawn.remove();
        vars.npc = new NPC(player, null, null, null)
                .setLocation(npcSpawnLoc)
                .setBridge(Color.BLACK)
                .showToPlayer(false)
                .setVeloRunnable((npc)->{
                    long timeSinceLastJump = System.currentTimeMillis() - npc.lastJump;
                    boolean isOnGround = npc.npcLoc.clone().subtract(0, 0.05, 0).getBlock().getType() != Material.AIR ||
                            npc.npcLoc.clone().subtract(-0.15, 0.05, 0).getBlock().getType() != Material.AIR ||
                            npc.npcLoc.clone().subtract(0.15, 0.05, 0).getBlock().getType() != Material.AIR;
                    int difficulty = info.locSettings.difficulty+1; // 0=easy, 1=normal, 2=hard

                    if(timeSinceLastJump > 500 && isOnGround) {
                        npc.jump();
                    }
                    if((!botOpposite && npc.npcLoc.getZ() < player.getLocation().getZ()+0.5)
                        || (botOpposite && npc.npcLoc.getZ() > player.getLocation().getZ()-0.5)) {
                        npc.velocity.add(new Vector(0, 0, zDir*((difficulty*0.01)+(vars.getTimeSinceLastStateChange()/(botOpposite ? 40_000D : 20_000D))+0.03)));
                    }

                    if(vars.getTimeSinceLastStateChange() > 1000
                            && (
                                (!botOpposite && player.getLocation().getZ() - npc.npcLoc.getZ() < 2)
                                || (botOpposite && npc.npcLoc.getZ() - player.getLocation().getZ() < 2)
                            )
                            && npc.isPlayerInSight(2)
                            && vars.state == State.BotChase
                    ) {
                        npc.veloRunnable.cancel();

                        int leftOrRight;
                        switch(hitDirection) {
                            case Left: leftOrRight = 1; break;
                            case Right: leftOrRight = -1; break;
                            case Random: leftOrRight = Math.random() > 0.5 ? -1 : 1; break;
                            default: new Exception("Unknown hit direction "+hitDirection).printStackTrace(); return;
                        }

                        double[] xDoubleHitModifier = {1};
                        double[] yDoubleHitModifier = {info.locSettings.doubleHit ? 0.8 : 1};
                        double[] zDoubleHitModifier = {1};
                        Runnable hitPlayer = ()->{
                            npc.swingHand();
                            RavenAntiCheat.emulatePlayerTakeKnockback(player);
                            if(difficulty == 0) {
                                player.setVelocity(new Vector(xDoubleHitModifier[0]*hitDistanceModifier*leftOrRight*(Math.random()*0.02+0.25), yDoubleHitModifier[0]*0.50, zDoubleHitModifier[0]*zDir*0.30));
                            } else if(difficulty == 1) {
                                player.setVelocity(new Vector(xDoubleHitModifier[0]*hitDistanceModifier*leftOrRight*(Math.random()*0.03+0.31), yDoubleHitModifier[0]*0.49, zDoubleHitModifier[0]*zDir*0.40));
                            } else if(difficulty == 2) {
                                player.setVelocity(new Vector(xDoubleHitModifier[0]*hitDistanceModifier*leftOrRight*(Math.random()*0.03+0.40), yDoubleHitModifier[0]*0.35, zDoubleHitModifier[0]*zDir*(Math.random()*0.05+0.4)));
                            }
                            player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);
                            EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutAnimation(entityPlayer, 1));
                        };

                        hitPlayer.run();

                        if(info.locSettings.doubleHit) {
                            vars.tasks.add(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    xDoubleHitModifier[0] = 1.3;
                                    yDoubleHitModifier[0] = 0.7;
                                    zDoubleHitModifier[0] = 0.7;
                                    hitPlayer.run();
                                }
                            }.runTaskLater(Bridge.instance, 9));
                        }



                        vars.setState(State.BotChaseClutching);
                        board.getTeam("info").setPrefix("§f Time: ");
                        int clutchSecs = 2000;
                        vars.tasks.add((new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(vars.state != State.BotChaseClutching) {
                                    cancel();
                                    return;
                                }
                                boolean isPlayerOnGround = player.getLocation().subtract(0, 0.001, 0).getBlock().getType() != Material.AIR; // NOTE: this is inaccurate if shifting to edge; works fine for other cases
                                long lastStateChange = vars.getTimeSinceLastStateChange();
                                if((lastStateChange > 500 && isPlayerOnGround) // fast path
                                        || lastStateChange > clutchSecs) {
                                    cancel();
                                    // win!
                                    vars.setState(State.Winning);
                                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                                    int gainedXp = (info.locSettings.doubleHit ? 2 : 1)*((difficulty*2)+3);
                                    player.sendMessage("§aYou clutched! §b+"+gainedXp+"xp");
                                    Bridge.sendTitle(player, "", "§aYou clutched! §b+"+gainedXp+"xp", 5, 5, 25);
                                    vars.xpGained += gainedXp;
                                    vars.tasks.add((new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            nextGameMode(player, vars, info);
                                        }
                                    }).runTaskLater(Bridge.instance, 25));
                                    return;
                                }
                                String secs = Bridge.prettifyNumber((float) (clutchSecs - vars.getTimeSinceLastStateChange()));
                                Bridge.sendActionBar(player, "§eTime Remaining: §f"+secs);
                                board.getTeam("info").setSuffix("§a"+secs);
                            }
                        }).runTaskTimer(Bridge.instance, 0, 5));
                    }
                }, true);
        info.locSettings.npcId = vars.npc.npc.getId();
        info.locSettings.onNpcHit = (i) -> {
            player.sendMessage("§cYou cannot hit the bot in this mode!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1);
        };
        vars.npc.healthScore.setScore(999);
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 0.3f, 1);
        player.setGameMode(GameMode.SURVIVAL);
    }
    private void reset(Player player, Variables vars, PlayerInfo info) {
        Bridge.sendTitle(player, "", "", 0, 0, 0);
        vars.spawn.remove();
        vars.bridge.remove();
        if(vars.npc != null) {
            vars.npc.remove();
        }
        for(BukkitTask task : vars.tasks) {
            if(task != null) {
                task.cancel();
            }
        }
        vars.tasks.clear();
        for(Location loc : info.changedBlocks) {
            loc.getBlock().setType(Material.AIR);
        }
        info.changedBlocks.clear();
    }
    private void nextAttempt(Player player, Variables vars) {
        vars.attempt++;
        updateAttempt(player, vars);
    }
    private void updateAttempt(Player player, Variables vars) {
        player.getScoreboard().getTeam("attempt").setSuffix("§a" + vars.attempt + "§7/" + vars.maxAttempts);
    }

    static class Variables {
        Structure spawn;
        Structure bridge;
        NPC npc;
        State state = State.WaitingForWalkOnGold;
        Location bridgeLocation;
        long lastStateChange = 0;
        int blocksPlaced = 0;
        int xpGained = 0;
        int attempt = 0;
        int maxAttempts = 5;
        MiniMode curGameMode = MiniMode.Flat;

        // all tasks are canceled before the next gamemode or on leave
        ArrayList<BukkitTask> tasks = new ArrayList<>();

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
        BotChaseClutching,
        Dying,
        Winning,
    }
    enum MiniMode {
        Flat,
        Developed,
        Bypass1,
        Bypass2
    }
    public static void selectDifficulty(Player player, int num) {
        // this is unreadable, but is the most efficient way
        // this method makes the clay block at index `num`+1 enchanted
        PlayerInventory inv = player.getInventory();
        Team difficultyTeam = player.getScoreboard().getTeam("difficulty");
        if(num == -1) {
            inv.setItem(0, Bridge.getEnchanted(difficultyItemEasy.clone()));
            inv.setItem(1, difficultyItemNormal);
            inv.setItem(2, difficultyItemHard);
            difficultyTeam.setSuffix("§aEasy");
        } else if(num == 0) {
            inv.setItem(0, difficultyItemEasy);
            inv.setItem(1, Bridge.getEnchanted(difficultyItemNormal.clone()));
            inv.setItem(2, difficultyItemHard);
            difficultyTeam.setSuffix("§eNormal");
        } else if(num == 1) {
            inv.setItem(0, difficultyItemEasy);
            inv.setItem(1, difficultyItemNormal);
            inv.setItem(2, Bridge.getEnchanted(difficultyItemHard.clone()));
            difficultyTeam.setSuffix("§c§lHard");
        }
    }
}
