package net.bridgepractice.BridgePracticeLobby;

import net.minecraft.server.v1_8_R3.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class Leaderboard {
    EntityArmorStand titleStand;
    EntityArmorStand playerStand;
    EntityArmorStand clickStand;
    EntityArmorStand[] topPlayers;
    PlayerConnection connection;
    Player player;
    World nmsWorld;
    Set<Integer> ids = new HashSet<>();
    long lastClick = 0;

    enum Direction {
        Ascending,
        Descending
    }

    enum ColumnType {
        Float,
        Integer,
        Xp,
        MinutesToHours
    }

    Direction dir;
    ColumnType columnType;

    double x, y, z;
    public Leaderboard(String title, Player player, double x, double y, double z, Direction dir, ColumnType columnType) {
        connection = ((CraftPlayer) player).getHandle().playerConnection;
        this.player = player;
        this.dir = dir;
        this.columnType = columnType;
        this.nmsWorld = ((CraftWorld) player.getWorld()).getHandle();
        this.x = x;
        this.y = y;
        this.z = z;

        titleStand = spawnStand("§b" + title, x, y, z);
        ids.add(titleStand.getId());

        topPlayers = new EntityArmorStand[10];

        for(int i = 0; i < topPlayers.length; i++) {
            topPlayers[i] = spawnStand(utilPadString("§f" + (i + 1) + ". §6...") + "§a0.00", x, y - 0.3 - (i * 0.27), z);
            ids.add(topPlayers[i].getId());
        }
        playerStand = spawnStand(utilPadString("§cN/A§f. §6" + player.getName()) + "§a0.00", x, y - 0.31 - (topPlayers.length * 0.27), z);
        ids.add(playerStand.getId());
    }
    private String[] clickableText, clickableColumns;
    private Direction[] clickableDirections;
    private ColumnType[] clickableColumnTypes;
    private int selectedClickable;
    public void addClickable(String[] text, String[] columns, Direction[] directions, ColumnType[] columnTypes) {
        if(text.length != columns.length) {
            BridgePracticeLobby.instance.getLogger().severe("Attempted to add clickable toggle that didn't have enough column strings");
            return;
        }

        clickableText = text;
        clickableColumns = columns;
        clickableDirections = directions;
        clickableColumnTypes = columnTypes;
        selectedClickable = 0;
        loadColumn(columns[0]);

        ids.add(spawnStand("§6§lClick to toggle!", x, y - 0.31 - ((topPlayers.length + 1) * 0.27), z).getId());

        clickStand = spawnStand(getClickableText(), x, y - 0.31 - ((topPlayers.length + 2) * 0.27), z);
        ids.add(clickStand.getId());
    }
    public boolean isIdFromThisLeaderboard(int id) {
        return ids.contains(id);
    }
    public void onClickableClick(Player player) {
        if(System.currentTimeMillis() - lastClick > 3000) {
            lastClick = System.currentTimeMillis();
            selectedClickable++;
            if(selectedClickable == clickableText.length) selectedClickable = 0;

            dir = clickableDirections[selectedClickable];
            columnType = clickableColumnTypes[selectedClickable];

            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
            player.sendMessage("§aShowing §b§l" + clickableText[selectedClickable] + "§a.");
            connection.sendPacket(new PacketPlayOutEntityDestroy(playerStand.getId()));
            for(EntityArmorStand armorStand : topPlayers) {
                connection.sendPacket(new PacketPlayOutEntityDestroy(armorStand.getId()));
            }

            connection.sendPacket(new PacketPlayOutEntityDestroy(clickStand.getId()));
            clickStand.setCustomName(getClickableText());
            connection.sendPacket(new PacketPlayOutSpawnEntityLiving(clickStand));
            loadColumn(clickableColumns[selectedClickable]);
        } else {
            player.sendMessage("§cYou must wait §e3s§c between switches");
        }
    }
    public String getClickableText() {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < clickableText.length; i++) {
            if(i == selectedClickable) {
                builder.append("§a§l");
            } else {
                builder.append("§7");
            }
            builder.append(clickableText[i]);
            if(i != clickableText.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
    public int getIdOfClickable() {
        if(clickStand == null) {
            return -1;
        } else {
            return clickStand.getId();
        }
    }
    public void remove() {
        connection.sendPacket(new PacketPlayOutEntityDestroy(titleStand.getId()));
        connection.sendPacket(new PacketPlayOutEntityDestroy(playerStand.getId()));
        for(EntityArmorStand armorStand : topPlayers) {
            connection.sendPacket(new PacketPlayOutEntityDestroy(armorStand.getId()));
        }
    }
    public void loadColumn(String col) {
        for(int i = 0; i < topPlayers.length; i++) {
            topPlayers[i].setCustomName(utilPadString("§f" + (i + 1) + ". §6...") + "§a" + (columnType == ColumnType.Float ? "0.00" : ("0")) + (columnType == ColumnType.Xp ? "⫯" : (columnType == ColumnType.MinutesToHours ? "h" : "")));
        }
        resetLeaderboard();
        (new BukkitRunnable() {
            @Override
            public void run() {
                boolean isPlayerShownAlready = false;
                try(PreparedStatement statement = BridgePracticeLobby.connection.prepareStatement("SELECT uuid, " + col + " FROM players WHERE " + col + " IS NOT NULL AND frozen <> 1 AND bannedAt IS NULL ORDER BY " + col + " " + (dir == Direction.Ascending ? "ASC" : "DESC") + " LIMIT 10;")) {
                    ResultSet res = statement.executeQuery();
                    int i = 0;
                    while(res.next()) {
                        String uuid = res.getString(1);
                        double value = columnType == ColumnType.Float ? res.getFloat(2) : res.getInt(2);
                        if(value == 0) continue;
                        OfflinePlayer offlinePlayer = BridgePracticeLobby.instance.getServer().getOfflinePlayer(uuid);
                        String leaderboardPlayerName = offlinePlayer != null && offlinePlayer.hasPlayedBefore() ? offlinePlayer.getName() : Utils.getNameFromUuidSyncCached(uuid);
                        if(player.getName().equals(leaderboardPlayerName)) {
                            isPlayerShownAlready = true;
                        }
                        topPlayers[i].setCustomName(utilPadString("§f" + (i + 1) + ". §" + (player.getName().equals(leaderboardPlayerName) ? "e" : "6") + leaderboardPlayerName) + "§a" +
                                (columnType == ColumnType.Float ? BridgePracticeLobby.prettifyTime(value / 1000) : String.valueOf(Math.round(columnType == ColumnType.MinutesToHours ? value / 60 : value)))
                                + (columnType == ColumnType.Xp ? "⫯" : (columnType == ColumnType.MinutesToHours ? "h" : "")));
                        i++;
                    }
                    if(!isPlayerShownAlready) {
                        try(PreparedStatement playerValueStatement = BridgePracticeLobby.connection.prepareStatement("SELECT "+col+" FROM players WHERE uuid = ?;")) {
                            playerValueStatement.setString(1, player.getUniqueId().toString());
                            ResultSet playerValueRes = playerValueStatement.executeQuery();
                            if(!playerValueRes.next()) {
                                // set to N/A
                                playerStand.setCustomName(utilPadString("§cN/A. §6" + player.getName()) + "§a0.00");
                            } else {
                                double value = columnType == ColumnType.Float ? playerValueRes.getFloat(1) : playerValueRes.getInt(1);
                                if(playerValueRes.wasNull() || value == 0) {
                                    // set to N/A
                                    playerStand.setCustomName(utilPadString("§cN/A. §6" + player.getName()) + "§a0.00");
                                    return;
                                }
                                playerStand.setCustomName(utilPadString("§f" + (getPlace(col, ((float) value))) + ". §e" + player.getName()) +
                                        (columnType == ColumnType.Float ? BridgePracticeLobby.prettifyTime(value / 1000) : String.valueOf(Math.round(columnType == ColumnType.MinutesToHours ? value / 60 : value)))
                                        + (columnType == ColumnType.Xp ? "⫯" : (columnType == ColumnType.MinutesToHours ? "h" : "")));
                            }
                        }
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
                playerStand.setCustomNameVisible(!isPlayerShownAlready);
                resetLeaderboard();
            }
        }).runTaskAsynchronously(BridgePracticeLobby.instance);
    }
    public void setTitle(String newTitle) {
        titleStand.setCustomName(newTitle);
        connection.sendPacket(new PacketPlayOutEntityDestroy(titleStand.getId()));
        connection.sendPacket(new PacketPlayOutSpawnEntityLiving(titleStand));
    }
    private void resetLeaderboard() {
        connection.sendPacket(new PacketPlayOutEntityDestroy(playerStand.getId()));
        connection.sendPacket(new PacketPlayOutSpawnEntityLiving(playerStand));
        for(EntityArmorStand armorStand : topPlayers) {
            connection.sendPacket(new PacketPlayOutEntityDestroy(armorStand.getId()));
            connection.sendPacket(new PacketPlayOutSpawnEntityLiving(armorStand));
        }
    }
    private EntityArmorStand spawnStand(String name, double x, double y, double z) {
        EntityArmorStand armorStand = new EntityArmorStand(nmsWorld, x, y, z);
        armorStand.setCustomName(name);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvisible(true);

        connection.sendPacket(new PacketPlayOutSpawnEntityLiving(armorStand));

        return armorStand;
    }
    private String utilPadString(String str) {
        return String.format("%-24s", str);
    }

    public int getPlace(String columnName, float value) {
        try(PreparedStatement statement = BridgePracticeLobby.connection.prepareStatement("SELECT COUNT(*) AS place FROM (SELECT 1 FROM players WHERE " + columnName + " " + (dir == Direction.Ascending ? "<" : ">") + " ? AND frozen <> 1 AND bannedAt IS NULL) t;")) {
            statement.setFloat(1, value);
            ResultSet res = statement.executeQuery();
            if(!res.next()) return -1;
            return res.getInt("place") + 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
