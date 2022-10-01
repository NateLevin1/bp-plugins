package net.bridgepractice.BridgePracticeLobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {
    public static ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        List<String> itemLore = Arrays.asList(lore);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(itemLore);
        item.setItemMeta(itemMeta);
        return item;
    }

    public static ItemStack makeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(name);
        item.setItemMeta(itemMeta);
        return item;
    }

    public static ItemStack makeDyed(Material material, DyeColor color, String name, String... lore) {
        ItemStack item = makeItem(material, name, lore);
        item.setDurability(color.getData());
        return item;
    }
    public static ItemStack makePlayerHead(String ownerName, String name, String... lore) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        List<String> itemLore = Arrays.asList(lore);
        SkullMeta itemMeta = (SkullMeta) item.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(itemLore);
        itemMeta.setOwner(ownerName);
        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack makeCustomPlayerHead(String url, String name, String... lore) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        List<String> itemLore = Arrays.asList(lore);
        SkullMeta itemMeta = (SkullMeta) item.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(itemLore);

        // https://www.spigotmc.org/threads/create-a-skull-item-stack-with-a-custom-texture-base64.82416/#post-909452
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
        try {
            Field profileField = itemMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(itemMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack getEnchanted(ItemStack stack) {
        // see https://www.spigotmc.org/threads/adding-the-enchant-glow-to-block.50892/
        stack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.set("HideFlags", new NBTTagInt(1));
        nmsStack.setTag(tag);
        return CraftItemStack.asCraftMirror(nmsStack);
    }
    public static void sendTablist(Player player, String Title, String subTitle) {
        // see https://www.spigotmc.org/threads/tablist-header-in-1-8-8.296009/
        IChatBaseComponent tabTitle = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + Title + "\"}");
        IChatBaseComponent tabSubTitle = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + subTitle + "\"}");

        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(tabTitle);

        try {
            Field field = packet.getClass().getDeclaredField("b");
            field.setAccessible(true);
            field.set(packet, tabSubTitle);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void sendActionBar(Player player, String text, int times) {
        IChatBaseComponent comp = IChatBaseComponent.ChatSerializer
                .a("{\"text\":\"" + text + " \"}");
        PacketPlayOutChat packet = new PacketPlayOutChat(comp, (byte) 2);
        for (int i = 0; i < times; i++) {
            (new BukkitRunnable() {
                @Override
                public void run() {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
            }).runTaskLater(BridgePracticeLobby.instance, 20L * (i));
        }
    }
    public static ItemStack addLore(ItemStack item, String... lore) {
        ItemMeta itemMeta = item.getItemMeta();
        List<String> itemLore = itemMeta.getLore();
        itemLore.addAll(Arrays.asList(lore));
        itemMeta.setLore(itemLore);
        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack getUnbreakable(ItemStack stack) {
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound nmsCompound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
        nmsCompound.set("Unbreakable", new NBTTagByte((byte) 1));
        nmsStack.setTag(nmsCompound);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }
    public static JsonObject getJSON(String urlString) throws IOException {
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        JsonElement parsed = new JsonParser().parse(in);
        if (!parsed.isJsonObject()) {
            throw new IOException("Did not get a JSON object from URL " + urlString);
        }
        JsonObject result = parsed.getAsJsonObject();
        in.close();
        return result;
    }
    private static String getNameFromUuidSync(String uuid) throws IOException {
        JsonObject player = getJSON("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        return player.get("name").getAsString();
    }
    private static final ConcurrentHashMap<String, String> cachedUuidToName = new ConcurrentHashMap<>();
    private static final AtomicLong lastCacheEvict = new AtomicLong(System.currentTimeMillis());
    public static String getNameFromUuidSyncCached(String uuid) throws IOException {
        if (System.currentTimeMillis() - lastCacheEvict.get() > 60 * 60 * 1000) {
            lastCacheEvict.set(System.currentTimeMillis());
            cachedUuidToName.clear();
            BridgePracticeLobby.instance.getLogger().info("Evicting UUID to username cache");
        }
        String name = cachedUuidToName.get(uuid);
        if (name != null) {
            return name;
        } else {
            String requestedName = Utils.getNameFromUuidSync(uuid);
            cachedUuidToName.put(uuid, requestedName);
            return requestedName;
        }
    }
    public static String getUuidFromNameSync(String name) throws IOException {
        Player possiblyOnlinePlayer = BridgePracticeLobby.instance.getServer().getPlayer(name);
        if (possiblyOnlinePlayer != null) {
            return possiblyOnlinePlayer.getUniqueId().toString();
        }
        return getJSON("https://api.mojang.com/users/profiles/minecraft/" + name).get("id").getAsString().replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5");
    }
    public static org.bukkit.scoreboard.Scoreboard createScoreboard(String displayName, String[] scores) {
        Scoreboard board = BridgePracticeLobby.instance.getServer().getScoreboardManager().getNewScoreboard();
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
    public static int getPlayerXPSync(Player player) {
        try (PreparedStatement statement = BridgePracticeLobby.connection.prepareStatement("SELECT xp FROM players WHERE uuid=?;")) {
            statement.setString(1, player.getUniqueId().toString()); // uuid
            ResultSet res = statement.executeQuery();
            if (!res.next()) {
                throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
            }
            return res.getInt(1); // 1 indexing!
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your xp from our database. Please open a ticket on the discord!");
        }
        return -1;
    }

    public static int getPlayerCoinsSync(Player player) {
        try(PreparedStatement statement = BridgePracticeLobby.connection.prepareStatement("SELECT coins FROM players WHERE uuid=?;")) {
            statement.setString(1, player.getUniqueId().toString()); // uuid
            ResultSet res = statement.executeQuery();
            if(!res.next()) {
                throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
            }
            return res.getInt(1); // 1 indexing!
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your coins from our database. Please open a ticket on the discord!");
        }
        return -1;
    }
    public static void sendDuelRequest(Player requester, Player playerToDuel, String gameType) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("DuelPlayer");
        out.writeUTF(playerToDuel.getName());
        out.writeUTF(gameType);
        requester.sendPluginMessage(BridgePracticeLobby.instance, "BungeeCord", out.toByteArray());
    }
    private static final ItemStack sword = Utils.getUnbreakable(new ItemStack(Material.IRON_SWORD));
    private static final ItemStack bow = Utils.getUnbreakable(Utils.makeItem(Material.BOW, "§aBow", "§7Arrows regenerate every", "§a3.5s§7. You can have a maximum", "§7of §a1§7 arrow at a time.", ""));
    private static final ItemStack arrow = Utils.makeItem(Material.ARROW, "§aArrow", "§7Regenerates every §a3.5s§7!", "");
    private static final ItemStack glyph = Utils.makeItem(Material.DIAMOND, "§6Glyph Menu", "§7Least useful item", "§7in the game!", "");
    public static ItemStack getSword() {
        return sword.clone();
    }
    public static ItemStack getBow() {
        return bow.clone();
    }
    public static ItemStack getArrow() {
        return arrow.clone();
    }
    public static ItemStack getGlyph() {
        return glyph.clone();
    }
    public static ItemStack getPickaxe() {
        ItemStack pick = Utils.makeItem(Material.DIAMOND_PICKAXE, "§bDiamond Pickaxe");
        pick.addEnchantment(Enchantment.DIG_SPEED, 2);
        return Utils.getUnbreakable(pick);
    }
    public static ItemStack getBlocks() {
        ItemStack blocks = new ItemStack(Material.STAINED_CLAY);
        blocks.setAmount(64);
        return blocks;
    }
    public static ItemStack getGapple() {
        ItemStack gap = Utils.makeItem(Material.GOLDEN_APPLE, "§bGolden Apple", "§7Instantly heals you to full", "§7health and grants §aAbsorption", "§aI§7.");
        gap.setAmount(8);
        return gap;
    }
    public static void sendMessageSync(Player player, String message) {
        (new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(message);
            }
        }).runTask(BridgePracticeLobby.instance);
    }
   
}
