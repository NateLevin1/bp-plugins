package net.bridgepractice.bpbungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BPBungee extends Plugin implements Listener {
    public static BPBungee instance;
    public static LuckPerms luckPerms;
    public static HashMap<MultiplayerMode, ArrayList<String>> queueingGames = new HashMap<>();
    public static HashMap<UUID, String> playerChatChannels = new HashMap<>();
    public static HashMap<UUID, UUID> playerMessageChannel = new HashMap<>();
    public static boolean multiplayerEnabled = true;
    public static boolean chatEnabled = true;
    public static String punishmentWebhook = "https://discord.com/api/webhooks/888106865697894410/bPuDlfi_RXBdY7ulqS_U9JT62rWbsSF_C45SQVM24rb2p4db3mhRWCIwj7peG6a-9zEs";

    @Override
    public void onEnable() {
        instance = this;
        luckPerms = LuckPermsProvider.get();

        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Lobby());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Hub());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Discord());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Stat());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Message());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Reply());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Freeze());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Unfreeze());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Mute());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Unmute());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Warn());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Help());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Rules());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new ChangeMultiplayer());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Play());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Duel());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new StaffChat());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new RankChat());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AllChat());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Ping());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Ban());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Unban());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Mutechat());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Unmutechat());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new RankInfo());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new ChangeTag());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Store());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Emotes());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new EditBan());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Immuted());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Whitelist());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Socialspy());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new SetChat());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new GetQueueingGames());
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("bp:messages");

        openConnection();

        queueingGames.put(MultiplayerMode.unranked, new ArrayList<>());
        queueingGames.put(MultiplayerMode.pvp, new ArrayList<>());
        queueingGames.put(MultiplayerMode.nobridge, new ArrayList<>());
    }

    HashMap<UUID, Integer> mutedPlayers = new HashMap<>();
    public final HashMap<UUID, NamedPlayer> playerReplyTo = new HashMap<>();
    public final HashMap<UUID, Long> gameRequests = new HashMap<>();
    public static final String frozenMessage = "§c§lYou have been frozen by our mod team.\n\n§fTo continue playing, you must join the Discord and open a ticket.\nInclude your IGN and any info you think we need in the ticket.\n\n\n§6Join the Discord by going to §b§nbridgepractice.net/discord\n§6Then accept the rules, go to #support, and click the \"Support\" button.";

    public static class NamedPlayer {
        public String name;
        public String rankedName;
        NamedPlayer(String name, String rankedName) {
            this.name = name;
            this.rankedName = rankedName;
        }
    }

    // db related things
    String host = "localhost";
    String port = "3306";
    String database = "bridge";
    String username = "mc";
    String password = "mcserver";
    static Connection connection;

    public void openConnection() {
        try {
            if(connection != null && !connection.isClosed()) {
                return;
            }
            // NOTE: If something around this are fails, something is different between my host machine and the machine
            //       this is running on. Getting rid of the `characterEncoding` query parameter may help, but other
            //       solutions are likely.
            connection = DriverManager.getConnection("jdbc:mysql://"
                            + this.host + ":" + this.port + "/" + this.database + "?characterEncoding=latin1&autoReconnect=true",
                    this.username, this.password);

            CommandQueueChecker.startChecking();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    HashMap<UUID, Long> playerSessionLogOnTime = new HashMap<>();
    @EventHandler
    public void onLogin(LoginEvent event) {
        UUID uuid = event.getConnection().getUniqueId();
        // check if banned
        try(PreparedStatement statement = connection.prepareStatement("SELECT players.bannedAt, players.uuid, players.bannedDays, players.bannedReason FROM bannedIps INNER JOIN players ON bannedIps.uuid=players.uuid WHERE ip=?;")) {
            statement.setString(1, event.getConnection().getAddress().getAddress().toString());
            ResultSet res = statement.executeQuery();
            if(res.next()) {
                // player's ip is in fact banned
                Date bannedAt = res.getDate("bannedAt");
                if(!res.wasNull()) {
                    int bannedDays = res.getInt("bannedDays");
                    int daysSince = (int) ChronoUnit.DAYS.between(bannedAt.toLocalDate(), LocalDate.now());
                    if(daysSince < bannedDays) {
                        event.setCancelReason(Utils.getBanMessage(bannedDays - daysSince, res.getString("bannedReason"), !res.getString("uuid").equals(uuid.toString())));
                        event.setCancelled(true);
                        return;
                    } else {
                        Unban.applyUnban(res.getString("uuid"), null);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try(PreparedStatement statement = connection.prepareStatement("SELECT frozen, bannedAt, bannedDays, bannedReason, mutedAt, mutedDays FROM players WHERE uuid=?;")) {
            statement.setString(1, uuid.toString()); // uuid
            ResultSet res = statement.executeQuery();
            if(!res.next()) {
                checkIpForAbuse(event.getConnection());
                return; // perfectly acceptable since the player might have never logged in
            }
            boolean frozen = res.getBoolean("frozen");
            if(frozen) {
                event.setCancelReason(new ComponentBuilder(frozenMessage).create());
                event.setCancelled(true);
                return;
            }

            Date bannedAt = res.getDate("bannedAt");
            if(!res.wasNull()) {
                int bannedDays = res.getInt("bannedDays");
                int daysSince = (int) ChronoUnit.DAYS.between(bannedAt.toLocalDate(), LocalDate.now());
                if(daysSince < bannedDays) {
                    event.setCancelReason(Utils.getBanMessage(bannedDays - daysSince, res.getString("bannedReason"), false));
                    event.setCancelled(true);
                    return;
                } else {
                    Unban.applyUnban(uuid.toString(), null);
                }
            }

            Date mutedAt = res.getDate("mutedAt");
            if(!res.wasNull()) {
                int mutedDays = res.getInt("mutedDays");
                int daysSince = (int) ChronoUnit.DAYS.between(mutedAt.toLocalDate(), LocalDate.now());
                if(daysSince < mutedDays) {
                    mutedPlayers.put(uuid, mutedDays - daysSince);
                } else {
                    // they aren't muted but the DB says they are - remove the mute from the DB - this makes it so we don't have to compute the distance every time
                    try(PreparedStatement updateMutedAt = connection.prepareStatement("UPDATE players SET mutedAt = NULL, mutedDays = NULL WHERE uuid=?;")) {
                        updateMutedAt.setString(1, uuid.toString()); // uuid, set to player uuid
                        updateMutedAt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if(Whitelist.enabled) {
            ServerPing ping = event.getResponse();
            ping.setVersion(new ServerPing.Protocol("Maintenance", 0));
            ping.setDescription("§a              §c✕§a  bridge§bpractice§a.net  §c✕\n§c       Under maintenance, check back later!");
            event.setResponse(ping);
        }
    }
    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if(Whitelist.enabled && !player.hasPermission("group.mod")) {
            player.disconnect(new ComponentBuilder("BridgePractice is currently under maintenance!\nPlease come back in a bit!").color(ChatColor.RED).create());
            return;
        }
        User luckPermsUser = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
        String prefix = luckPermsUser.getCachedData().getMetaData().getPrefix();
        if(prefix == null) return;
        String rankedName = prefix + "§";
        if(player.hasPermission("group.admin") || player.hasPermission("group.youtube")) {
            rankedName += "c";
        } else if(!player.hasPermission("group.custom") && (player.hasPermission("group.godlike") || player.hasPermission("group.legend"))) {
            rankedName += prefix.charAt(4);
        } else if(rankedName.startsWith("§")) {
            rankedName += prefix.charAt(1);
        }
        rankedName += player.getName();
        player.setDisplayName(rankedName);
        playerSessionLogOnTime.put(player.getUniqueId(), System.currentTimeMillis());

        // Staff Join Message
        if (player.hasPermission("group.helper") || player.hasPermission("group.youtube")) {
            for (ProxiedPlayer p : BPBungee.instance.getProxy().getPlayers()) {
                if ((p.hasPermission("group.mod")) && !p.equals(player)) {
                    p.sendMessage(new ComponentBuilder("[STAFF]").color(ChatColor.AQUA) // [STAFF]
                            .append(" ") // Space
                            .append(player.getDisplayName()) // Rank + Name
                            .append(" ") // Space
                            .append("connected.").color(ChatColor.YELLOW)
                            .create());
                }
            }
        }
    }
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = event.getPlayer().getUniqueId();
        mutedPlayers.remove(uuid);
        playerReplyTo.remove(uuid);
        gameRequests.remove(uuid);
        playerChatChannels.remove(uuid);
        Long logOnTime = playerSessionLogOnTime.get(uuid);
        if(logOnTime != null) {
            int playingTimeMinutes = Math.round((int) (((System.currentTimeMillis() - logOnTime) / 1000) / 60));
            try(PreparedStatement playingTimeUpdate = connection.prepareStatement("UPDATE players SET playingTime = playingTime + ? WHERE uuid=?;")) {
                playingTimeUpdate.setInt(1, playingTimeMinutes); // playingTime, set to playingTimeMinutes
                playingTimeUpdate.setString(2, uuid.toString()); // uuid, set to player uuid
                playingTimeUpdate.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            playerSessionLogOnTime.remove(uuid);
        }

        // Staff Leave Message
        if (player.hasPermission("group.helper") || player.hasPermission("group.youtube")) {
            for (ProxiedPlayer p : BPBungee.instance.getProxy().getPlayers()) {
                if ((p.hasPermission("group.mod")) && !p.equals(player)) {
                    p.sendMessage(new ComponentBuilder("[STAFF]").color(ChatColor.AQUA) // [STAFF]
                            .append(" ") // Space
                            .append(player.getDisplayName()) // Rank + Name
                            .append(" ") // Space
                            .append("disconnected.").color(ChatColor.YELLOW)
                            .create());
                }
            }
        }
    }
    List<String> blockedCommandsIfMuted = Arrays.asList("msg", "r", "w", "message", "reply", "rainbow");
    List<String> blockedCommands = Arrays.asList("/worldedit:/calc", "/worldedit:/calculate", "/worldedit:/eval", "/worldedit:/evaluate", "/worldedit:/solve");
    private final String[] blockedWords = {"nigga", "nigger", "anigger", "anigga", "aniga", "aniger", "niger", "niga", "fag", "faggot", "retard", "n1ger", "sex", "esex", "cum"};
    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        if(event.isCommand()) {
            if(blockedCommands.contains(event.getMessage().split(" ")[0])) {
                event.setCancelled(true);
                return;
            }
        }
        net.md_5.bungee.api.connection.Connection sender = event.getSender();
        if(!(sender instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = ((ProxiedPlayer) sender);

        if(!event.isCommand() && !chatEnabled) {
            event.setCancelled(true);
            player.sendMessage(new ComponentBuilder("Chat has been temporarily disabled.").color(ChatColor.RED).create());
            return;
        }

        // add emojis
        if(event.isProxyCommand()) {
            event.setMessage(addEmojisToMessage(event.getMessage()));
        }

        boolean isMessageToOthers = !event.isCommand() || blockedCommandsIfMuted.contains(event.getMessage().split(" ")[0].substring(1));
        if (event.getMessage().startsWith("/ac ")) {
            isMessageToOthers = true;
            event.setMessage(event.getMessage().replaceAll("/ac ", ""));
        }

        if(isMessageToOthers) {
            if(event.getMessage().replaceAll(/* remove jokes */"(?:1\\.){3}1|(?:(?:69|420)\\.){3}(?:69|420)|1[.,|!\\s]\\s*2[.,|!\\s]\\s*3[.,|!\\s]\\s*4|(?:1[.,|!\\s]\\s*){4}", "").matches(".*(((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9])(\\s*[.,|]\\s*|$)){4}).*")) {
                // INSTANTLY BAN IF SEND AN IP ADDRESS
                BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
                    Ban.applyBan(player.getName(), 30, "Doxxing/Attempt to dox", player.getUniqueId().toString(), false, null);
                    Utils.sendPunishmentWebhook(true, "automatically banned", "Doxxing/Attempt to dox\n> " + event.getMessage() + "", 30, "Server", "SERVER", player.getName(), null);
                }, 0, TimeUnit.MILLISECONDS);
                event.setCancelled(true);
                return;
            } else if(event.getMessage().matches(".* >[\\w\\d]{4,11}<")) {
                // liquidbounce always follows this format
                BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
                    Ban.applyBan(player.getName(), 7, "Chat Abuse/Scam", player.getUniqueId().toString(), true, null);
                    Utils.sendPunishmentWebhook(true, "automatically banned", "Chat Abuse/Scam\n> " + event.getMessage() + "", 7, "Server", "SERVER", player.getName(), null);
                }, 0, TimeUnit.MILLISECONDS);
                event.setCancelled(true);
                return;
            } else if(event.getMessage().replaceAll("(https?://)?bridgepractice\\.net", "").replace("exe", "").matches(".*(http|[\\w(\\[{#^'\".,|]+\\s*[.,][a-zA-Z]{2,8}(\\W|$)|gg/.+).*")) {
                player.sendMessage(new ComponentBuilder()
                        .append(new ComponentBuilder("---------------------------------------").color(ChatColor.GOLD).strikethrough(true).create())
                        .append(new ComponentBuilder("\nAdvertising is against the rules. You will be\npermanently banned from the server if you\nattempt to advertise.\n").color(ChatColor.RED).strikethrough(false).create())
                        .append(new ComponentBuilder("---------------------------------------").color(ChatColor.GOLD).strikethrough(true).create())
                        .create());
                event.setCancelled(true);
                Utils.log("§e"+player.getName()+" §3attempted to §aadvertise§3: §f"+event.getMessage());
                return;
            }
            String filteredMessage = event.getMessage().toLowerCase().replaceAll("[\"'.:;,|`~!@#$%^&*()_\\-+={}\\[\\]/\\\\?<>]", "");
            for(String word : blockedWords) {
                if(filteredMessage.startsWith(word) || filteredMessage.contains(" " + word)) {
                    event.setCancelled(true);
                    player.sendMessage(new ComponentBuilder()
                            .append(new ComponentBuilder("---------------------------------------").color(ChatColor.DARK_RED).strikethrough(true).create())
                            .append(new ComponentBuilder("\nBlocked message containing disallowed word.\nYou will be muted if you continue using blocked words or attempt to bypass this filter.").color(ChatColor.RED).strikethrough(false).create())
                            .append(new ComponentBuilder("---------------------------------------").color(ChatColor.DARK_RED).strikethrough(true).create())
                            .create());
                    Utils.log("§e"+player.getName()+" §3attempted to say §c§lblocked words§3: §f"+event.getMessage());
                    return;
                }
            }
        }

        // if player is muted
        boolean isMuted = mutedPlayers.containsKey(player.getUniqueId());
        if(isMuted) {
            // if is command and is not a command in the blocked list
            if(event.isCommand() && !blockedCommandsIfMuted.contains(event.getMessage().split(" ")[0].substring(1))) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(new ComponentBuilder()
                    .append(new ComponentBuilder("----------------------------------------------------------------").color(ChatColor.RED).strikethrough(true).create())
                    .append(new ComponentBuilder("\nYou cannot chat because you are muted.").strikethrough(false).color(ChatColor.RED).create())
                    .append(new ComponentBuilder("\nYour mute will expire in ").color(ChatColor.GRAY).append(new ComponentBuilder(mutedPlayers.get(player.getUniqueId()) + " days").color(ChatColor.RED).create()).create())
                    .append(new ComponentBuilder("\n\nTo appeal your mute, ").color(ChatColor.GRAY).append(new ComponentBuilder("join the Discord (click)").color(ChatColor.AQUA).underlined(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://bridgepractice.net/discord")).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to go to the Discord invite."))).create()).create())
                    .append(new ComponentBuilder("\n----------------------------------------------------------------").color(ChatColor.RED).strikethrough(true).underlined(false).event(((ClickEvent) null)).event(((HoverEvent) null)).create())
                    .create());
        }

        String chatChannel = playerChatChannels.get(player.getUniqueId());
        if (chatChannel != null && chatChannel.equals("staff") && !event.isCommand()) {
            event.setCancelled(true);
            String text = event.getMessage();
            StaffChat.sendToStaffChat(player.getDisplayName(), text);
            return;
        }
        if (chatChannel != null && chatChannel.equals("rank") && !event.isCommand()) {
            event.setCancelled(true);
            String text = event.getMessage();
            RankChat.sendToRankChat(player.getDisplayName(), text);
            return;
        }
        if (chatChannel != null && chatChannel.equals("message") && !event.isCommand()) {
            event.setCancelled(true);
            ProxiedPlayer playerToSendMessage = BPBungee.instance.getProxy().getPlayer(BPBungee.playerMessageChannel.get(player.getUniqueId()));
            String text = event.getMessage();
            player.sendMessage(new ComponentBuilder("§dTo "+playerToSendMessage.getDisplayName()).append(": "+text).color(ChatColor.GRAY).create());
            playerToSendMessage.sendMessage(new ComponentBuilder("§dFrom "+player.getDisplayName()).append(": "+text).color(ChatColor.GRAY).create());
            BPBungee.instance.playerReplyTo.put(playerToSendMessage.getUniqueId(), new BPBungee.NamedPlayer(player.getName(), player.getDisplayName()));
            Utils.log(new ComponentBuilder("SocialSpy: ").color(ChatColor.AQUA).append("From "+player.getDisplayName()).color(ChatColor.LIGHT_PURPLE).append(" To "+playerToSendMessage.getDisplayName()).color(ChatColor.LIGHT_PURPLE).append(": "+text).color(ChatColor.GRAY).create(), "bridgepractice.moderation.socialspy");
            return;
        }
    }
    private String addEmojisToMessage(String msg) {
        msg = msg.replace("<3", "§c❤§f");
        msg = msg.replace(":eyes:", "⌊●_●⌋");
        msg = msg.replace(":eyes2:", "⊙_⊙");
        msg = msg.replace(":snowman:", "§b☃§f");
        msg = msg.replace(":star:", "§e★§f");
        msg = msg.replace(":smile:", "§e☻§f");
        msg = msg.replace(":frown:", "§9☹§f");
        msg = msg.replace(":x:", "§c✕§f");
        msg = msg.replace(":cross:", "§c✕§f");
        msg = msg.replace(":y:", "§a✔§f");
        msg = msg.replace(":check:", "§a✔§f");
        msg = msg.replace(":tm:", "§b™§f");
        msg = msg.replace(":cry:", "§9（>﹏<）§f");
        msg = msg.replace(":why:", "§6(｢•-•)｢ why?§f");
        msg = msg.replace(":sunglasses:", "§68§e)§f");
        msg = msg.replace(":sunglasses2:", "§6B§e)§f");
        msg = msg.replace(":fire:", "§c♨♨♨§f");
        msg = msg.replace(":fire2:", "§cѰѰѰ§f");
        msg = msg.replace(":+1:", "§6⬆§f");
        msg = msg.replace(":thumbsup:", "§6(§eb§9^§7_§9^§6)§eb§f");
        msg = msg.replace(":thumbsup2:", "§6⬈§f");
        msg = msg.replace(":-1:", "§c⬇§f");
        msg = msg.replace(":thumbsdown:", "§c⬊§f");
        msg = msg.replace(":bruh:", "§c(P-_-)P§f");
        msg = msg.replace(":bear:", "§6ʕ•ᴥ•ʔ§f");
        msg = msg.replace(":shrug:", "¯\\_(°-°)_/¯");
        msg = msg.replace(":cash:", "§2[§a$§2(§a$$$§2)§a$§2]§f");
        msg = msg.replaceAll(":(\\d+)([$€£RP]):", "§2[§a$2§2(§a$1§2)§a$2§2]§f");
        return msg;
    }

    public String getPlayerPlayingTimeSync(ProxiedPlayer player) {
        try(PreparedStatement statement = connection.prepareStatement("SELECT playingTime FROM players WHERE uuid=?;")) {
            statement.setString(1, player.getUniqueId().toString()); // uuid
            ResultSet res = statement.executeQuery();
            if(!res.next()) {
                throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
            }
            int playingTimeMinutes = Math.round((int) (((System.currentTimeMillis() - playerSessionLogOnTime.get(player.getUniqueId())) / 1000) / 60));
            int totalMinutes = (res.getInt(1) + playingTimeMinutes);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            return hours + " hour" + (hours == 1 ? "" : "s") + " " + minutes + " min" + (minutes == 1 ? "" : "s");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your playing time from our database. Please open a ticket on the discord!");
        }
        return "N/A";
    }
    public void requestGame(String gameName, ProxiedPlayer player) {
        if(System.currentTimeMillis() - gameRequests.getOrDefault(player.getUniqueId(), 0L) > 3000) {
            gameRequests.put(player.getUniqueId(), System.currentTimeMillis());
            ArrayList<String> queuingGame = queueingGames.get(MultiplayerMode.valueOf(gameName));
            if(!(multiplayerEnabled || (queuingGame.size() > 0))) { // ensure there are no games left un-queued even if disabled
                player.sendMessage(new ComponentBuilder("Multiplayer modes are temporarily disabled!").color(ChatColor.RED).create());
                return;
            }
            BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
                ServerInfo multiplayerServer = ProxyServer.getInstance().getServerInfo("multiplayer_1");
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                player.sendMessage(new ComponentBuilder("Sending you to the server...").color(ChatColor.GREEN).create());
                if(queuingGame.size() == 0 || multiplayerServer.getPlayers().size() == 0) {
                    out.writeUTF("StartGame");
                    out.writeUTF(gameName);
                    out.writeUTF(player.getName());
                    if(multiplayerServer.getPlayers().size() == 0) {
                        // if there are no players online we need to connect them so we can send a message
                        player.connect(ProxyServer.getInstance().getServerInfo("multiplayer_1"));
                        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
                            player.getServer().getInfo().sendData("bp:messages", out.toByteArray());
                        }, 1000, TimeUnit.MILLISECONDS);
                    } else {
                        multiplayerServer.sendData("bp:messages", out.toByteArray());
                    }
                } else {
                    // we can assume there are players online because we check above
                    out.writeUTF("IntentToJoinGame");
                    out.writeUTF(queuingGame.remove(0));
                    out.writeUTF(player.getName());
                    multiplayerServer.sendData("bp:messages", out.toByteArray());

                    player.connect(multiplayerServer);
                }
            }, 0, TimeUnit.MILLISECONDS);
        } else {
            player.sendMessage(new ComponentBuilder("You are sending too many requests! Please slow down.").color(ChatColor.RED).create());
        }

    }
    public boolean isWorldQueueing(String worldName, MultiplayerMode mode) {
        ArrayList<String> queueingGamesForMode = queueingGames.get(mode);
        return queueingGamesForMode.contains(worldName);
    }
    public void sendIntentToJoinGame(String worldName, MultiplayerMode mode, ProxiedPlayer player) {
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
            ServerInfo multiplayerServer = ProxyServer.getInstance().getServerInfo("multiplayer_1");
            ArrayList<String> queueingGamesForMode = queueingGames.get(mode);
            int index = queueingGamesForMode.indexOf(worldName);
            if(index == -1) {
                return;
            }
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            player.sendMessage(new ComponentBuilder("Sending you to the server...").color(ChatColor.GREEN).create());
            out.writeUTF("IntentToJoinGame");
            out.writeUTF(queueingGamesForMode.remove(index));
            out.writeUTF(player.getName());
            multiplayerServer.sendData("bp:messages", out.toByteArray());

            player.connect(multiplayerServer);
        }, 0, TimeUnit.MILLISECONDS);
    }
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if(!event.getTag().equals("BungeeCord")) return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
        try {
            String channel = in.readUTF();
            switch(channel) {
                case "GetPlayerPlayingTime": {
                    ProxiedPlayer player = ((ProxiedPlayer) event.getReceiver());
                    getProxy().getScheduler().schedule(this, () -> {
                        String playingTime = getPlayerPlayingTimeSync(player);
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("ReturnPlayerPlayingTime");
                        out.writeUTF(playingTime);
                        out.writeUTF(player.getUniqueId().toString());
                        player.getServer().getInfo().sendData("bp:messages", out.toByteArray());
                    }, 0, TimeUnit.MILLISECONDS);
                    break;
                }
                case "SetGameQueueing": {
                    String gameMode = in.readUTF();
                    String worldName = in.readUTF();
                    MultiplayerMode multiplayerMode = MultiplayerMode.valueOf(gameMode);
                    ArrayList<String> queuingWorlds = queueingGames.get(multiplayerMode);
                    queuingWorlds.add(worldName);
                    getProxy().getScheduler().schedule(this, ()->{
                        if(queuingWorlds.contains(worldName)) {
                            ProxiedPlayer queueingPlayer = null;
                            if(event.getReceiver() instanceof ProxiedPlayer) {
                                queueingPlayer = ((ProxiedPlayer) event.getReceiver());
                                if(!queueingPlayer.getServer().getInfo().getName().equals("multiplayer_1")) {
                                    return;
                                }
                            }
                            BaseComponent[] queuingMessage = new ComponentBuilder("\n").append(Utils.getGameModeName(multiplayerMode))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to join this "+Utils.getGameModeName(multiplayerMode)+" game!")))
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/q ifavailable "+worldName+" "+gameMode))
                                    .color(ChatColor.LIGHT_PURPLE).bold(true).append(" >> ").bold(false).color(ChatColor.GRAY).append("A player is waiting in queue! ").color(ChatColor.GOLD).append("(Click to Join)").color(ChatColor.GREEN).underlined(true).create();
                            int numAdvertisedTo = 0;
                            for(ProxiedPlayer player : getProxy().getPlayers()) {
                                if(!player.getServer().getInfo().getName().equals("multiplayer_1")) {
                                    player.sendMessage(queuingMessage);
                                    player.sendMessage();
                                    numAdvertisedTo++;
                                }
                            }
                            if(queueingPlayer == null) return;
                            queueingPlayer.sendMessage(new ComponentBuilder("\nYour game has been advertised to ").color(ChatColor.GREEN).append(numAdvertisedTo+"").color(ChatColor.AQUA).bold(true).append(" players since it has not queued!").color(ChatColor.GREEN).bold(false).create());
                            queueingPlayer.sendMessage();
                        }
                    }, 20, TimeUnit.SECONDS);
                    break;
                }
                case "RemoveGameQueueing": {
                    String gameMode = in.readUTF();
                    String worldName = in.readUTF();
                    queueingGames.get(MultiplayerMode.valueOf(gameMode)).remove(worldName);
                    break;
                }
                case "RequestGame": {
                    String gameName = in.readUTF();
                    ProxiedPlayer player = ((ProxiedPlayer) event.getReceiver());
                    requestGame(gameName, player);
                    break;
                }
                case "DuelPlayer": {
                    ProxiedPlayer requester = ((ProxiedPlayer) event.getReceiver());
                    String playerToDuelName = in.readUTF();
                    String gameType = in.readUTF();
                    Duel.sendDuelRequest(requester, playerToDuelName, gameType, null);
                    break;
                }
                case "NewBPReport": {
                    String reporterName = in.readUTF();
                    String reportedName = in.readUTF();
                    String reason = in.readUTF();
                    Utils.log(new ComponentBuilder("[REPORT] ").color(ChatColor.RED)
                            .append(new ComponentBuilder(reporterName).color(ChatColor.GOLD).create())
                            .append(new ComponentBuilder(" reported ").color(ChatColor.RED).create())
                            .append(new ComponentBuilder(reportedName).color(ChatColor.YELLOW).create())
                            .append(new ComponentBuilder(" for ").color(ChatColor.RED).create())
                            .append(new ComponentBuilder(reason).color(ChatColor.YELLOW).create())
                            .create());
                    break;
                }
                case "NewBPChatReport": {
                    String reporterName = in.readUTF();
                    String reportedName = in.readUTF();
                    String reason = in.readUTF();
                    String messages = in.readUTF();
                    Utils.log(new ComponentBuilder("[REPORT] ").color(ChatColor.RED)
                            .append(new ComponentBuilder(reporterName).color(ChatColor.GOLD).create())
                            .append(new ComponentBuilder(" chat reported ").color(ChatColor.RED).create())
                            .append(new ComponentBuilder(reportedName).color(ChatColor.YELLOW).create())
                            .append(new ComponentBuilder(" for ").color(ChatColor.RED).create())
                            .append(new ComponentBuilder(reason).color(ChatColor.YELLOW).create())
                            .append(new ComponentBuilder(". Messages:\n" + messages).color(ChatColor.GOLD).create())
                            .create());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int checksInLastMinute = 0;
    private static long lastResetOfChecksMinute = 0;
    private static int checksInLastDay = 0;
    private static long lastResetOfChecksDay = 0;
    private static final HashSet<String> badIps = new HashSet<>();

    public void checkIpForAbuse(PendingConnection connection) {
        // this method is called on a player's first login
        String ip = connection.getAddress().getAddress().getHostAddress();
        String playerName = connection.getName();

        if(badIps.contains(ip)) {
            disconnectBadIp(connection, ip);
            return;
        }

        // respect rate limits
        if(System.currentTimeMillis() - lastResetOfChecksMinute > 60*1000) {
            lastResetOfChecksMinute = System.currentTimeMillis();
            checksInLastMinute = 0;
        }

        if(System.currentTimeMillis() - lastResetOfChecksDay > 24*60*60*1000) {
            lastResetOfChecksDay = System.currentTimeMillis();
            checksInLastDay = 0;
            badIps.clear();
        }

        checksInLastMinute++;
        checksInLastDay++;

        if(checksInLastMinute > 15 || checksInLastDay > 500) {
            // oh well :(
            String message = "Unable to check IP of player "+playerName+" because there were too many checks in the last minute: "+checksInLastMinute+" or too many checks in the last day: "+checksInLastDay;
            getLogger().severe(message);
            Utils.log(new ComponentBuilder("[IPCheck] ").color(ChatColor.DARK_RED).append(new ComponentBuilder(message).color(ChatColor.RED).create()).create());
            return;
        }

        // call the https://www.getipintel.net API
        try {
            URL url = new URL("http://check.getipintel.net/check.php?ip=" + ip + "&contact=natelevindev@gmail.com&flags=m");
            URLConnection con = url.openConnection();
            HttpURLConnection req = ((HttpURLConnection) con);
            req.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15");
            req.connect();
            int responseCode = req.getResponseCode();
            if(responseCode < 200 || responseCode >= 300) {
                try(BufferedReader response = new BufferedReader(new InputStreamReader(req.getErrorStream()))) {
                    String errno = response.readLine();
                    Utils.log(new ComponentBuilder("[IPCheck]").color(ChatColor.DARK_RED).append(new ComponentBuilder(" Error checking IP for abuse: " + responseCode + " " + req.getResponseMessage() + " " + errno).color(ChatColor.RED).create()).create());
                    getLogger().severe("Error checking IP for abuse: " + responseCode + " " + req.getResponseMessage() + " Error number=" + errno);
                }
                return;
            }
            try(BufferedReader in = new BufferedReader(new InputStreamReader(req.getInputStream()))) {
                String resString = in.readLine();
                int res = Integer.parseInt(resString);
                // res is between 0 and 1
                if(res >= 0.995) {
                    badIps.add(ip);
                    disconnectBadIp(connection, ip);
                } else if(res >= 0.9) {
                    Utils.log(new ComponentBuilder("[IPCheck]").color(ChatColor.DARK_RED).append(new ComponentBuilder(" Player ").color(ChatColor.RED).create()).append(new ComponentBuilder(playerName).color(ChatColor.YELLOW).create()).append(new ComponentBuilder(" has a suspicious IP ("+((res-0.9)*1000)+"%)").color(ChatColor.RED).create()).create());
                }
            }
        } catch (IOException e) {
            getLogger().severe("Error checking IP for abuse: ");
            e.printStackTrace();
            Utils.log(new ComponentBuilder("[IPCheck]").color(ChatColor.DARK_RED).append(new ComponentBuilder(" Error checking IP for abuse: IOException: "+e.getMessage()).color(ChatColor.RED).create()).create());
        }
    }
    private static void disconnectBadIp(PendingConnection connection, String ip) {
        connection.disconnect(new ComponentBuilder("Your IP ").color(ChatColor.RED)
                .append("("+ip+")").bold(true)
                .append(" appears to be related to known bad IPs\nso you have been blocked from joining the server.\n\n").bold(false)
                .append("If you are using a VPN/proxy, disable it to play.").bold(true).color(ChatColor.WHITE)
                .append("\n\nIf you are not using a VPN and are still being blocked,\nplease join the Discord and report it by making a ticket in #support.").bold(false).color(ChatColor.GOLD)
                .append("\nTo join the discord, go to ").color(ChatColor.WHITE)
                .append("https://bridgepractice.net/discord").color(ChatColor.AQUA).underlined(true)
                .create());
    }
}
