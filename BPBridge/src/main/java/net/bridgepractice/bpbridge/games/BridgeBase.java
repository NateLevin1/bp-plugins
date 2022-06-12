package net.bridgepractice.bpbridge.games;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.bridgepractice.bpbridge.*;
import net.bridgepractice.bpbridge.bridgemodifiers.BridgeModifier;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BridgeBase extends Game {
    ArrayList<Player> redTeamPlayers = new ArrayList<>();
    ArrayList<Player> blueTeamPlayers = new ArrayList<>();
    ArrayList<OfflinePlayer> allRedPlayersPossiblyOnline = new ArrayList<>();
    ArrayList<OfflinePlayer> allBluePlayersPossiblyOnline = new ArrayList<>();
    HashMap<UUID, Integer> playerKills = new HashMap<>();
    HashMap<UUID, BukkitTask> protectedPlayers = new HashMap<>();
    HashMap<UUID, Integer> playerGoals = new HashMap<>();
    HashMap<UUID, Player> lastHits = new HashMap<>();
    HashMap<UUID, Integer> currentWinstreaks = new HashMap<>();
    HashMap<UUID, Integer> allTimeWinstreaks = new HashMap<>();
    private final Set<Location> blocksPlaced = new HashSet<>();
    int redGoals;
    int blueGoals;
    Rectangle redGoal;
    Rectangle blueGoal;
    int desiredPlayersPerTeam = 1;
    String formattedDate;
    Location redSpawnLoc;
    Location blueSpawnLoc;
    Structure redCage;
    Structure blueCage;
    long startTime;
    BukkitTask timeUpdater;
    BukkitTask startTimer = null;
    BukkitTask countdownTimer = null;
    Rectangle blockPlaceableRect;
    BridgeModifier bridgeModifier;
    public BridgeBase(World world, String map, boolean shouldCountAsStats, Player player, BridgeModifier bridgeModifier) {
        super(bridgeModifier.getGameType(), world, map, shouldCountAsStats);
        this.bridgeModifier = bridgeModifier;
        redGoal = Maps.getRedGoal(map);
        blueGoal = Maps.getBlueGoal(map);
        blockPlaceableRect = Maps.getBlocksPlaceableRect(map);
        formattedDate = new SimpleDateFormat("MM/dd/yy").format(new Date(System.currentTimeMillis()));
        redSpawnLoc = Maps.getRedSpawnLoc(map, world);
        blueSpawnLoc = Maps.getBlueSpawnLoc(map, world);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(Maps.getTimeOfMap(map));
        world.setGameRuleValue("randomTickSpeed", "0");
        setAmountOfBlocks(bridgeModifier.getAmountOfBlocks());
        setAmountOfGaps(bridgeModifier.getAmountOfGaps());
        onPlayerJoin(player);
    }

    public void onPlayerJoinImpl(Player player) {
        // FIXME: remove this try/catch
        try {
            player.teleport(redSpawnLoc);
            player.getInventory().setHeldItemSlot(0);
            loadPlayerSidebar(player);
            if(redTeamPlayers.size() < desiredPlayersPerTeam) {
                if(redTeamPlayers.size() == 0 && bridgeModifier.shouldUseCages()) {
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            Cage playerCage = Utils.getCageSync(player);
                            redCage = new Structure(Utils.cages[playerCage.ordinal()].getRed());
                        }
                    }).runTaskAsynchronously(BPBridge.instance);
                }
                redTeamPlayers.add(player);
                allRedPlayersPossiblyOnline.add(player);
            } else if(blueTeamPlayers.size() < desiredPlayersPerTeam) {
                if(blueTeamPlayers.size() == 0 && bridgeModifier.shouldUseCages()) {
                    (new BukkitRunnable() {
                        @Override
                        public void run() {
                            Cage playerCage = Utils.getCageSync(player);
                            blueCage = new Structure(Utils.cages[playerCage.ordinal()].getBlue());
                        }
                    }).runTaskAsynchronously(BPBridge.instance);
                }
                blueTeamPlayers.add(player);
                allBluePlayersPossiblyOnline.add(player);
            } else {
                // UH OH!
                BPBridge.instance.removeFromQueueable(world.getName(), gameType);
                player.sendMessage("§c§lUh Oh!§c Something went wrong sending you to that server! (Queued a game that had already started)");
                BPBridge.connectPlayerToLobby(player);
                return;
            }
            player.getInventory().clear();
            ItemStack empty = new ItemStack(Material.AIR);
            player.getInventory().setHelmet(empty);
            player.getInventory().setChestplate(empty);
            player.getInventory().setLeggings(empty);
            player.getInventory().setBoots(empty);
            player.setAllowFlight(false);
            player.setGameMode(GameMode.ADVENTURE);
            // clear potion effects
            for(PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.getInventory().setItem(8, Utils.makeItem(Material.BED, "§c§lReturn to Lobby §7(Right Click)", "§7Right-click to leave to the lobby!"));
            loadPlayerHotbar(player, redTeamPlayers.contains(player));
            Scoreboard board = Utils.createScoreboard("§b§lBridge §c§lPractice", new String[]{
                    "%top%§7" + formattedDate + "% §8" + world.getName().substring(world.getName().indexOf("-") + 1).substring(0, 5),
                    "",
                    "§fMap: §a" + Maps.humanReadableMapName(map),
                    "%players%§fPlayers: ",
                    "",
                    "%timer%§fWaiting...",
                    "",
                    "§ebridgepractice.net"
            });
            Team hidden = board.registerNewTeam("hidden");
            hidden.setPrefix("§7§k");
            player.setScoreboard(board);
            player.sendMessage("\n");
            String joinMessage = "§7§k" + StringUtils.repeat("x", player.getName().length()) + "§e has joined (§b" + allPlayers.size() + "§e/§b" + (desiredPlayersPerTeam * 2) + "§e)!";
            for(Player p : allPlayers) {
                if(!p.isOnline()) {
                    Utils.sendDebugErrorWebhook("P "+p.getName()+"was offline when looping through all players in onPlayerJoinImpl. "+Utils.getGameDebugInfo(world.getName()));
                }
                hidden.addEntry(p.getName());
                Team hiddenTeam = p.getScoreboard()
                        .getTeam("hidden");
                if(hiddenTeam == null) {
                    Utils.sendDebugErrorWebhook("Hidden team is null for "+player.getName()+" scoreboard="+p.getScoreboard().getTeams()+Utils.getGameDebugInfo(world.getName()));
                }
                hiddenTeam.addEntry(player.getName());
                p.sendMessage(joinMessage);
                // if we dont hide then show the player then players randomly turn invisible. still don't know why
                p.hidePlayer(player);
                p.showPlayer(player);
            }
            for(Player p : allPlayers) {
                p.getScoreboard().getTeam("players").setSuffix("§a" + allPlayers.size() + "/" + (desiredPlayersPerTeam * 2));
            }
            if(redTeamPlayers.size() == desiredPlayersPerTeam && blueTeamPlayers.size() == desiredPlayersPerTeam) {
                // teams are full, lets remove from queuing games and get it started!
                BPBridge.instance.removeFromQueueable(world.getName(), gameType);

                startTimer = (new BukkitRunnable() {
                    private int times = 0;
                    @Override
                    public void run() {
                        if(times > 4) {
                            startTimer = null;
                            start();
                            this.cancel();
                            return;
                        }

                        int secsLeft = 5 - times;

                        for(Player p : allPlayers) {
                            Team timerTeam = p.getScoreboard().getTeam("timer");
                            if(timerTeam != null) {
                                timerTeam.setPrefix("§fStarting in ");
                                timerTeam.setSuffix("§a" + secsLeft + "s");
                            }
                            p.sendMessage("§eThe game starts in §c" + secsLeft + " §esecond" + (secsLeft == 1 ? "" : "s") + "!");
                            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1f, 1);
                            Utils.sendTitle(p, "§" + (secsLeft > 3 ? "e" : "c") + secsLeft, "", 0, 0, 25);
                        }

                        times++;
                    }
                }).runTaskTimer(BPBridge.instance, 0, 20);
            }
        } catch(Exception e) {
            Utils.sendDebugErrorWebhook("Exception in onPlayerJoinImpl: ", e);
            throw e;
        }

        // NoBridge Stuff Here, (Badly-coded)
        // When player joins, remove the bridge
        if (gameType.equals("nobridge")) {
            // double x = 0;
            double y = 92;
            double z = 0; // z-axis is useless

            while (y >= 84) {
                // Positive
                for (double x = 0; x <= 20; x++) {
                    Location checker = new Location(player.getWorld(), x, y, z);
                    if (checker.getBlock().getType() == Material.STAINED_CLAY) checker.getBlock().setType(Material.AIR);
                }
                // Negative
                for (double x = 0; x >= -20; x--) {
                    Location checker = new Location(player.getWorld(), x, y, z);
                    if (checker.getBlock().getType() == Material.STAINED_CLAY) checker.getBlock().setType(Material.AIR);
                }
                y--;
            }
        }
    }
    public void startImpl() {
        bridgeModifier.onBeforeStart(this);

        Location redPlayerSpawnCageLoc = redSpawnLoc.clone();
        Location bluePlayerSpawnCageLoc = blueSpawnLoc.clone();

        if(bridgeModifier.shouldUseCages()) {
            placeCages();
            redPlayerSpawnCageLoc.setY(Maps.getHeightOfMap(map) + 6.1);
            bluePlayerSpawnCageLoc.setY(Maps.getHeightOfMap(map) + 6.1);
        }

        String blueTeamFormatted = blueTeamPlayers.stream().map(Utils::getRankedName).collect(Collectors.joining(", "));
        String redTeamFormatted = redTeamPlayers.stream().map(Utils::getRankedName).collect(Collectors.joining(", "));

        for(Player player : redTeamPlayers) {
            player.teleport(redPlayerSpawnCageLoc);
            player.setCustomName("§c" + player.getName());
            player.sendMessage("§a§l" + dashes);
            bridgeModifier.sendIntroMessage(player);
            player.sendMessage("\n§f§l          Opponent: " + blueTeamFormatted);
            player.sendMessage("\n§a§l" + dashes);
        }


        for(Player player : blueTeamPlayers) {
            player.teleport(bluePlayerSpawnCageLoc);
            player.setCustomName("§9" + player.getName());
            player.sendMessage("§a§l" + dashes);
            bridgeModifier.sendIntroMessage(player);
            player.sendMessage("\n§f§l          Opponent: " + redTeamFormatted);
            player.sendMessage("\n§a§l" + dashes);
        }

        for(Player player : allPlayers) {
            PlayerInventory currentPlayerInv = player.getInventory();
            boolean isOnRed = redTeamPlayers.contains(player);
            currentPlayerInv.setBoots(Utils.getBoots(isOnRed));
            currentPlayerInv.setLeggings(Utils.getLeggings(isOnRed));
            currentPlayerInv.setChestplate(Utils.getChestplate(isOnRed));

            Scoreboard board = Utils.createScoreboard("§b§lBridge §c§lPractice", new String[]{
                    "%top%§7" + formattedDate + "% §8" + world.getName().substring(world.getName().indexOf("-") + 1).substring(0, 8),
                    "",
                    "%time%§fTime Left: %§a" + bridgeModifier.getGameLengthMinutes() + ":00",
                    "",
                    "%red%§c[R] %§7⬤⬤⬤⬤⬤",
                    "%blue%§9[B] %§7⬤⬤⬤⬤⬤",
                    "",
                    "%kills%§fKills: %§a0",
                    bridgeModifier.getCustomStatistic(),
                    "",
                    "§fMode: §a" + bridgeModifier.getPrettyGameType(),
                    (gameType.equals("nobridge")) ? "%cws%§fWinstreak: %§cNONE" : "%cws%§fWinstreak: %§a" + currentWinstreaks.getOrDefault(player.getUniqueId(), 0),
                    (gameType.equals("nobridge")) ? "%bws%§fBest Winstreak%§f: §cNONE" : "%bws%§fBest Winstreak%§f: §a" + allTimeWinstreaks.getOrDefault(player.getUniqueId(), 0),
                    "",
                    "§ebridgepractice.net"
            });
            Team redTeam = board.registerNewTeam("red_color");
            redTeam.setPrefix("§c");
            Team blueTeam = board.registerNewTeam("blue_color");
            blueTeam.setPrefix("§9");

            Objective healthObj = board.registerNewObjective("__HEALTH__", "dummy");
            healthObj.setDisplaySlot(DisplaySlot.BELOW_NAME);
            healthObj.setDisplayName("§c" + Utils.hearts(1));

            Objective tabHealthObj = board.registerNewObjective("__TAB_HEALTH__", "dummy");
            tabHealthObj.setDisplaySlot(DisplaySlot.PLAYER_LIST);

            for(Player p : allPlayers) {
                healthObj.getScore(p.getName()).setScore(20);
                tabHealthObj.getScore(p.getName()).setScore(20);
            }

            for(Player p : redTeamPlayers) {
                redTeam.addEntry(p.getName());
            }
            for(Player p : blueTeamPlayers) {
                blueTeam.addEntry(p.getName());
            }
            player.setScoreboard(board);
        }

        startTime = System.currentTimeMillis();
        timeUpdater = (new BukkitRunnable() {
            @Override
            public void run() {
                long time = ((long) bridgeModifier.getGameLengthMinutes() * 60 * 1000) - (System.currentTimeMillis() - startTime);
                long minutes = time / (60 * 1000);
                long seconds = (time / 1000) % 60;
                String formatted = String.format("%d:%02d", minutes, seconds);
                for(Player player : allPlayers) {
                    Team timeTeam = player.getScoreboard().getTeam("time");
                    if(timeTeam != null)
                        timeTeam.setSuffix("§a" + formatted);
                }
                if(time <= 0) {
                    cancel();
                    try {
                        if(redGoals == blueGoals) {
                            onWin("draw", "f");
                        } else if(redGoals > blueGoals) {
                            onWin("red", "c");
                        } else {
                            onWin("blue", "9");
                        }
                    } catch(Exception e) {
                        Utils.sendDebugErrorWebhook("Error in timeUpdate runnable calling onWin!", e);
                    }
                }
            }
        }).runTaskTimer(BPBridge.instance, 20, 20);

        startCountdown("");
    }

    private void startCountdown(String titleText) {
        boolean useCages = bridgeModifier.shouldUseCages();
        countdownTimer = (new BukkitRunnable() {
            private int times = 0;
            @Override
            public void run() {
                if(times >= bridgeModifier.getCountdownTime()) {
                    countdownTimer = null;
                    if(useCages) {
                        redCage.remove();
                        blueCage.remove();
                    }
                    for(Player p : allPlayers) {
                        Utils.sendTitle(p, "", "§aFight!", 0, 10, 20);
                        p.setGameMode(GameMode.SURVIVAL);
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1, 1f);
                    }
                    this.cancel();
                    return;
                }

                int secsLeft = bridgeModifier.getCountdownTime() - times;

                for(Player p : allPlayers) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1f, 1);
                    Utils.sendTitle(p, titleText, "§7" + (useCages ? "Cages open" : "Fight") + " in §a" + secsLeft + "s§7...", 0, 0, 25);
                }

                times++;
            }
        }).runTaskTimer(BPBridge.instance, 0, 20);
    }
    private void placeCages() {
        Location redCageLoc = redSpawnLoc.clone();
        Location blueCageLoc = blueSpawnLoc.clone();
        redCageLoc.setY(Maps.getHeightOfMap(map));
        blueCageLoc.setY(Maps.getHeightOfMap(map));
        redCageLoc.subtract(3, 0, 4);
        blueCageLoc.subtract(3, 0, 4);

        long startTime = System.currentTimeMillis();
        redCage.batchedPlace(redCageLoc, blueCage, blueCageLoc);
        BPBridge.instance.getLogger().info("placed cages; took " + (System.currentTimeMillis() - startTime) + "ms");
    }
    private void loadPlayerSidebar(Player player) {
        if (gameType.equals("nobridge")) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = BPBridge.connection.prepareStatement("SELECT " + gameType + "CurrentWinStreak, " + gameType + "AllTimeWinStreak FROM players WHERE uuid=?;")) {
                    statement.setString(1, player.getUniqueId().toString()); // uuid
                    ResultSet res = statement.executeQuery();
                    if(!res.next()) {
                        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
                    }
                    currentWinstreaks.put(player.getUniqueId(), res.getInt(1));
                    allTimeWinstreaks.put(player.getUniqueId(), res.getInt(2));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).runTaskAsynchronously(BPBridge.instance);
    }

    public void onPlayerDeath(Player player) {
        player.closeInventory();
        if(!isPlaying()) {
            player.teleport(redSpawnLoc);
        } else {
            player.setHealth(20);

            Player killer = lastHits.get(player.getUniqueId());
            if(killer != null) {
                for(Player p : allPlayers) {
                    p.sendMessage("§c" + player.getCustomName() + "§7 was killed by §9" + killer.getCustomName() + "§7.");
                }

                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 1, 1);
                incrementKills(killer);
                killer.getScoreboard().getTeam("kills").setSuffix("§a" + getKills(killer));
                lastHits.remove(player.getUniqueId());
            } else {
                for(Player p : allPlayers) {
                    p.sendMessage("§c" + player.getCustomName() + "§7 fell into the void.");
                }
            }


            protectedPlayers.put(player.getUniqueId(), (new BukkitRunnable() {
                @Override
                public void run() {
                    protectedPlayers.remove(player.getUniqueId());
                }
            }).runTaskLater(BPBridge.instance, 2 * 20));

            if(redTeamPlayers.contains(player)) {
                player.teleport(redSpawnLoc);
            } else {
                player.teleport(blueSpawnLoc);
            }

            player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);

            if(bridgeModifier.shouldResetPlayerOnDeath()) {
                resetPlayer(player);
            }
            onPlayerHealthChange(player);

            bridgeModifier.onPlayerKilledByPlayer(player, killer, this);
        }
    }
    public int getKills(Player player) {
        return playerKills.getOrDefault(player.getUniqueId(), 0);
    }
    public Player getMemberOfTeam(String team) {
        if(team.equals("red")) {
            return redTeamPlayers.get(0);
        } else {
            return blueTeamPlayers.get(0);
        }
    }
    public void incrementKills(Player player) {
        playerKills.put(player.getUniqueId(), getKills(player) + 1);
    }
    public void onPlayerHealthChange(Player playerWhoseHealthChanged) {
        onPlayerHealthChange(playerWhoseHealthChanged, (int) (Math.round(playerWhoseHealthChanged.getHealth()) + ((CraftPlayer) playerWhoseHealthChanged).getHandle().getAbsorptionHearts()));
    }
    public void onPlayerHealthChange(Player playerWhoseHealthChanged, int newValue) {
        for(Player player : allPlayers) {
            Objective health = player.getScoreboard().getObjective("__HEALTH__");
            Objective tabHealth = player.getScoreboard().getObjective("__TAB_HEALTH__");
            if(health == null) {
                health = player.getScoreboard().registerNewObjective("__HEALTH__", "dummy");
                health.setDisplaySlot(DisplaySlot.BELOW_NAME);
                health.setDisplayName("§c" + Utils.hearts(1));
            }
            if(tabHealth == null) {
                tabHealth = player.getScoreboard().registerNewObjective("__TAB_HEALTH__", "dummy");
                tabHealth.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            }
            health.getScore(playerWhoseHealthChanged.getName()).setScore(newValue);
            tabHealth.getScore(playerWhoseHealthChanged.getName()).setScore(newValue);
        }
    }
    public void onPlayerHitByPlayer(Player hit, Player hitter, double damage) {
        if(allPlayers.contains(hitter)) {
            // if the player that hit someone has spawn prot, remove it - like in bedwars
            if(canPlayerTakeDamage(hitter)) {
                setPlayerUnprotected(hitter);
            }
            lastHits.put(hit.getUniqueId(), hitter);
            bridgeModifier.onPlayerHitByPlayer(hit, hitter, damage);
        }
    }
    private void showFireworks(String team) {
        (new BukkitRunnable() {
            int times = 0;
            @Override
            public void run() {
                if(times == bridgeModifier.getCountdownTime()) {
                    this.cancel();
                    return;
                }
                Firework fw = (Firework) world.spawnEntity(new Location(world, 0, 98 + ThreadLocalRandom.current().nextInt(-1, 6), (times % 2 == 0 ? 2 : -2) + ThreadLocalRandom.current().nextInt(-2, 3)), EntityType.FIREWORK);
                FireworkMeta fwm = fw.getFireworkMeta();

                fwm.setPower(5);
                fwm.addEffect(FireworkEffect.builder().withColor(team.equals("blue") ? Color.BLUE : Color.RED).withFade(Color.WHITE).flicker(true).with(FireworkEffect.Type.values()[times % 5]).build());

                fw.setFireworkMeta(fwm);
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        fw.detonate();
                    }
                }).runTaskLater(BPBridge.instance, 2);

                times++;
            }
        }).runTaskTimer(BPBridge.instance, 0, 20);
    }
    private long lastGoal = System.currentTimeMillis();
    public void onPlayerScore(Player player, String team) {
        Location redPlayerSpawnCageLoc = redSpawnLoc.clone();
        Location bluePlayerSpawnCageLoc = blueSpawnLoc.clone();
        if(bridgeModifier.shouldUseCages()) {
            redPlayerSpawnCageLoc.setY(Maps.getHeightOfMap(map) + 6.1);
            bluePlayerSpawnCageLoc.setY(Maps.getHeightOfMap(map) + 6.1);
        }

        // For some _ungodly reason_, spigot decides to just *not* teleport the player sometimes.
        // The solution to this is to detect when this happens by checking `lastGoal` and teleporting
        // the player back if it did happen
        // This was put into place to solve the "double-scoring" problem
        // An alternative solution seems to be to run the teleports one tick after they happen, but that solution
        // seems worse since it will run regardless of whether the teleports happen
        if(System.currentTimeMillis() - lastGoal < bridgeModifier.getCountdownTime() * 1000L) {
            if(team.equals("red")) {
                player.teleport(redPlayerSpawnCageLoc);
            } else {
                player.teleport(bluePlayerSpawnCageLoc);
            }
            return;
        }
        lastGoal = System.currentTimeMillis();

        if(team.equals("blue")) {
            blueGoals++;
        } else {
            redGoals++;
        }
        playerGoals.put(player.getUniqueId(), playerGoals.getOrDefault(player.getUniqueId(), 0) + 1);
        Team goalsTeam = player.getScoreboard().getTeam("goals");
        if(goalsTeam != null) {
            goalsTeam.setSuffix("§a" + playerGoals.get(player.getUniqueId()));
        }

        int teamGoals = (team.equals("blue") ? blueGoals : redGoals);
        String teamColor = team.equals("blue") ? "9" : "c";
        String suffix = "§" + teamColor + StringUtils.repeat("⬤", teamGoals);
        String content = "\n                " + "§" + teamColor + "§l" + player.getName() + " §7(§a" + String.format("%.1f", player.getHealth() + ((CraftPlayer) player).getHandle().getAbsorptionHearts()).replace(".0", "") + "§c" + Utils.hearts(1) + "§7) §escored! §7(§6" + (Utils.ordinal(playerGoals.get(player.getUniqueId()))) + " " + bridgeModifier.getNameForScore() + "§7)" +
                "\n                                " + (team.equals("blue") ? "§9§l" + blueGoals : "§c§l" + redGoals) + " §7§l- " + (team.equals("blue") ? "§c§l" + redGoals : "§9§l" + blueGoals);

        String start = "§6" + dashes;
        String end = "\n§6" + dashes;

        if(5 - teamGoals > 0) {
            suffix += "§7" + StringUtils.repeat("⬤", 5 - teamGoals);
        } else {
            // a team won
            for(Player p : allPlayers) {
                p.sendMessage(start);
                p.sendMessage(content);
                p.sendMessage(end);

                Team teamTeam = p.getScoreboard().getTeam(team);
                if(teamTeam != null) {
                    teamTeam.setSuffix(suffix);
                }
            }
            onWin(team, teamColor);
            return;
        }

        showFireworks(team);

        for(Player p : allPlayers) {
            p.sendMessage(start);
            p.sendMessage(content);
            p.sendMessage(end);

            Team teamTeam = p.getScoreboard().getTeam(team);
            if(teamTeam != null) {
                teamTeam.setSuffix(suffix);
            }

            p.setGameMode(GameMode.ADVENTURE);
            resetPlayer(p);

            p.setHealth(20);
            lastHits.remove(player.getUniqueId());
        }

        if(bridgeModifier.shouldUseCages()) {
            // teleport into cages
            for(Player p : redTeamPlayers) {
                p.teleport(redPlayerSpawnCageLoc.clone().add(0, 2, 0));
            }
            for(Player p : blueTeamPlayers) {
                p.teleport(bluePlayerSpawnCageLoc.clone().add(0, 2, 0));
            }

            placeCages();
        }

        // teleport into correct place
        for(Player p : redTeamPlayers) {
            p.teleport(redPlayerSpawnCageLoc);
        }
        for(Player p : blueTeamPlayers) {
            p.teleport(bluePlayerSpawnCageLoc);
        }

        // reset players
        startCountdown(player.getCustomName() + " scored!");
    }
    private void onWin(String team, String teamColor) {
        state = State.Finished;
        timeUpdater.cancel();
        long time = System.currentTimeMillis() - startTime;
        long minutes = time / (60 * 1000);
        long seconds = (time / 1000) % 60;
        String formattedTime = String.format("%d:%02d", minutes, seconds);
        /*
              This is what I have designed the win message to look like
                §a--------------------------------------------------
                §a--------------
                §9██
                §9██                  §eNAME Duel
                §9██                       §602:17
                §f██
                §f██                    §b§lBlue Wins!
                §c██                      §9§l5 §7§l- §c§l4
                §c██              §fTop Killer §7§l- §cByeParihs §6§l[11]
                §c██
                §a--------------------------------------------------
                §a--------------
                One idea for cosmetics later is to be able to purchase certain pixel arts to go there when the game ends
             */
        String score = (team.equals("blue") ? "§9§l" + blueGoals : "§c§l" + redGoals) + " §7§l- " + (team.equals("blue") ? "§c§l" + redGoals : "§9§l" + blueGoals);
        Comparator<Map.Entry<UUID, Integer>> compare = Map.Entry.comparingByValue();
        List<Map.Entry<UUID, Integer>> playersWithKills = playerKills.entrySet().stream().sorted(compare.reversed()).collect(Collectors.toList());
        Map.Entry<UUID, Integer> topKiller = playersWithKills.size() == 0 ? null : playersWithKills.get(0);
        String nameOfTopKiller = topKiller == null ? allPlayers.get(0).getName() : BPBridge.instance.getServer().getOfflinePlayer(topKiller.getKey()).getName();
        int topKillerKills = topKiller == null ? 0 : topKiller.getValue();
        boolean topKillerOnBlue = false;
        for(Player p : blueTeamPlayers) {
            if(p.getName().equals(nameOfTopKiller)) {
                topKillerOnBlue = true;
            }
        }
        String winMessage = "§" + teamColor + "██" +
                "\n§" + teamColor + "██" + "                 §e" + Utils.qualifyGameType(gameType) + " Duel" +
                "\n§" + teamColor + "██" + "                       §6" + formattedTime +
                "\n§" + teamColor + "██" +
                "\n§" + teamColor + "██" + "                   §" + (team.equals("draw") ? "f§lTie Game!" : (team.equals("blue") ? "b§lBlue" : ("c§lRed")) + " Wins!") +
                "\n§" + teamColor + "██" + "                      " + score +
                "\n§" + teamColor + "██" + "            §fTop Killer §7§l- " + (topKillerOnBlue ? "§9" : "§c") + nameOfTopKiller + " §6§l[" + topKillerKills + "]" +
                "\n§" + teamColor + "██";

        for(Player p : allPlayers) {
            Utils.sendTitle(p, team.equals("draw") ? "§fIt's a Draw!" : ("§" + (team.equals("blue") ? "9BLUE" : "cRED") + " WINS!"), score, 0, 10, 5 * 20);

            p.sendMessage("§a§l" + dashes);
            p.sendMessage(winMessage);
            p.sendMessage("§a§l" + dashes);

            if(!shouldCountAsStats) {
                p.sendMessage("§cYour stats didn't change because you /duel'ed your opponent!");
            }

            p.setHealth(20);

            // remove arrow recharges
            resetArrowRecharge(p);
        }

        showFireworks(team);

        if(countdownTimer != null) {
            countdownTimer.cancel();
            if(bridgeModifier.shouldUseCages()) {
                redCage.remove();
                blueCage.remove();
            }
        }


        for(int i = 0; i < allRedPlayersPossiblyOnline.size(); i++) {
            OfflinePlayer player = allRedPlayersPossiblyOnline.get(i);
            if(team.equals("red")) {
                givePlayerWin(player);

                // send webhook
                sendWinstreakWebhook(player, allBluePlayersPossiblyOnline.get(i), formattedTime);
            } else if(!team.equals("draw")) {
                givePlayerLoss(player);
            }
        }

        for(int i = 0; i < allBluePlayersPossiblyOnline.size(); i++) {
            OfflinePlayer player = allBluePlayersPossiblyOnline.get(i);
            if(team.equals("blue")) {
                givePlayerWin(player);

                // send webhook
                sendWinstreakWebhook(player, allRedPlayersPossiblyOnline.get(i), formattedTime);
            } else if(!team.equals("draw")) {
                givePlayerLoss(player);
            }
        }

        for(Player player : redTeamPlayers) {
            player.teleport(redSpawnLoc);
            if(team.equals("blue") || team.equals("draw")) {
                makePlayerSpec(player);
            }
            givePlayerEndOptions(player);
        }
        for(Player player : blueTeamPlayers) {
            player.teleport(blueSpawnLoc);
            if(team.equals("red") || team.equals("draw")) {
                makePlayerSpec(player);
            }
            givePlayerEndOptions(player);
        }

        (new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : allPlayers) {
                    Utils.sendTitle(player, "§cGAME OVER", "", 0, 10, 3 * 20);
                    makePlayerSpec(player);
                }
            }
        }).runTaskLater(BPBridge.instance, 8 * 20);
        (new BukkitRunnable() {
            @Override
            public void run() {
                endGame();
            }
        }).runTaskLater(BPBridge.instance, 10 * 20);
    }
    private void givePlayerWin(OfflinePlayer player) {
        if(!shouldCountAsStats || gameType.equals("nobridge")) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = BPBridge.connection.prepareStatement("UPDATE players SET " + gameType + "AllTimeWinStreak = CASE WHEN " + gameType + "AllTimeWinStreak <= " + gameType + "CurrentWinStreak THEN " + gameType + "AllTimeWinStreak + 1 ELSE " + gameType + "AllTimeWinStreak END, " + gameType + "CurrentWinStreak = " + gameType + "CurrentWinStreak + 1, " + gameType + "Wins = " + gameType + "Wins + 1, xp = xp + 100 WHERE uuid=?;")) {
                    // increase current winstreak, if all time is equal to current, update all time too, also increase XP
                    statement.setString(1, player.getUniqueId().toString()); // uuid, set to player uuid
                    statement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    if(player.isOnline()) {
                        ((Player) player).sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and screenshot your current win streak!");
                    }
                }
            }
        }).runTaskAsynchronously(BPBridge.instance);
    }
    private void givePlayerLoss(OfflinePlayer player) {
        if(!shouldCountAsStats || gameType.equals("nobridge")) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                try(PreparedStatement statement = BPBridge.connection.prepareStatement("UPDATE players SET " + gameType + "CurrentWinStreak = 0, " + gameType + "Losses = " + gameType + "Losses + 1, xp = xp + 25 WHERE uuid=?;")) {
                    // set current winstreak to 0, add a little xp
                    statement.setString(1, player.getUniqueId().toString()); // uuid, set to player uuid
                    statement.executeUpdate();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    if(player.isOnline()) {
                        ((Player) player).sendMessage("§c§lUh oh!§r§c Something went wrong syncing your information to our database. Please open a ticket on the discord and tell us what happened!");
                    }
                }
            }
        }).runTaskAsynchronously(BPBridge.instance);
    }
    public void sendWinstreakWebhook(OfflinePlayer winningPlayer, OfflinePlayer losingPlayer, String formattedTime) {
        if(!shouldCountAsStats) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                // send a winstreak log message in the discord - this is so winstreak audits are possible
                JsonObject webhook = new JsonObject();
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                JsonObject author = new JsonObject();

                webhook.add("embeds", embeds);
                embeds.add(embed);

                embed.addProperty("color", 0x39c2ff);

                embed.add("author", author);
                author.addProperty("name", "Winstreak Change (Mode: " + bridgeModifier.getPrettyGameType() + ")");

                int winningPlayerWs = currentWinstreaks.get(winningPlayer.getUniqueId());
                int losingPlayerWs = (currentWinstreaks.get(losingPlayer.getUniqueId()));
                boolean isOnRed = allRedPlayersPossiblyOnline.contains(winningPlayer);
                int winningTeamScore = isOnRed ? redGoals : blueGoals;
                int losingTeamScore = isOnRed ? blueGoals : redGoals;

                embed.addProperty("title", winningPlayer.getName() + ": " + (winningPlayerWs) + " ⇢ " + (winningPlayerWs + 1) +
                        " | " + losingPlayer.getName() + ": " + losingPlayerWs + " ⇢ 0");

                embed.addProperty("description", winningPlayer.getName() + " beat " + losingPlayer.getName() + "           |           on " + Maps.humanReadableMapName(map) + "\n"
                        + "```pascal\n"
                        + "          " + winningTeamScore + " to " + losingTeamScore + " {" + formattedTime + "}\n"
                        + "```");

                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", "https://minotar.net/armor/bust/" + winningPlayer.getName() + "/64");
                embed.add("thumbnail", thumbnail);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", losingPlayer.getName() + " lost their ws to " + winningPlayer.getName() + " [" + losingPlayerWs + "]\nWorld: " + world.getName());
                embed.add("footer", footer);

                embed.addProperty("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

                Utils.sendWebhookSync(webhook);
            }
        }).runTaskAsynchronously(BPBridge.instance);
    }
    public void onPlayerChat(Player player, String message) {
        if(player.hasPermission("bridgepractice.goldengg")) {
            if(message.equals("gg")) {
                message = "§6"+(player.hasPermission("bridgepractice.boldgg") ? "§l" : "")+"gg";
            }
        }

        // we get all players in the world rather than all players so that spectators can see chat messages too
        for(Player p : player.getWorld().getPlayers()) {
            p.sendMessage((redTeamPlayers.contains(player) ? "§c[RED] " : "§9[BLUE] ")+Utils.getRankedName(player) + "§f: §f" + message);
        }
    }
    private void makePlayerSpec(Player player) {
        PlayerInventory playerInv = player.getInventory();
        ItemStack air = new ItemStack(Material.AIR);
        playerInv.setBoots(air);
        playerInv.setLeggings(air);
        playerInv.setChestplate(air);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
    }
    private void givePlayerEndOptions(Player player) {
        PlayerInventory playerInv = player.getInventory();
        playerInv.clear();
        playerInv.setItem(3, Utils.makeItem(Material.PAPER, "§b§lPlay Again §7(Right Click)", "§7Right-click to play another game!"));
        playerInv.setItem(5, Utils.makeItem(Material.BED, "§c§lReturn to Lobby §7(Right Click)", "§7Right-click to go to the lobby!"));
    }
    public void onPlayerLeaveImpl(Player player) {
        // FIXME: remove this try/catch
        try {
            boolean wasOnRedTeam = redTeamPlayers.remove(player);
            boolean wasOnBlueTeam = blueTeamPlayers.remove(player);

            if(isPlaying()) {
                // here we tell players someone left and give the appropriate team the win
                String leaveMessage = player.getCustomName() + "§7 left the game.";
                for(Player p : allPlayers) {
                    p.sendMessage(leaveMessage);
                }
                if(wasOnRedTeam && redTeamPlayers.size() == 0) {
                    // blue wins since all the people on red left
                    onWin("blue", "9");
                } else if(wasOnBlueTeam && blueTeamPlayers.size() == 0) {
                    // red wins since all the people on blue left
                    onWin("red", "c");
                }
            } else {
                if(state != State.Finished) {
                    // this is the logic that handles re-queueing and stuff
                    allBluePlayersPossiblyOnline.remove(player);
                    allRedPlayersPossiblyOnline.remove(player);
                    if(allPlayers.size() > 0) {
                        if(startTimer != null) {
                            startTimer.cancel();
                            for(Player p : allPlayers) {
                                p.sendMessage("§cThere are not enough players! Start canceled.");
                            }
                        }
                        for(Player p : allPlayers) {
                            p.sendMessage("§7§k" + player.getName() + "§e has quit!");
                            Utils.sendTitle(p, "§cCANCELLED", "", 0, 5, 3 * 20);
                            p.playSound(p.getLocation(), Sound.CLICK, 1, 1);
                            Scoreboard pScoreboard = p.getScoreboard();
                            Team timerTeam = pScoreboard.getTeam("timer");
                            timerTeam.setPrefix("§fWaiting...");
                            timerTeam.setSuffix("");
                            pScoreboard.getTeam("players").setSuffix("§a" + allPlayers.size() + "/" + (desiredPlayersPerTeam * 2));
                        }
                        BPBridge.instance.sendCreateQueuePluginMessage(allPlayers.get(0), gameType); // we don't use `.createQueue` because that will change the game info
                    } else {
                        if(timeUpdater != null) {
                            timeUpdater.cancel();
                        }
                        // when the game hasn't queued and nobody is left
                        if(!shouldCountAsStats) {
                            endGame();
                            return;
                        }

                        // for some reason, players that are leaving are counted as being online in Bukkit.getOnlinePlayers();
                        // since we can't send a plugin message through a player who is soon going to be offline,
                        // we have to delay a little in removing this game from the proxy's queueable games.
                        // Note: if somebody tries to queue this game during this short time period, they will see an error
                        //       message that says that they tried to queue a non-existent game. This is acceptable behavior.
                        (new BukkitRunnable() {
                            @Override
                            public void run() {
                                BPBridge.instance.removeFromQueueable(world.getName(), gameType);
                            }
                        }).runTaskLater(BPBridge.instance, 5);
                        endGame();
                    }
                }
            }
        } catch(Exception e) {
            Utils.sendDebugErrorWebhook("Exception in onPlayerLeaveImpl: ", e);
            throw e;
        }
    }
    public void onPlayerBowCharge(PlayerInteractEvent event, Player player) {
        if(!bridgeModifier.shouldUseCages() && countdownTimer != null) {
            player.setItemInHand(event.getItem());
            event.setCancelled(true);
            player.sendMessage("§cYou can't shoot your bow right now!");
        }
    }
    public void onPlayerMove(PlayerMoveEvent event, Player player) {
        if(player.getLocation().getY() < 80) {
            onPlayerDeath(player);
            return;
        }
        if(!isPlaying())
            return;
        if(!bridgeModifier.shouldUseCages() && countdownTimer != null) {
            Location loc = redTeamPlayers.contains(player) ? redSpawnLoc.clone() : blueSpawnLoc.clone();
            if(player.getLocation().getX() != loc.getX() || player.getLocation().getZ() != loc.getZ()) {
                loc.setYaw(player.getLocation().getYaw());
                loc.setPitch(player.getLocation().getPitch());
                player.teleport(loc);
            }
            return;
        }
        if(redGoal.isInBounds(player.getLocation())) {
            if(blueTeamPlayers.contains(player)) {
                onPlayerScore(player, "blue");
            } else {
                player.sendMessage("§cYou can't score on your own goal!");
                player.teleport(player.getLocation().subtract(0, 3.8, 0));
            }
        } else if(blueGoal.isInBounds(player.getLocation())) {
            if(redTeamPlayers.contains(player)) {
                onPlayerScore(player, "red");
            } else {
                player.sendMessage("§cYou can't score on your own goal!");
                player.teleport(player.getLocation().subtract(0, 3.8, 0));
            }
        }
    }
    public String getTeamOfPlayer(Player player) {
        return blueTeamPlayers.contains(player) ? "blue" : "red";
    }
    public void onPlayerBlockPlace(BlockPlaceEvent event, Player player) {
        blocksPlaced.add(event.getBlock().getLocation());
    }
    public boolean cannotPlaceBlocks(Location loc, Player player) {
        if(loc.getY() > 99) {
            return true;
        }
        if(map.equals("flora")) {
            // flora is the only map that has a box around its goal. why!?!?
            if(new Rectangle(27, 92, -3, 6, 6, 6).isInBounds(loc) ||
                    new Rectangle(-33, 92, -3, 6, 6, 6).isInBounds(loc) ||
                    new Rectangle(-33, 99, -4, 6, 3, 8).isInBounds(loc) ||
                    new Rectangle(27, 99, -4, 6, 3, 8).isInBounds(loc)) {
                return true;
            }
        }
        return !canPlaceBlocksAtLoc(loc);
    }

    public boolean cannotBreakBlock(Block block, Location loc, Player player) {
        return !(block.getType() == Material.STAINED_CLAY &&
                (block.getData() == DyeColor.RED.getData() || block.getData() == DyeColor.BLUE.getData() || block.getData() == DyeColor.WHITE.getData()) &&
                !cannotPlaceBlocks(loc, player) &&
                ((loc.getX() >= -20 && loc.getX() <= 20) || hasBlockBeenPlaced(loc))
        );
    }
    public boolean canPlaceBlocksAtLoc(Location loc) {
        return blockPlaceableRect.isInBounds(loc);
    }
    public boolean hasBlockBeenPlaced(Location loc) {
        return blocksPlaced.contains(loc);
    }
    public boolean canPlayerTakeDamage(Player player) {
        return protectedPlayers.get(player.getUniqueId()) != null;
    }
    public void setPlayerUnprotected(Player player) {
        BukkitTask task = protectedPlayers.get(player.getUniqueId());
        if(task != null) {
            task.cancel();
        }
        protectedPlayers.remove(player.getUniqueId());
    }
    public void setRedSpawnLoc(Location redSpawnLoc) {
        this.redSpawnLoc = redSpawnLoc;
    }
    public void setBlueSpawnLoc(Location blueSpawnLoc) {
        this.blueSpawnLoc = blueSpawnLoc;
    }
    public String getMap() {
        return map;
    }
    public World getWorld() {
        return world;
    }
    public Set<Location> getBlocksPlaced() {
        return blocksPlaced;
    }
    public void clearBlocksPlaced() {
        blocksPlaced.clear();
    }
    public Location getBlueSpawnLoc() {
        return blueSpawnLoc;
    }
    public Location getRedSpawnLoc() {
        return redSpawnLoc;
    }

    public String toString() {
        String res = bridgeModifier.getPrettyGameType()+" {\n";
        res += " players: {\n";
        res += "  redTeamPlayers: "+redTeamPlayers+"\n";
        res += "  blueTeamPlayers: "+blueTeamPlayers+"\n";
        res += "  allPlayers: "+allPlayers+"\n";
        res += " }\n";
        res += " goals: {\n";
        res += "  redGoals: "+redGoals+"\n";
        res += "  blueGoals: "+blueGoals+"\n";
        res += " }\n";
        res += " bridgeModifier: {\n";
        res += "  getGameType: "+bridgeModifier.getGameType()+"\n";
        res += "  getNameForScore: "+bridgeModifier.getNameForScore()+"\n";
        res += " }\n";
        res += " state: "+state+"\n";
        res += " secsSinceStart: "+((System.currentTimeMillis()-(isQueueing() ? System.currentTimeMillis() : startTime)) / 1000)+"\n";
        res += " worldName: "+world.getName()+"\n";
        res += "}";
        return res;
    }
}
