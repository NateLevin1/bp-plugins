package net.bridgepractice.skywarspractice.SkywarsPracticeMain;
import java.sql.*;
import java.util.*;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.commands.*;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.listeners.*;
import net.bridgepractice.skywarspractice.SkywarsPracticeMain.menus.PearlPracticeStart;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scoreboard.*;

public class Main extends JavaPlugin implements PluginMessageListener {
  public static List<UUID> lootPracticeQueue = new ArrayList<>(); public static Main instance;
  public static List<String> availableLootPracticeMaps = new ArrayList<>();
  public static HashMap<UUID, String> playersInLootPractice = new HashMap<>();
  public static HashMap<UUID, List<Block>> lootPracticeBlocksPlaced = new HashMap<>();
  public static HashMap<String, Long> lootPracticeMapTimes = new HashMap<>();

  public static List<UUID> pearlPracticeQueue = new ArrayList<>();
  public static List<String> availablePearlPracticeMaps = new ArrayList<>();
  public static HashMap<UUID, String> playersInPearlPractice = new HashMap<>();


  public ScoreboardManager sm;
  public boolean queueingEnabled = true;

  String host = "localhost";
  String port = "3306";
  String database = "bridge";
  String username = "mc";
  String password = "mcserver";

  public static Connection connection;

  public static Location spawn;

  public void onEnable() {
    instance = this;
    this.sm = getServer().getScoreboardManager();
    spawn = new Location(Bukkit.getWorld("skywars"), 76.5D, 57.0D, 18.5D, -164.0F, 3.0F);

    availableLootPracticeMaps.add("plainsone");
    availableLootPracticeMaps.add("plainstwo");
    availableLootPracticeMaps.add("plainsthree");
    availableLootPracticeMaps.add("plainsfour");

    availablePearlPracticeMaps.add("pearlone");
    availablePearlPracticeMaps.add("pearltwo");
    availablePearlPracticeMaps.add("pearlthree");
    availablePearlPracticeMaps.add("pearlfour");

    try {
      openConnection();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    getCommand("debug").setExecutor(new Debug());
    getCommand("spawn").setExecutor(new Spawn());
    getCommand("leavequeue").setExecutor(new LeaveQueue());
    getCommand("loot").setExecutor(new LootPractice());
    getCommand("pearl").setExecutor(new PearlPractice());


    Bukkit.getServer().getPluginManager().registerEvents(new OnPlayerMove(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new OnBlockPlace(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new OnBlockBreak(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new OnPlayerJoin(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new OnPlayerDisconnect(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new OnPlayerFoodChange(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new OnPlayerTeleport(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new OnPlayerHit(), instance);

    Bukkit.getServer().getPluginManager().registerEvents(new PearlPractice(), instance);
    Bukkit.getServer().getPluginManager().registerEvents(new PearlPracticeStart(), instance);

    getServer().getMessenger().registerIncomingPluginChannel( this, "BungeeCord", this );

    getLogger().info("Skywars Practice enabled!");
  }
  public void onDisable() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }

    getLogger().info("Skywars Practice disabled!");
  }

  public static void givePlayerXP(Player player, int xpAmount) {
    try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET xp = xp + ? WHERE uuid=?;")) {
      statement.setInt(1, xpAmount);
      statement.setString(2, player.getUniqueId().toString());
      statement.executeUpdate();
    } catch (SQLException throwables) {
      throwables.printStackTrace();
      player.sendMessage("§c§lUh oh!§r§c Something went wrong pushing your information to our database. Please open a ticket on the discord!");
    }

    try (PreparedStatement statement = connection.prepareStatement("UPDATE skywarsPlayers SET xp = xp + ? WHERE uuid=?;")) {
      statement.setInt(1, xpAmount);
      statement.setString(2, player.getUniqueId().toString());
      statement.executeUpdate();
    } catch (SQLException throwables) {
      throwables.printStackTrace();
      player.sendMessage("§c§lUh oh!§r§c Something went wrong pushing your information to our database. Please open a ticket on the discord!");
    }
  }
  public static int getNetworkPlayerXP(Player player) {
    try (PreparedStatement statement = connection.prepareStatement("SELECT xp FROM players WHERE uuid=?;")) {
      statement.setString(1, player.getUniqueId().toString());
      ResultSet res = statement.executeQuery();
      if (!res.next()) {
        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
      }
      return res.getInt(1);
    } catch (SQLException throwables) {
      throwables.printStackTrace();
      player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your xp from our database. Please open a ticket on the discord!");

      return -1;
    }
  }
  public static int getPlayerXP(Player player) {
    try (PreparedStatement statement = connection.prepareStatement("SELECT xp FROM skywarsPlayers WHERE uuid=?;")) {
      statement.setString(1, player.getUniqueId().toString());
      ResultSet res = statement.executeQuery();
      if (!res.next()) {
        throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
      }
      return res.getInt(1);
    } catch (SQLException throwables) {
      throwables.printStackTrace();
      player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your xp from our database. Please open a ticket on the discord!");

      return -1;
    }
  }
  public static void setScoreboard(Player player, Scoreboard scoreboard) {
    player.setScoreboard(scoreboard);
  }

  public static String ordinal(int i) {
    int mod100 = i % 100;
    int mod10 = i % 10;
    if (mod10 == 1 && mod100 != 11)
      return i + "st";
    if (mod10 == 2 && mod100 != 12)
      return i + "nd";
    if (mod10 == 3 && mod100 != 13) {
      return i + "rd";
    }
    return i + "th";
  }

  public static Scoreboard createScoreboard(String displayName, String[] scores) {
    Scoreboard board = instance.sm.getNewScoreboard();
    Objective objective = board.registerNewObjective("scoreboard", "dummy");
    objective.setDisplayName(displayName);
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    int numSpaces = 0;
    int numResets = 1;
    for (int i = 0; i < scores.length; i++) {
      if (scores[i].equals("")) {
        objective.getScore(String.join("", Collections.nCopies(numSpaces, " "))).setScore(scores.length - i);
        numSpaces++;
      } else if (scores[i].startsWith("%")) {
        int percent = scores[i].substring(1).indexOf('%') + 1;
        String teamName = scores[i].substring(1, percent);
        Team team = board.registerNewTeam(teamName);
        String entry = String.join("", Collections.nCopies(numResets, "§r"));
        team.addEntry(entry);
        String content = scores[i].substring(percent + 1);
        int split = content.indexOf("%");
        if (split == -1) {
          team.setPrefix(content);
        } else {
          team.setPrefix(content.substring(0, split));
          team.setSuffix(content.substring(split + 1));
        }
        objective.getScore(entry).setScore(scores.length - i);
        numResets++;
      } else {
        objective.getScore(scores[i]).setScore(scores.length - i);
      }
    }

    return board;
  }


  public void openConnection() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      return;
    }



    connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?characterEncoding=latin1&autoReconnect=true", this.username, this.password);
  }

  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    if (!channel.equals("BungeeCord")) {
      return;
    }
    ByteArrayDataInput in = ByteStreams.newDataInput(message);
    String subchannel = in.readUTF();
    if (subchannel.equals("SetQueueing")) {
      queueingEnabled = in.readBoolean();
    }
  }
}