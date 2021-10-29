package club.bridgepractice.Bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static club.bridgepractice.Bridge.Bridge.nmsWorld;

public class Leaderboard {
    EntityArmorStand titleStand;
    EntityArmorStand playerStand;
    EntityArmorStand[] topPlayers;
    PlayerConnection connection;
    Player player;

    enum Direction {
        Ascending,
        Descending
    }

    enum ColumnType {
        Float,
        Integer
    }

    Direction dir;
    ColumnType columnType;
    public Leaderboard(String title, Player player, double x, double y, double z, Direction dir, ColumnType columnType) {
        connection = ((CraftPlayer) player).getHandle().playerConnection;
        this.player = player;
        this.dir = dir;
        this.columnType = columnType;

        titleStand = spawnStand(title, x, y, z);

        topPlayers = new EntityArmorStand[10];

        for(int i = 0; i < topPlayers.length; i++) {
            topPlayers[i] = spawnStand(utilPadString("§f" + (i + 1) + ". §6...") + "§a0.00", x, y - 0.3 - (i * 0.27), z);
        }
        playerStand = spawnStand(utilPadString("§cN/A§f. §6" + player.getName()) + "§a0.00", x, y - 0.31 - (topPlayers.length * 0.27), z);
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
            topPlayers[i].setCustomName(utilPadString("§f" + (i + 1) + ". §6...") + "§a" + (columnType == ColumnType.Float ? "0.00" : "0"));
        }
        resetLeaderboard();
        (new BukkitRunnable() {
            @Override
            public void run() {
                boolean isPlayerShownAlready = false;
                try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT uuid, " + col + " FROM players WHERE " + col + " IS NOT NULL AND frozen <> 1 AND bannedAt IS NULL ORDER BY " + col + " " + (dir == Direction.Ascending ? "ASC" : "DESC") + " LIMIT 10;")) {
                    ResultSet res = statement.executeQuery();
                    int i = 0;
                    while(res.next()) {
                        String uuid = res.getString(1);
                        double value = columnType == ColumnType.Float ? res.getFloat(2) : res.getInt(2);
                        if(value == 0) continue;
                        OfflinePlayer offlinePlayer = Bridge.instance.getServer().getOfflinePlayer(uuid);
                        String leaderboardPlayerName = offlinePlayer != null && offlinePlayer.hasPlayedBefore() ? offlinePlayer.getName() : getNameFromUuidSyncCached(uuid);
                        if(player.getName().equals(leaderboardPlayerName)) {
                            isPlayerShownAlready = true;
                        }
                        topPlayers[i].setCustomName(utilPadString("§f" + (i + 1) + ". §" + (player.getName().equals(leaderboardPlayerName) ? "e" : "6") + leaderboardPlayerName) + "§a" +
                                (columnType == ColumnType.Float ? Bridge.padWithZeroes(String.valueOf(value / 1000)) : String.valueOf(Math.round(value))));
                        i++;
                    }
                    if(!isPlayerShownAlready) {
                        try(PreparedStatement playerValueStatement = Bridge.connection.prepareStatement("SELECT "+col+" FROM players WHERE uuid = ?;")) {
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
                                        (columnType == ColumnType.Float ? Bridge.padWithZeroes(String.valueOf(value / 1000)) : String.valueOf(Math.round(value))));
                            }
                        }
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
                playerStand.setCustomNameVisible(!isPlayerShownAlready);
                resetLeaderboard();
            }
        }).runTaskAsynchronously(Bridge.instance);
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
        try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT COUNT(*) AS place FROM (SELECT 1 FROM players WHERE " + columnName + " " + (dir == Direction.Ascending ? "<" : ">") + " ? AND frozen <> 1 AND bannedAt IS NULL) t;")) {
            statement.setFloat(1, value);
            ResultSet res = statement.executeQuery();
            if(!res.next()) return -1;
            return res.getInt("place") + 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public static int getPlace(String columnName, float value, Direction dir) {
        try(PreparedStatement statement = Bridge.connection.prepareStatement("SELECT COUNT(*) AS place FROM (SELECT 1 FROM players WHERE " + columnName + " " + (dir == Direction.Ascending ? "<" : ">") + " ? AND frozen <> 1 AND bannedAt IS NULL) t;")) {
            statement.setFloat(1, value);
            ResultSet res = statement.executeQuery();
            if(!res.next()) return -1;
            return res.getInt("place") + 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static JsonArray getJSONArray(String urlString) throws IOException {
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        JsonElement parsed = new JsonParser().parse(in);
        if(!parsed.isJsonArray()) {
            throw new IOException("Did not get a JSON array from URL "+urlString);
        }
        JsonArray result = parsed.getAsJsonArray();
        in.close();
        return result;
    }

    private static final ConcurrentHashMap<String, String> cachedUuidToName = new ConcurrentHashMap<>();
    private static final AtomicLong lastCacheEvict = new AtomicLong(System.currentTimeMillis());
    private static String getNameFromUuidSyncCached(String uuid) throws IOException {
        if(System.currentTimeMillis() - lastCacheEvict.get() > 60*60*1000) {
            lastCacheEvict.set(System.currentTimeMillis());
            cachedUuidToName.clear();
            Bridge.instance.getLogger().info("Evicting UUID to username cache");
        }
        String name = cachedUuidToName.get(uuid);
        if(name != null) {
            return name;
        } else {
            String requestedName = getNameFromUuidSync(uuid);
            cachedUuidToName.put(uuid, requestedName);
            return requestedName;
        }
    }
    private static String getNameFromUuidSync(String uuid) throws IOException {
        JsonArray names = getJSONArray("https://api.mojang.com/user/profiles/" + uuid + "/names");
        return names.get(names.size()-1).getAsJsonObject().get("name").getAsString();
    }
}
