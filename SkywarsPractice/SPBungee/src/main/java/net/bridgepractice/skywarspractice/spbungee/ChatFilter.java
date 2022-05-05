package net.bridgepractice.skywarspractice.spbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatFilter implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(ChatEvent e) {
        if (e.getMessage().contains("${")) {
            e.setCancelled(true);
        }
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        if (strictWordSearch(e.getMessage())) {
            e.setCancelled(true);
            player.sendMessage(new ComponentBuilder()
                    .append(new ComponentBuilder("---------------------------------------").color(ChatColor.DARK_RED).strikethrough(true).create())
                    .append(new ComponentBuilder("\nBlocked message containing disallowed word.\nYou will be muted if you continue using blocked words or attempt to bypass this filter.").color(ChatColor.RED).strikethrough(false).create())
                    .append(new ComponentBuilder("---------------------------------------").color(ChatColor.DARK_RED).strikethrough(true).create())
                    .create());
            Utils.log("§e"+player.getName()+" §3attempted to say §c§lblocked words§3: §f"+e.getMessage());
            if (SPBungee.chatStrikes.containsKey(player.getUniqueId())) {
                SPBungee.chatStrikes.put(player.getUniqueId(), SPBungee.chatStrikes.get(player.getUniqueId()) + 1);
                List<String> texts = SPBungee.chatStrikesText.get(player.getUniqueId());
                texts.add(e.getMessage());
                SPBungee.chatStrikesText.put(player.getUniqueId(), texts);
            } else {
                SPBungee.chatStrikes.put(player.getUniqueId(), 1);
                List<String> texts = new ArrayList<>();
                texts.add(e.getMessage());
                SPBungee.chatStrikesText.put(player.getUniqueId(), texts);
            }
            if (SPBungee.chatStrikes.get(player.getUniqueId()) == 3) {
                // Mute player for 3 days
                SPBungee.chatStrikes.remove(player.getUniqueId());
                SPBungee.instance.getProxy().getPluginManager().dispatchCommand(SPBungee.instance.getProxy().getConsole(), "mute "+player.getName()+" 3");
                List<String> texts = SPBungee.chatStrikesText.get(player.getUniqueId());
                Utils.broadcastToPermission("bridgepractice.moderation.chat", new TextComponent("§5§l"+player.getName()+" §5automatically §cmuted§5 by §dCONSOLE§5:\n" +
                        "§6Strike 1§7: §2" + texts.get(0) + "\n" +
                        "§6Strike 2§7: §2" + texts.get(1) + "\n" +
                        "§6Strike 3§7: §2" + texts.get(2)));
                SPBungee.chatStrikesText.remove(player.getUniqueId());
            }
        }
    }

    public static boolean strictWordSearch(String message) {
        List<String> blockedWords = SPBungee.cfg.getStringList("blocked-words");
        for (String blockedWord : blockedWords) {
            if (message.contains(" "+blockedWord+" ") || message.endsWith(" "+blockedWord) || message.startsWith(blockedWord+" ") || message.equalsIgnoreCase(blockedWord)) {
                return true;
            }
            message = message.toLowerCase(Locale.ROOT);
            message = message
                    .replaceAll("1", "i")
                    .replaceAll("4", "a")
                    .replaceAll("@", "a")
                    .replaceAll("\\.", "")
                    .replaceAll("\\+", "")
                    .replaceAll("=", "")
                    .replaceAll("_", "")
                    .replaceAll("-", "");
            char[] wordLetters = blockedWord.toCharArray();
            char[] messageLetters = message.toCharArray();

            for (Integer position : stringMatch(message, String.valueOf(wordLetters[0]))) {
                int currentPlaceInWord = 0;
                int endPosition;
                for (int i = position; i < message.length(); i++) {
                    if (wordLetters.length - 1 == currentPlaceInWord) {
                        endPosition = i;
                        String badWord = message.substring(position, endPosition + 1)
                                .replaceAll(" ", "");
                        String fullWord = getWordAtPosition(message, endPosition)
                                .replaceAll(" ", "");
                        if (badWord.equalsIgnoreCase(blockedWord) && (fullWord.equalsIgnoreCase(blockedWord) || blockedWord.contains(fullWord))) {
                            return true;
                        }
                    }
                    if (messageLetters[i] == wordLetters[currentPlaceInWord] || messageLetters[i] == ' ' || messageLetters[i] == '.' || messageLetters[i] == '+' || messageLetters[i] == '_' || messageLetters[i] == '=') {
                        try {
                            if (messageLetters[i] == wordLetters[currentPlaceInWord]) {
                                currentPlaceInWord++;
                            }
                        } catch (ArrayIndexOutOfBoundsException ignored) {

                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return false;
    }

    public static List<Integer> stringMatch(String text, String pattern) {
        List<Integer> instances = new ArrayList<>();

        int len_t = text.length();
        int len_p = pattern.length();

        int k = 0, i = 0, j = 0;

        for (i = 0; i <= (len_t - len_p); i++) {

            for (j = 0; j < len_p; j++) {
                if (text.charAt(i + j) != pattern.charAt(j))
                    break;
            }

            if (j == len_p) {
                k++;
                instances.add(i);
            }
        }
        return instances;
    }

    private static String getWordAtPosition(String message, int position) {
        int startPos = -1, endPos = -1;
        for (int i = position; i > -1; i--) {
            if (message.toCharArray()[i] == ' ') {
                startPos = i + 1;
            }
        }
        for (int i = position; i < message.length(); i++) {
            if (message.toCharArray()[i] == ' ') {
                endPos = i - 1;
            }
        }
        if (startPos == -1) {
            startPos = 0;
        }
        if (endPos == -1) {
            endPos = message.length() - 1;
        }

        List<Character> lettersInWord = new ArrayList<>();

        for (int i = startPos; i < endPos + 1; i++) {
            lettersInWord.add(message.toCharArray()[i]);
        }
        return StringUtils.join(lettersInWord, "")
                .replaceAll(" ", "");
    }
}
