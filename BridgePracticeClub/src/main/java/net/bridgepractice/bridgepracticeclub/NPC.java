package net.bridgepractice.bridgepracticeclub;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import static net.bridgepractice.bridgepracticeclub.Bridge.*;

public class NPC {
    Player player;
    EntityPlayer npc;
    PlayerConnection connection;
    ItemStack[] armor = {};
    ItemStack itemInHand;
    ItemStack blocks;
    ItemStack sword;
    ItemStack pickaxe;
    ItemStack bow;
    ArrayList<Location> changedBlocks = new ArrayList<>();
    ArrayList<BukkitTask> schedules = new ArrayList<>();
    BukkitRunnable veloRunnable;
    Location respawnLocation;
    Vector velocity = new Vector(0, 0, 0);
    Vector direction = new Vector(0, 0, 0);
    long lastPlayerHit = 0;
    boolean lockToPlayer = true;
    boolean pvp = true;
    long lastHit = 0;
    long lastJump = 0;
    long lastBlockBreak = 0;
    long bowDrawbackTime = -1;
    long timeDisabledAll = -1;
    int bridgeX;
    double health = 20;
    int facingDirection = -1;
    Location npcLoc;
    public boolean isInCage = false;

    enum Goal {
        Start,
        Score,
        Defend,
        AtGoal,
        Void,
    }

    Goal goal = Goal.Start;
    String name;
    org.bukkit.scoreboard.Scoreboard board;
    Score healthScore;
    Score tabHealthScore;
    Score playerHealthScore;
    Score tabPlayerHealthScore;
    ScoreHandler scoreHandler;
    ScoreHandler onWin;
    PutInCages putInCages;

    private final double drag = 0.19;
    private final double gravity = 0.2;
    private final double terminalVelocity = 0.8;

    int playerGoals = 0;
    int npcGoals = 0;
    int npcDeaths = 0;
    NPC(Player target, ScoreHandler scoreHandler, ScoreHandler onWin, PutInCages putInCages) {
        player = target;
        board = target.getScoreboard();
        this.scoreHandler = scoreHandler;
        this.onWin = onWin;
        this.putInCages = putInCages;
        UUID id = UUID.randomUUID();
        int random = getRandom();
        name = names[random];
        GameProfile gameProfile = new GameProfile(id, name); // max 16 characters
        if(skins[random] != null)
            gameProfile.getProperties().put("textures", skins[random]);
        npc = new EntityPlayer(nmsServer, nmsWorld, gameProfile, new PlayerInteractManager(nmsWorld)); // This will be the EntityPlayer (NPC) we send with the sendNPCPacket method.
        connection = ((CraftPlayer) player).getHandle().playerConnection;
    }
    NPC setLocation(Location loc) {
        npc.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        velocity = velocity.zero();
        return this;
    }
    NPC setLocation(float x, float y, float z) {
        this.setLocation(new Location(Bridge.instance.world, x, y, z));
        return this;
    }
    NPC setRespawnLocation(float x, float y, float z) {
        this.respawnLocation = new Location(Bridge.instance.world, x, y, z);
        return this;
    }
    NPC setArmor(ItemStack[] arr) {
        if(arr.length != 4) throw new IllegalArgumentException("Length of armor array must be 4!");
        armor = arr;
        return this;
    }
    NPC setBridge() {
        return setBridge(Color.RED);
    }
    NPC setBridge(Color botColor) {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta bootsM = (LeatherArmorMeta) boots.getItemMeta();
        LeatherArmorMeta leggingsM = (LeatherArmorMeta) leggings.getItemMeta();
        LeatherArmorMeta chestM = (LeatherArmorMeta) chest.getItemMeta();
        bootsM.setColor(botColor);
        leggingsM.setColor(botColor);
        chestM.setColor(botColor);
        boots.setItemMeta(bootsM);
        leggings.setItemMeta(leggingsM);
        chest.setItemMeta(chestM);
        setArmor(new ItemStack[]{boots, leggings, chest, new ItemStack(Material.AIR)});

        ItemStack clay = new ItemStack(Material.STAINED_CLAY, 64);
        clay.setDurability((byte) 14);
        blocks = clay;

        sword = new ItemStack(Material.IRON_SWORD);
        setItemInHand(sword);

        pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        pickaxe.addEnchantment(org.bukkit.enchantments.Enchantment.DIG_SPEED, 2);

        bow = new ItemStack(Material.BOW);

        return this;
    }
    NPC setBridgeX(int bridgeX) {
        this.bridgeX = bridgeX;
        return this;
    }
    NPC setItemInHand(ItemStack item) {
        itemInHand = item;
        connection.sendPacket(new PacketPlayOutEntityEquipment(npc.getId(), 0, CraftItemStack.asNMSCopy(itemInHand)));
        return this;
    }
    NPC showToPlayer() {
        return showToPlayer(true);
    }
    NPC showToPlayer(boolean runVelo) {
        {
            // show stuff, not velo
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
            connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc)); // Spawns the NPC for the player client.

