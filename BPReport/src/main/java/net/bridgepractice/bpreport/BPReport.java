package net.bridgepractice.bpreport;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class BPReport extends JavaPlugin implements Listener, PluginMessageListener {
    public static String discordWebhook = "https://discord.com/api/webhooks/874654594502918184/qcutz-EZ9HEyb8LuvPGbi8GyhoCOEnbbBoIiG5DwnkiCylgyf9YN4b7HbHgDGWsLNwDY";
    public static BPReport instance;
    private final HashMap<UUID, String[]> playerChatMessages = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "bp:messages", this);

        CommandReport reportCommand = new CommandReport();
        this.getCommand("report").setExecutor(reportCommand);
        this.getCommand("wdr").setExecutor(reportCommand);
        this.getCommand("chatreport").setExecutor((sender, command, label, args) -> {
            if(args.length == 0) {
                sender.sendMessage("§cYou need to provide a player for this command!");
                return true;
            }
            if(sender instanceof Player) {
                ((Player) sender).chat("/report " + args[0] + " chat");
            } else {
                sender.sendMessage("§cOnly players can use this command!");
            }
            return true;
        });

        // reset chat messages every 2 days
        (new BukkitRunnable() {
            int lastDay = -1;
            @Override
            public void run() {
                Calendar calender = Calendar.getInstance();
                calender.setTimeInMillis(System.currentTimeMillis());
                int hour = calender.get(Calendar.HOUR_OF_DAY);
                if(hour == 4) {
                    int day = calender.get(Calendar.DAY_OF_WEEK);
                    if(day == lastDay) return;
                    lastDay = day;

                    getServer().broadcast("\n§7§k|||||§r", "bridgepractice.moderation.chat");
                    getServer().broadcast("§7§k|||||§r  §c[BPReport] §4§lResetting message logs...", "bridgepractice.moderation.chat");
                    getServer().broadcast("§7§k|||||§r", "bridgepractice.moderation.chat");
                    getServer().broadcast("", "bridgepractice.moderation.chat");

                    playerChatMessages.clear();
                }
            }
        }).runTaskTimer(this, 0, (60 * 20) * 20);
    }
    enum ReportReason {
        Cheating,
        Chat,
        Alt,
    }
    public static void report(Player reporter, String reportedName, ReportReason reason) {
        BukkitRunnable asyncSendReport = new BukkitRunnable() {
            @Override
            public void run() {
                String prettyReason = null;
                switch(reason) {
                    case Cheating:
                        prettyReason = "Cheating";
                        break;
                    case Chat:
                        prettyReason = "Chat Abuse/Scam";
                        break;
                    case Alt:
                        prettyReason = "Alting";
                        break;
                }

                instance.getLogger().info("\u001B[31mPlayer `" + reporter.getName() + "` reported `" + reportedName + "` for " + prettyReason + ".\u001B[0m");
                try {
                    String rUuid;
                    try {
                        rUuid = Utils.getUuidFromName(reportedName);
                    } catch (IOException e) {
                        reporter.sendMessage("§c✕ Report failed: '" + reportedName + "' is not a valid username");
                        return;
                    }

                    // send a discord message embed to #server-reports using a webhook

                    JsonObject webhook = new JsonObject();
                    JsonArray embeds = new JsonArray();
                    JsonObject embed = new JsonObject();
                    JsonObject author = new JsonObject();
                    JsonArray fields = new JsonArray();

                    webhook.add("embeds", embeds);
                    embeds.add(embed);

                    embed.addProperty("color", 0x39c2ff);

                    embed.add("author", author);
                    author.addProperty("name", reporter.getName() + " made a new report");
                    author.addProperty("icon_url", "https://crafatar.com/renders/head/" + reporter.getUniqueId() + "?overlay=true&size=64");

                    embed.add("fields", fields);
                    JsonObject playerField = new JsonObject();
                    playerField.addProperty("name", "Player");
                    playerField.addProperty("value", reportedName);
                    playerField.addProperty("inline", true);
                    fields.add(playerField);

                    JsonObject reasonField = new JsonObject();
                    reasonField.addProperty("name", "Reason");
                    reasonField.addProperty("value", prettyReason);
                    reasonField.addProperty("inline", true);
                    fields.add(reasonField);

                    JsonObject thumbnail = new JsonObject();
                    embed.add("thumbnail", thumbnail);
                    thumbnail.addProperty("url", "https://crafatar.com/avatars/" + rUuid + "?overlay=true&size=64");

                    embed.addProperty("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

                    if(reason == ReportReason.Chat) {
                        JsonObject messagesField = new JsonObject();
                        messagesField.addProperty("name", "Messages");
                        String[] chatMessages = instance.playerChatMessages.get(UUID.fromString(rUuid));
                        String messagesString;
                        if(chatMessages != null && chatMessages[0] != null) {
                            messagesString = Arrays.stream(chatMessages).filter(StringUtils::isNotBlank).map((s) -> s.replace("_", "\\_").replace("*", "\\*")).collect(Collectors.joining("\n"));
                        } else {
                            messagesString = "No messages found for this player";
                        }
                        messagesField.addProperty("value", messagesString);
                        messagesField.addProperty("inline", false);
                        fields.add(messagesField);

                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("NewBPChatReport");
                        out.writeUTF(reporter.getName());
                        out.writeUTF(reportedName);
                        out.writeUTF(prettyReason);
                        out.writeUTF(messagesString);
                        reporter.sendPluginMessage(BPReport.instance, "BungeeCord", out.toByteArray());
                    } else {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("NewBPReport");
                        out.writeUTF(reporter.getName());
                        out.writeUTF(reportedName);
                        out.writeUTF(prettyReason);
                        reporter.sendPluginMessage(BPReport.instance, "BungeeCord", out.toByteArray());
                    }

                    Utils.sendWebhookSync(webhook);

                    reporter.sendMessage("§a✔ Successfully reported " + reportedName + " for " + prettyReason + ".");
                } catch (Exception e) {
                    reporter.sendMessage("§c✕ Report failed.");
                    e.printStackTrace();
                }
            }
        };
        asyncSendReport.runTaskAsynchronously(instance);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        if(e.getWhoClicked() instanceof Player && e.getClickedInventory() != null) {
            String title = e.getClickedInventory().getTitle();
            if(title.startsWith("Reporting")) {
                String playerToReport = title.split(" ")[1];
                String reason = null;
                switch(e.getCurrentItem().getType()) {
                    case GOLD_SWORD:
                        reason = "cheating";
                        break;
                    case WEB:
                        reason = "chat";
                        break;
                    case SKULL_ITEM:
                        reason = "alt";
                        break;
                }
                assert reason != null;
                ((Player) e.getWhoClicked()).chat("/report " + playerToReport + " " + reason);
            }
        }
    }
    private final String[] blockedWords = {"nigga", "nigger", "anigger", "anigga", "aniga", "aniger", "niger", "niga", "fag", "faggot", "retard", "n1ger", "sex", "esex", "cum"};
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String msg = addEmojisToMessage(event.getMessage());
        event.setMessage(msg);
        Player player = event.getPlayer();

        String[] chatMessages = playerChatMessages.get(player.getUniqueId());
        if(chatMessages != null && msg.equals(chatMessages[0]) && msg.equals(chatMessages[1]) && msg.equals(chatMessages[2])) {
            player.sendMessage("§cBlocked excessive spam.");
            event.setCancelled(true);
            return;
        }
        event.setFormat("%1$s§r: %2$s");
        addMessageToPlayerMessages(player, msg);
        String filteredMessage = msg.toLowerCase().replaceAll("[\"'.:;,|`~!@#$%^&*()_\\-+={}\\[\\]/\\\\?<>]", "");
        for(String word : blockedWords) {
            if(filteredMessage.startsWith(word) || filteredMessage.contains(" " + word)) {
                event.setCancelled(true);
                player.sendMessage("§cBlocked message. You will be muted if you continue using these words.");
                return;
            }
        }
    }
    private String addEmojisToMessage(String msg) {
        msg = StringUtils.replace(msg, "<3", "§c❤§f");
        msg = StringUtils.replace(msg, ":eyes:", "⌊●_●⌋");
        msg = StringUtils.replace(msg, ":snowman:", "§b☃§f");
        msg = StringUtils.replace(msg, ":star:", "§e★§f");
        msg = StringUtils.replace(msg, ":smile:", "§e☻§f");
        msg = StringUtils.replace(msg, ":frown:", "§9☹§f");
        msg = StringUtils.replace(msg, ":x:", "§c✕§f");
        msg = StringUtils.replace(msg, ":cross:", "§c✕§f");
        msg = StringUtils.replace(msg, ":y:", "§a✔§f");
        msg = StringUtils.replace(msg, ":check:", "§a✔§f");
        msg = StringUtils.replace(msg, ":tm:", "§b™§f");
        msg = StringUtils.replace(msg, ":cry:", "§9（>﹏<）§f");
        msg = msg.replace(":why:", "§6(｢•-•)｢ why?§f");
        msg = msg.replace(":sunglasses:", "§68§e)§f");
        msg = msg.replace(":sunglasses2:", "§6B§e)§f");
        return msg;
    }
    public void addMessageToPlayerMessages(Player player, String msg) {
        playerChatMessages.computeIfAbsent(player.getUniqueId(), (uuid) -> new String[10]);
        String[] msgs = playerChatMessages.get(player.getUniqueId());
        System.arraycopy(msgs, 0, msgs, 1, msgs.length - 1);
        msgs[0] = msg;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if(channel.equals("bp:messages")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if(subchannel.equals("PlaySound")) {
                String sound = in.readUTF();
                Player p = getServer().getPlayer(UUID.fromString(in.readUTF()));
                p.playSound(p.getLocation(), sound, 1, 1);
            }
        }
    }
}