            if(itemInHand != null)
                connection.sendPacket(new PacketPlayOutEntityEquipment(npc.getId(), 0, CraftItemStack.asNMSCopy(itemInHand)));

            if(armor.length != 0) {
                for(int i = 0; i < armor.length; i++) {
                    connection.sendPacket(new PacketPlayOutEntityEquipment(npc.getId(), i + 1, CraftItemStack.asNMSCopy(armor[i])));
                }
            }

            connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) (npc.yaw * 256 / 360))); // Correct head rotation when spawned in player look direction.

            // apply skins
            DataWatcher watcher = npc.getDataWatcher();
            watcher.watch(10, (byte) 0xFF);
            PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(npc.getId(), watcher, true);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);

            // show colored names
            Team redTeam = getOrCreateTeam(board, "__RED__");
            Team blueTeam = getOrCreateTeam(board, "__BLUE__");
            redTeam.setPrefix("§c");
            blueTeam.setPrefix("§9");
            redTeam.addEntry(npc.getName());
            blueTeam.addEntry(player.getName());

            // show health
            Objective healthObj = getOrCreateObjective(board, "__HEALTH__", "dummy");
            healthObj.setDisplaySlot(DisplaySlot.BELOW_NAME);
            healthObj.setDisplayName("§c" + Bridge.hearts(1));
            healthScore = healthObj.getScore(npc.getName());
            playerHealthScore = healthObj.getScore(player.getName());
            healthScore.setScore(20);
            playerHealthScore.setScore(20);

            Objective tabHealthObj = getOrCreateObjective(board, "__TAB_HEALTH__", "dummy");
            tabHealthObj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            tabHealthScore = tabHealthObj.getScore(npc.getName());
            tabPlayerHealthScore = tabHealthObj.getScore(player.getName());
            tabHealthScore.setScore(20);
            tabPlayerHealthScore.setScore(20);
        }

        if(!runVelo) {
            npcLoc = new Location(Bridge.instance.world, npc.locX, npc.locY, npc.locZ, npc.yaw, npc.pitch);
            return this;
        }

        // start velo stuff
        veloRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                npc.move(velocity.getX(), velocity.getY(), velocity.getZ());
                npcLoc = new Location(Bridge.instance.world, npc.locX, npc.locY, npc.locZ, npc.yaw, npc.pitch);
                Location pLoc = player.getLocation();
                connection.sendPacket(new PacketPlayOutEntityTeleport(npc));
                velocity.setY(Math.max(velocity.getY() - gravity, -terminalVelocity));
                velocity.multiply(1.0 - drag);
                if(isInCage) {
                    return;
                }
                boolean isOnGround = npcLoc.clone().subtract(0, 0.05, 0).getBlock().getType() != Material.AIR ||
                        npcLoc.clone().subtract(-0.15, 0.05, 0).getBlock().getType() != Material.AIR ||
                        npcLoc.clone().subtract(0.15, 0.05, 0).getBlock().getType() != Material.AIR;

                updateGoal(isOnGround);
                if(npcLoc.getY() < 80) {
                    kill();
                    return;
                }

                // scoring
                if(npcLoc.getX() <= bridgeX + 2 && npcLoc.getX() >= bridgeX - 1 &&
                        npcLoc.getY() <= 88 && npcLoc.getY() >= 82 &&
                        npcLoc.getZ() <= -6 && npcLoc.getZ() >= -9) {
                    onNpcScore();
                } else if(pLoc.getX() <= bridgeX + 2 && pLoc.getX() >= bridgeX - 1 &&
                        pLoc.getY() <= 88 && pLoc.getY() >= 82 &&
                        pLoc.getZ() <= 54 && pLoc.getZ() >= 51) {
                    onPlayerScore();
                } else if(npcLoc.getX() <= bridgeX + 2 && npcLoc.getX() >= bridgeX - 1 &&
                        npcLoc.getY() <= 88 &&
                        npcLoc.getZ() <= 54 && npcLoc.getZ() >= 51) {
                    kill();
                    return;
                }

                double reach = 3.15;
                double visualReach = 4;
                Location npcEye = npcLoc.clone().add(0, 1, 0);
                double eyeToFoot = npcEye.distance(player.getLocation());
                double eyeToEye = npcEye.distance(player.getLocation().add(0, 1, 0));


                if(goal == Goal.Void) {
                    if(isOnGround && System.currentTimeMillis() - lastJump > 350) {
                        jump();
                    }
                    if(!isWallInFront(0, 1)) {
                        strafeLeft();
                    } else {
                        velocity.setX(velocity.getX() - 0.05);
                    }
                    return;
                }

                if((goal == Goal.Defend || (eyeToFoot < visualReach || eyeToEye < visualReach)) && System.currentTimeMillis() - lastBlockBreak < 400) {
                    // look at player
                    direction = player.getLocation().toVector().subtract(npcLoc.toVector());
                    Location newLoc = npcLoc.setDirection(direction);
                    rotateInstantly(newLoc.getYaw(), newLoc.getPitch());
                } else {
                    npcLoc.setYaw(facingDirection == -1 ? 180 : 0);
                    npcLoc.setPitch(0);
                    direction = npcLoc.getDirection();
                    rotateInstantly(npcLoc.getYaw(), npcLoc.getPitch());
                }

                if(pvp) {
                    Vector moveDir = new Vector(velocity.getX(), velocity.getY(), velocity.getZ()).normalize();
                    moveDir.subtract(direction);

                    if(System.currentTimeMillis() - lastBlockBreak > 400 && bowDrawbackTime == -1 && (eyeToFoot < reach || eyeToEye < reach) && System.currentTimeMillis() - lastHit > 550 && isPlayerInSight(reach) && player.getFallDistance() < 2 && (eyeToEye < 2.7 || (player.isOnGround() && Math.random() > 0.6) || Math.random() > 0.95)) {
                        // hit player
                        if(itemInHand.getType() != sword.getType()) {
                            setItemInHand(sword);
                        }
                        lastHit = System.currentTimeMillis();
                        swingHand();
                        if(player.getHealth() - 5 <= 0) {
                            onPlayerKilled();
                        }
                        Bridge.damagePlayer(player, 5);
                        int newPlayerHealth = (int) Math.round(player.getHealth());
                        playerHealthScore.setScore(newPlayerHealth);
                        tabPlayerHealthScore.setScore(newPlayerHealth);
                        Vector kb = npcLoc.getDirection();
                        kb.multiply(0.6);
                        if(Math.random() > 0.9) {
                            kb.multiply(1.2);
                        }
                        kb.setY(Math.max(kb.getY(), randomBetween(0.3, 0.4)));
                        if(System.currentTimeMillis() - lastPlayerHit < 100) {
                            // reducing
                            kb.multiply(0.2);
                        }
                        player.setVelocity(kb);
                    }

                    if(Math.abs(pLoc.getX() - bridgeX) >= 4 && (goal == Goal.Defend || goal == Goal.Start)) {
                        // shoot bow at player
                        if(bowDrawbackTime == -1) {
                            setItemInHand(bow);
                            bowDrawbackTime = System.currentTimeMillis();
                        } else if(System.currentTimeMillis() - bowDrawbackTime > 3.5 * 1000) {
                            Arrow arrow = Bridge.instance.world.spawnArrow(npcLoc, player.getLocation().toVector().subtract(npcLoc.toVector()).add(new Vector(0, 2, 0)), 2, 0);
                            arrow.setMetadata("NO_DAMAGE", new FixedMetadataValue(Bridge.instance, true));
                            arrow.setMetadata("INTENDED_FOR", new FixedMetadataValue(Bridge.instance, player.getUniqueId()));
                            bowDrawbackTime = -1;
                        }
                    } else {
                        bowDrawbackTime = -1;
                    }
                }

                // movement
                double separation = 2;
                double neededToTurn = 0.1;
                if(goal == Goal.Defend) {
                    if(pLoc.getZ() + separation < npcLoc.getZ() && velocity.getZ() < neededToTurn) {
                        velocity.setZ(velocity.getZ() - 0.05);
                        facingDirection = -1;
                    } else if(pLoc.getZ() - separation > npcLoc.getZ() && velocity.getZ() > -neededToTurn) {
                        velocity.setZ(velocity.getZ() + 0.05);
                        facingDirection = 1;
                    }

                    if(npcLoc.getY() < 90 && pLoc.getY() >= 94) {
                        // just void if we are too low
                        goal = Goal.Void;
                    }
                } else {
                    if(npcLoc.getZ() < -8.4) {
                        // over the goal
                        velocity.setZ(velocity.getZ() + 0.05);
                        facingDirection = 1;
                    } else if(velocity.getZ() < neededToTurn) {
                        velocity.setZ(velocity.getZ() - 0.05);
                        facingDirection = -1;
                    }
                }

                // break the blocks in front of us (hippo) if we are at the goal
                if(goal == Goal.AtGoal) {
                    double xDir = bridgeX + 0.5 - npcLoc.getX();
                    if(isOnGround) {
                        if(npcLoc.getY() >= 94 && npcLoc.getZ() < -7 && npcLoc.getX() < bridgeX + 2.8) { // so we don't get stuck
                            strafeRight();
                        } else if(xDir > 0) { // move towards the center of the goal
                            strafeLeft();
                        } else if(xDir < 0) {
                            strafeRight();
                        }
                    }
                    if(npcLoc.getY() >= 94) {
                        if(npcLoc.clone().subtract(0, 1, 0).getBlock().getType() == Material.STAINED_CLAY) {
                            breakBlock(npcLoc.clone().add(0, -1, 0));
                        }
                    }
                    // mine through hippo
                    if(npcLoc.getZ() >= -3 && npcLoc.getY() >= 93) {
                        if(npcLoc.clone().add(0, 1, -1).getBlock().getType() != Material.AIR) {
                            breakBlock(npcLoc.clone().add(0, 1, -1));
                        } else if(npcLoc.clone().add(0, 0, -1).getBlock().getType() != Material.AIR) {
                            breakBlock(npcLoc.clone().add(0, 0, -1));
                        }
                    }
                }

                // move towards the player if there are blocks there
                if(goal == Goal.Defend && pLoc.getBlockX() != npcLoc.getBlockX() && isOnGround && npcLoc.getY() > 92) {
                    double xDir = pLoc.getBlockX() - npcLoc.getBlockX();
                    if(xDir > 0 && npcLoc.clone().add(1, -1, 0).getBlock().getType() != Material.AIR) {
                        strafeLeft();
                    } else if(xDir < 0 && npcLoc.clone().add(-1, -1, 0).getBlock().getType() != Material.AIR) {
                        strafeRight();
                    }
                }

                // drop down on player if they are bypassing
                if(goal != Goal.Score && pLoc.getY() < 92 && pLoc.getBlockX() != bridgeX && npcLoc.getBlockX() == bridgeX && Math.abs(velocity.getX()) < 0.15 && Math.abs(pLoc.getX() - npcLoc.getX()) < 12 && (pLoc.getX() < bridgeX + 4 && pLoc.getX() > bridgeX - 4)) {
                    velocity.setX(velocity.getX() + (pLoc.getX() < bridgeX ? -1 : 1) * 0.24);
                }

                // clutch
                if(goal != Goal.Defend && goal != Goal.AtGoal && npcLoc.getBlockY() <= 91 && npcLoc.getBlockY() >= 87 && (npcLoc.getBlockX() == bridgeX - 1 || npcLoc.getBlockX() == bridgeX + 1) && ((Math.abs(velocity.getX()) <= 0.1 && Math.random() < Math.pow(91 - npcLoc.getBlockY(), 1.5) / 20) || (pLoc.getY() < 92 && npcLoc.getY() - pLoc.getY() <= 2))) {
                    Location loc = npcLoc.clone().subtract(0, 1, 0);
                    placeBlock(loc);
                }

                if(timeDisabledAll != -1 && System.currentTimeMillis() - timeDisabledAll > 700) {
                    lockToPlayer = true;
                    pvp = true;
                    timeDisabledAll = -1;
                }

                // move towards the center of the bridge if we can when bypassing
                if(goal == Goal.Score && npcLoc.getY() < 93) {
                    if(!isWallInFront(0, -1)) {
                        strafeLeft();
                    } else if(!isWallInFront(0, 1)) {
                        strafeRight();
                    }
                }

                // clutch when bypassing
                if((goal == Goal.Score || (goal == Goal.Defend && pLoc.getY() < 93 && npcLoc.getY() - pLoc.getY() <= 1)) && npcLoc.getY() < 93 && npcLoc.clone().add(0, -1, facingDirection).getBlock().getType() == Material.AIR && (isWallInFront(0, -1) || isWallInFront(0, 1))) {
                    placeBlock(npcLoc.clone().add(0, -1, facingDirection));
                }

                // move towards the center of the block
                double xDiff = 0.5 - (npcLoc.getX() - Math.floor(npcLoc.getX()));
                if(isOnGround && Math.abs(xDiff) > 0.3 && Math.abs(velocity.getX()) < 0.005) {
                    if(xDiff < 0) {
                        velocity.setX(0.02);
                    } else {
                        velocity.setX(-0.02);
                    }
                }

                long timeSinceLastJump = System.currentTimeMillis() - lastJump;

                if(timeSinceLastJump > 350 && goal != Goal.AtGoal) {
                    if(goal == Goal.Start && isOnGround && npcLoc.clone().add(0, -2, facingDirection * 4).getBlock().getType() != Material.AIR && npcLoc.getY() >= 93 && npcLoc.getY() <= 97) {
                        // place blocks under the npc if it is a start goal
                        if(npcLoc.clone().add(0, -1, facingDirection * 4).getBlock().getType() == Material.AIR)
                            placeBlock(npcLoc.clone().add(0, -1, facingDirection * 4));

                        placeBlock(npcLoc.clone().add(0, 0, facingDirection * 4));
                        jump();
                    } else if(goal == Goal.Score && npcLoc.getX() <= bridgeX + 1 && isWallInFront() && npcLoc.clone().add(1, 1, facingDirection).getBlock().getType() == Material.AIR) {
                        // bypass on positive X
                        velocity.setX(velocity.getX() + 0.2);
                        placeBlock(npcLoc.clone().add(1, -1, facingDirection));
                        jump();
                    } else if(goal == Goal.Score && npcLoc.getX() >= bridgeX - 1 && isWallInFront() && npcLoc.clone().add(-1, 1, facingDirection).getBlock().getType() == Material.AIR) {
                        // bypass on negative X
                        velocity.setX(velocity.getX() - 0.2);
                        placeBlock(npcLoc.clone().add(-1, -1, facingDirection));
                        jump();
                    } else if(npcLoc.clone().add(0, 1, facingDirection).getBlock().getType() != Material.AIR || (npcLoc.getY() <= 92 && pLoc.getY() - npcLoc.getY() > 3 && npcLoc.clone().subtract(0, 1, 0).getBlock().getType() != Material.AIR)) {
                        // jump place
                        jump();
                        Location originalLoc = npcLoc.clone();
                        schedule(10, (n) -> {
                            if(npcLoc.getBlockX() == originalLoc.getBlockX() && npcLoc.getBlockZ() == originalLoc.getBlockZ()) // prevent placing if we moved during it (like getting hit)
                                placeBlock(originalLoc);
                        });
                        breakBlock(npcLoc.clone().add(0, 2, 0));
                    } else if(npcLoc.clone().add(0, 0, facingDirection).getBlock().getType() != Material.AIR) {
                        // hurdle jump
                        jump();
                        breakBlock(npcLoc.clone().add(0, 2, 0));
                    } else if(timeSinceLastJump > 500 && Math.abs(pLoc.getZ() - npc.locZ) >= 6 && isOnGround) {
                        // sprint jump
                        jump();
                    }
                }

                if(bowDrawbackTime != -1) {
                    velocity.setX(velocity.getX() * 0.4);
                    velocity.setZ(velocity.getZ() * 0.4);
                }
            }
        };
        veloRunnable.runTaskTimer(Bridge.instance, 0, 1);
        return this;
    }
    interface VeloRunnable {
        void run(NPC npc);
    }
    public NPC setVeloRunnable(VeloRunnable runnable, boolean lookAtPlayer) {
        NPC npc = this;
        this.veloRunnable = (new BukkitRunnable() {
            @Override
            public void run() {
                if(lookAtPlayer) {
                    direction = player.getLocation().toVector().subtract(npcLoc.toVector());
                    Location newLoc = npcLoc.setDirection(direction);
                    rotateInstantly(newLoc.getYaw(), newLoc.getPitch());
                }

                npc.npc.move(velocity.getX(), velocity.getY(), velocity.getZ());
                npcLoc = new Location(Bridge.instance.world, npc.npc.locX, npc.npc.locY, npc.npc.locZ, npc.npc.yaw, npc.npc.pitch);
                Location pLoc = player.getLocation();
                connection.sendPacket(new PacketPlayOutEntityTeleport(npc.npc));
                velocity.setY(Math.max(velocity.getY() - gravity, -terminalVelocity));
                velocity.multiply(1.0 - drag);

                runnable.run(npc);
            }
        });
        veloRunnable.runTaskTimer(Bridge.instance, 0, 1);
        return this;
    }
    void moveInstantly(double dx, double dy, double dz) {
        assert dx <= 4;
        assert dy <= 4;
        assert dz <= 4;
        if(npc.isSneaking()) {
            npc.move(dx * 0.6, dy * 0.6, dz * 0.6);
        } else {
            npc.move(dx, dy, dz);
        }
        connection.sendPacket(new PacketPlayOutEntity.PacketPlayOutRelEntityMove(npc.getId(), (byte) ((dx * 32)), (byte) ((dy * 32)), (byte) ((dz * 32)), true));
    }
    void rotateInstantly(float yaw, float pitch) {
        npc.yaw = yaw;
        npc.pitch = pitch;
        connection.sendPacket(new PacketPlayOutEntity.PacketPlayOutEntityLook(npc.getId(), (byte) (yaw * 256 / 360), (byte) (pitch * 256 / 360), true));
        connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) (yaw * 256 / 360)));
    }
    void teleportToSetLocation() {
        connection.sendPacket(new PacketPlayOutEntityTeleport(npc));
    }
    void move(float x, float y, float z, int ticks) {
        final int[] i = {0};
        schedules.add((new BukkitRunnable() {
            @Override
            public void run() {
                i[0]++;
                moveInstantly(x / ticks, y / ticks, z / ticks);
                if(i[0] > ticks)
                    this.cancel();
            }
        }).runTaskTimer(Bridge.instance, 0, 1));
    }
    NPC bridge(float dx, float dz, int totalTicks, Material material, int data, boolean diagonal) {
        clearSchedules();
        connection.sendPacket(new PacketPlayOutEntityEquipment(npc.getId(), 0, CraftItemStack.asNMSCopy(new ItemStack(material, 1, (byte) data))));
        move(dx, 0, dz, totalTicks);
        NPC thisRef = this;
        BukkitRunnable sneak = new BukkitRunnable() {
            @Override
            public void run() {
                Location currentBlock = new Location(Bridge.instance.world, npc.locX, npc.locY - 1, npc.locZ);
                Location nextBlock = currentBlock.clone().subtract((dx == 0 ? 0 : (dx > 0 ? 0.2 : -0.2)), 0, (dz == 0 ? 0 : (dz > 0 ? 0.2 : -0.2)));
                if(nextBlock.getBlock().getType() == Material.AIR && !npc.isSneaking()) {
                    if(diagonal) {
                        Location otherBlock = nextBlock.getBlock().getLocation().add(0, 0, -1);
                        changedBlocks.add(otherBlock);
                        otherBlock.getBlock().setType(material);
                        if(data != -1)
                            otherBlock.getBlock().setData((byte) data);
                        swingHand();
                    }
                    thisRef.setSneaking(true);
                    schedules.add(thisRef.schedule(2, (n) -> {
                        nextBlock.getBlock().setType(material);
                        if(data != -1)
                            nextBlock.getBlock().setData((byte) data);
                        swingHand();
                        changedBlocks.add(nextBlock);
                    }));
                    thisRef.schedule(4, (n) -> n.setSneaking(false));
                }
            }
        };
        schedules.add(sneak.runTaskTimer(Bridge.instance, 0, 1));
        schedules.add(schedule(totalTicks - 4, (n2) -> sneak.cancel()));
        return this;
    }
    void swingHand() {
        connection.sendPacket(new PacketPlayOutAnimation(npc, 0));
    }
    void setSneaking(boolean shouldSneak) {
        // see https://stackoverflow.com/a/44006823/13608595
        npc.setSneaking(shouldSneak);
        DataWatcher dw = npc.getDataWatcher();
        if(shouldSneak) {
            dw.watch(0, (byte) 0x02);
        } else {
            dw.watch(0, (byte) 0x00);
        }
        connection.sendPacket(new PacketPlayOutEntityMetadata(npc.getId(), dw, false));
    }
    void showDamage() {
        connection.sendPacket(new PacketPlayOutAnimation(npc, 1));
    }
    double strafeSpeed = 0.04;
    void strafeRight() {
        velocity.setX(velocity.getX() - strafeSpeed);
    }
    void strafeLeft() {
        velocity.setX(velocity.getX() + strafeSpeed);
    }
    enum KnockbackReason {
        PlayerHit,
        ArrowHit
    }
    void hit(Vector velo, KnockbackReason reason, double damage) {
        if(isHitTimered()) return;

        showDamage();
        velo.multiply(0.55);
        if(reason != KnockbackReason.ArrowHit) {
            if(velocity.getY() < 2.1 && System.currentTimeMillis() - lastJump > 300) {
                velo.setY(velo.getY() + randomBetween(1.10, 1.25));
            } else {
                velo.setY(velo.getY() + randomBetween(0.2, 0.3));
            }
        } else {
            velo.setY(velo.getY() + randomBetween(0.2, 0.3));
        }


        this.health -= damage;
        if(this.health <= 0) {
            this.kill();
            return;
        }
        int newHealth = (int) Math.round(this.health);
        healthScore.setScore(newHealth);
        tabHealthScore.setScore(newHealth);

        this.velocity.add(velo);
    }
    void hitFromArrow(Player hittingPlayer, Arrow arrow) {
        if(isHitTimeredNoReset()) return;

        hittingPlayer.playSound(new Location(Bridge.instance.world, npc.locX, npc.locY, npc.locZ), Sound.ARROW_HIT, 1, 1); // arrow hit
        hittingPlayer.playSound(hittingPlayer.getLocation(), Sound.SUCCESSFUL_HIT, 1f, 0.5f); // bow ding
        Vector velocity = arrow.getVelocity();
        velocity.multiply(0.7);
        lastHit = System.currentTimeMillis() - 300; // bow combos should stop the npc from hitting

        double damage = Math.round(arrow.getVelocity().length()) * 1.6;

        int oldHealth = (int) Math.round(this.health / 2);
        this.hit(velocity, KnockbackReason.ArrowHit, damage);
        int newHealth = (int) Math.round(this.health / 2);
        Bridge.showActionBarDamage(player, newHealth, oldHealth, name);
    }
    void hit(Player hittingPlayer) {
        if(isHitTimeredNoReset()) return;

        hittingPlayer.playSound(new Location(Bridge.instance.world, npc.locX, npc.locY, npc.locZ), Sound.HURT_FLESH, 1, 1);
        Vector velo = hittingPlayer.getEyeLocation().getDirection();
        PlayerInfo info = Bridge.instance.getPlayer(player.getUniqueId());
        if(player.isSprinting()) {
            if(info.locSettings.isSprintingHit) {
                // sprinting hits do 4 blocks of kb
                info.locSettings.isSprintingHit = false;
                velo.multiply(randomBetween(1.4, 1.8));
                velo.setY(0.17);
            } else {
                // non sprinting hits after sprinting do 0.125-0.5 blocks of kb
                velo.multiply(randomBetween(0.8, 1));
                velo.setY(0.15);
            }
        } else {
            velo.multiply(randomBetween(0.7, 0.9));
        }

        double itemDamage;
        Material itemInHandType = player.getItemInHand().getType();
        if(itemInHandType == Material.IRON_SWORD) {
            itemDamage = 5;
        } else if(itemInHandType == Material.DIAMOND_PICKAXE) {
            itemDamage = 3;
        } else {
            itemDamage = 1;
        }

        if(!player.isOnGround() && player.getFallDistance() > 0) {
            itemDamage *= 1.5; // critical
        }
        int oldHealth = (int) Math.round(this.health / 2);
        this.hit(velo, KnockbackReason.PlayerHit, itemDamage);
        int newHealth = (int) Math.round(this.health / 2);
        Bridge.showActionBarDamage(player, newHealth, oldHealth, name);
    }
    void jump() {
        lastJump = System.currentTimeMillis();
        velocity.setY(velocity.getY() + 1.3);
        velocity.setZ(velocity.getZ() * 1.2);
    }
    void placeBlock(Location loc) {
        if(loc.getBlock().getType() != Material.AIR) return;
        if(player.getLocation().getBlock().getLocation() == loc.getBlock().getLocation())
            return; // don't place blocks in the player
        if(player.getEyeLocation().getBlock().getLocation() == loc.getBlock().getLocation())
            return; // don't place blocks in the player's eyes

        if(loc.getBlockY() > 99 || loc.getBlockY() < 84 ||
                loc.getBlockZ() < -2 || loc.getBlockZ() > 46)
            return;

        loc.setPitch(90);
        rotateInstantly(loc.getYaw(), loc.getPitch());

        setItemInHand(blocks);

        changedBlocks.add(loc);

        lockToPlayer = false;
        pvp = false;
        timeDisabledAll = System.currentTimeMillis() - 400;

        loc.getBlock().setType(blocks.getType());
        loc.getBlock().setData((byte) blocks.getDurability());
        player.playSound(loc, Sound.DIG_STONE, 0.8f, 0.8f);
    }
    void breakBlock(Location loc) {
        if(System.currentTimeMillis() - lastBlockBreak < 350) { // if it hasn't been enough time since we last broke a block, then don't break this one
            return;
        }
        lastBlockBreak = System.currentTimeMillis();
        org.bukkit.block.Block block = loc.getBlock();
        if(block.getType() != Material.STAINED_CLAY) return;
        if(block.getData() != 11 && block.getData() != 14) return;

        Vector dir = loc.toVector().subtract(npcLoc.toVector());
        Location newLoc = npcLoc.setDirection(dir);
        rotateInstantly(newLoc.getYaw(), newLoc.getPitch());

        setItemInHand(pickaxe);

        connection.sendPacket(new PacketPlayOutBlockBreakAnimation(npc.getId(), new BlockPosition(loc.getX(), loc.getY(), loc.getZ()), 4));

        this.schedule(5, (n) -> {
            loc.getBlock().breakNaturally();
            connection.sendPacket(new PacketPlayOutBlockBreakAnimation(npc.getId(), new BlockPosition(loc.getX(), loc.getY(), loc.getZ()), 0));
            player.playSound(loc, Sound.DIG_STONE, 1, 0.8f);
        });
    }
    boolean isHitTimered() {
        if(System.currentTimeMillis() - lastPlayerHit < 450) {
            return true;
        } else {
            lastPlayerHit = System.currentTimeMillis();
            return false;
        }
    }
    boolean isHitTimeredNoReset() {
        return System.currentTimeMillis() - lastPlayerHit < 450;
    }
    boolean isPlayerInSight(double reach) {
        double accuracy = 0.1;
        Vector origin = new Vector(npc.locX, npc.locY + 1, npc.locZ); // add 1 so it is the head
        for(double d = 0; d <= reach; d += accuracy) {
            Vector vec = origin.clone().add(direction.clone().multiply(d));
            Location adjustedLoc = vec.toLocation(Bridge.instance.world);
            if(adjustedLoc.getBlock().getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }
    boolean isWallInFront(int distanceFromFront, int xOffset) {
        boolean bottom = npcLoc.clone().add(xOffset, 0, facingDirection * distanceFromFront).getBlock().getType() != Material.AIR;
        boolean middle = npcLoc.clone().add(xOffset, 1, facingDirection * distanceFromFront).getBlock().getType() != Material.AIR;
        boolean top = npcLoc.clone().add(xOffset, 2, facingDirection * distanceFromFront).getBlock().getType() != Material.AIR;
        return (middle && top) || (bottom && top);
    }
    boolean isWallInFront() {
        return isWallInFront(1, 0);
    }
    void onPlayerKilled() {
        if(goal == Goal.Defend) {
            goal = Goal.Score;
        }
    }
    void onPlayerScore() {
        goal = Goal.Start;
        playerGoals++;


        String dashes = "§6§l" + new String(new char[47]).replace("\0", "-") + "§r";
        player.sendMessage(dashes);
        player.sendMessage("\n           §9§l" + player.getName() + "§r §7(§a" + new DecimalFormat("0.#").format(player.getHealth()) + "§c" + Bridge.hearts(1) + "§7) §escored! §7(§6" + ordinal(playerGoals) + " Goal§7)\n                                §9§l" + playerGoals + "§r §7- §c" + npcGoals);
        player.sendMessage("\n" + dashes);

        scoreHandler.call(false, this);

        // display on scoreboard
        String suffix = "§9" + new String(new char[playerGoals]).replace("\0", "⬤");
        if(5 - playerGoals > 0) {
            suffix += "§7" + new String(new char[5 - playerGoals]).replace("\0", "⬤");
            putInCages.call(0, this);
        } else {
            onWin.call(false, this);
            Bridge.sendTitle(player, "§9BLUE WINS!", "§9" + playerGoals + " §7- §c" + npcGoals, 0, 10, 5 * 20);
        }
        board.getTeam("blue").setSuffix(suffix);
        board.getTeam("goals").setSuffix("§a" + playerGoals);
    }
    void onNpcScore() {
        goal = Goal.Start;
        npcGoals++;

        scoreHandler.call(true, this);

        String dashes = "§6§l" + new String(new char[47]).replace("\0", "-") + "§r";
        player.sendMessage(dashes);
        player.sendMessage("\n           §c§l" + name + "§r §7(§a" + new DecimalFormat("0.#").format(health) + "§c" + Bridge.hearts(1) + "§7) §escored! §7(§6" + ordinal(npcGoals) + " Goal§7)\n                                §c§l" + npcGoals + "§r §7- §9" + playerGoals);
        player.sendMessage("\n" + dashes);

        // display on scoreboard
        String suffix = "§c" + new String(new char[npcGoals]).replace("\0", "⬤");
        if(5 - npcGoals > 0) {
            suffix += "§7" + new String(new char[5 - npcGoals]).replace("\0", "⬤");
            putInCages.call(1, this);
        } else {
            onWin.call(true, this);
            Bridge.sendTitle(player, "§cRED WINS!", "§c" + npcGoals + " §7- §9" + playerGoals, 0, 10, 5 * 20);
        }
        board.getTeam("red").setSuffix(suffix);

    }

    void updateGoal(boolean isOnGround) {
        double playerToNpcGoal = Math.abs(54 - player.getLocation().getZ());
        double npcToPlayerGoal = Math.abs(npcLoc.getZ() - (-6));
        if(goal == Goal.Start) {
            if(playerToNpcGoal <= 30) {
                goal = Goal.Defend;
                if(npcLoc.getZ() < player.getLocation().getZ()) {
                    // void
                    goal = Goal.Void;
                }
            } else if(npcToPlayerGoal <= 30) {
                goal = Goal.Score;
            }
        } else if(goal == Goal.Score) {
            if(npcToPlayerGoal < 10 && npcLoc.getY() >= 93 && isOnGround) {
                goal = Goal.AtGoal;
            } else if(playerToNpcGoal <= 30 && npcToPlayerGoal > 27) {
                goal = Goal.Defend;
                if(npcLoc.getZ() < player.getLocation().getZ()) {
                    // void
                    goal = Goal.Void;
                }
            }
        }
    }
    void kill() {
        if(goal == Goal.Void) {
            goal = Goal.Defend;
        } else if(goal == Goal.AtGoal) {
            goal = Goal.Score;
        }
        reset();
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
        player.sendMessage("§c" + name + "§7 was killed by §9" + player.getName() + "§7.");
        npcDeaths++;
        board.getTeam("kills").setSuffix("§a" + npcDeaths);
    }
    void reset() {
        setLocation(this.respawnLocation);
        teleportToSetLocation();
        health = 20;
        healthScore.setScore(20);
        tabHealthScore.setScore(20);
    }

    BukkitTask schedule(long ticksUntil, Scheduled scheduled) {
        NPC thisContext = this;
        return (new BukkitRunnable() {
            @Override
            public void run() {
                scheduled.call(thisContext);
            }
        }).runTaskLater(Bridge.instance, ticksUntil);
    }
    void clearSchedules() {
        if(schedules.size() != 0) {
            for(BukkitTask task : schedules) {
                task.cancel();
            }
            schedules.clear();
        }
    }

    void remove() {
        connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc));
        connection.sendPacket(new PacketPlayOutEntityDestroy(npc.getId()));
        if(veloRunnable != null) {
            veloRunnable.cancel();
        }
    }
    void removeChangedBlocks() {
        for(Location loc : changedBlocks) {
            loc.getBlock().setType(Material.AIR);
        }
    }

    private final Random rand = new Random();
    public static final String[] names = new String[]{"bdamja", "Gqtor", "SpeedToggled", "Cheetahh", "parihs", "BuckyBarrTV", "hypixel", "Cruh", "Steve", "iReply", "Vitaaal", "WillOCN", "OGWilly", "oNils", "Sezarity", "BitSalty"};
    public static Property[] skins = new Property[16];

    int getRandom() {
        return rand.nextInt(names.length);
    }
    double randomBetween(double min, double max) {
        return min + Math.random() * (max - min);
    }
    private static Team getOrCreateTeam(Scoreboard board, String teamName) {
        Team res = board.getTeam(teamName);
        if(res == null) {
            res = board.registerNewTeam(teamName);
        }
        return res;
    }
    private static Objective getOrCreateObjective(Scoreboard board, String s, String s1) {
        Objective res = board.getObjective(s);
        if(res == null) {
            res = board.registerNewObjective(s, s1);
        }
        return res;
    }
}

interface Scheduled {
    void call(NPC npc);
}

interface ScoreHandler {
    void call(boolean botWon, NPC npc);
}

interface PutInCages {
    void call(int scorer, NPC npc);
}