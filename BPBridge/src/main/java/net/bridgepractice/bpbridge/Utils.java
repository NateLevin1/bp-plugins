package net.bridgepractice.bpbridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.luckperms.api.model.user.User;
import net.minecraft.server.v1_8_R3.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Utils {
    public static void sendTitle(Player player, String titleText, String subtitleText, int fadeIn, int fadeOut, int duration) {
        IChatBaseComponent chatTitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + titleText + "\"}");
        IChatBaseComponent chatSubtitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitleText + "\"}");

        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, chatTitle);
        PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, chatSubtitle);
        PacketPlayOutTitle length = new PacketPlayOutTitle(fadeIn, duration, fadeOut);

        // For some reason if we don't send this length packet then the first time we try to send a title to a player
        // after they have disconnected the title does not show up.
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);

        // send the actual title
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(title);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(subtitle);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(length);
    }
    public static void sendTitle(Player player, String titleText, String subtitleText) {
        sendTitle(player, titleText, subtitleText, 5, 5, 40);
    }
    public static Location getMapSpawnLoc(World world, int x, double y, boolean isRed) {
        return new Location(world, x+0.5, y, 0.5, isRed ? 90 : -90, 0);
    }
    public static Scoreboard createScoreboard(String displayName, String[] scores) {
        Scoreboard board = BPBridge.instance.getServer().getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("scoreboard", "dummy");
        objective.setDisplayName(displayName);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int numSpaces = 0;
        int numResets = 1;
        for(int i = 0; i < scores.length; i++) {
            if(scores[i].equals("")) {
                objective.getScore(String.join("", Collections.nCopies(numSpaces, " "))).setScore(scores.length - i);
                numSpaces++;
            } else if(scores[i].startsWith("%")) {
                int percent = scores[i].substring(1).indexOf('%') + 1;
                String teamName = scores[i].substring(1, percent);
                Team team = board.registerNewTeam(teamName);
                String entry = String.join("", Collections.nCopies(numResets, "§r"));
                team.addEntry(entry);
                String content = scores[i].substring(percent + 1);
                int split = content.indexOf("%");
                if(split == -1) {
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

    public static ItemStack getUnbreakable(ItemStack stack) {
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound nmsCompound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
        nmsCompound.set("Unbreakable", new NBTTagByte((byte) 1));
        nmsStack.setTag(nmsCompound);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static BlockState[][][] getBlocks(Location start, int width, int height, int length) {
        BlockState[][][] res = new org.bukkit.block.BlockState[height][length][width];
        for(int y = 0; y < height; y++) {
            for(int z = 0; z < length; z++) {
                for(int x = 0; x < width; x++) {
                    res[y][z][x] = start.clone().add(x, y, z).getBlock().getState();
                }
            }
        }
        return res;
    }

    public static void sendActionBar(Player player, String text, int times) {
        IChatBaseComponent comp = IChatBaseComponent.ChatSerializer
                .a("{\"text\":\"" + text + " \"}");
        PacketPlayOutChat packet = new PacketPlayOutChat(comp, (byte) 2);
        for(int i = 0; i < times; i++) {
            (new BukkitRunnable() {
                @Override
                public void run() {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
            }).runTaskLater(BPBridge.instance, 20L * (i));
        }
    }
    public static String getRankedName(Player player) {
        User luckPermsUser = BPBridge.luckPerms.getPlayerAdapter(Player.class).getUser(player);
        String prefix = luckPermsUser.getCachedData().getMetaData().getPrefix();
        return prefix + player.getName();
    }
    public static String hearts(int num) {
        return (new String(new char[num]).replace("\0", "❤"));
    }
    public static String ordinal(int i) {
        int mod100 = i % 100;
        int mod10 = i % 10;
        if(mod10 == 1 && mod100 != 11) {
            return i + "st";
        } else if(mod10 == 2 && mod100 != 12) {
            return i + "nd";
        } else if(mod10 == 3 && mod100 != 13) {
            return i + "rd";
        } else {
            return i + "th";
        }
    }
    public static void sendActionBar(Player player, String text) {
        sendActionBar(player, text, 2);
    }

    public static CageBlocks[] cages = new CageBlocks[6];
    public static void loadCages() {
        World world = BPBridge.instance.getServer().getWorld("world2");
        cages[Cage.Flower.ordinal()] = new CageBlocks(getBlocks(new Location(world, 24, 97, -4), 7, 9, 9), getBlocks(new Location(world, 36, 97, -4), 7, 9, 9));
        cages[Cage.Mushroom.ordinal()] = new CageBlocks(getBlocks(new Location(world, 25, 97, -20), 7, 9, 9), getBlocks(new Location(world, 36, 97, -20), 7, 9, 9));
        cages[Cage.Sailboat.ordinal()] = new CageBlocks(getBlocks(new Location(world, 36, 97, -33), 7, 9, 9), getBlocks(new Location(world, 25, 97, -33), 7, 9, 9));
        cages[Cage.Default.ordinal()] = new CageBlocks(getBlocks(new Location(world, 25, 98, -46), 7, 9, 9), getBlocks(new Location(world, 36, 98, -46), 7, 9, 9));
        cages[Cage.Bed.ordinal()] = new CageBlocks(getBlocks(new Location(world, 25, 98, -68), 7, 9, 9), getBlocks(new Location(world, 36, 98, -68), 7, 9, 9));
        cages[Cage.Temple.ordinal()] = new CageBlocks(getBlocks(new Location(world, 36, 99, -87), 7, 9, 9), getBlocks(new Location(world, 26, 99, -87), 7, 9, 9));
    }
    public static Cage getCageSync(Player player) {
        try(PreparedStatement statement = BPBridge.connection.prepareStatement("SELECT cage FROM players WHERE uuid=?;")) {
            statement.setString(1, player.getUniqueId().toString()); // uuid
            ResultSet res = statement.executeQuery();
            if(!res.next()) {
                throw new SQLException("Did not get a row from the database. Player name: " + player.getName() + " Player UUID: " + player.getUniqueId());
            }
            return Cage.values()[res.getInt("cage")];
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            player.sendMessage("§c§lUh oh!§r§c Something went wrong fetching your information from our database. Please open a ticket on the discord!");
            return Cage.Default;
        }
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
    public static ItemStack getBlocks(boolean isRedTeam, int amount) {
        ItemStack blocks = new ItemStack(Material.STAINED_CLAY);
        blocks.setAmount(amount);
        if(isRedTeam) {
            blocks.setDurability(DyeColor.RED.getData());
        } else {
            blocks.setDurability(DyeColor.BLUE.getData());
        }
        return blocks;
    }
    public static ItemStack getGapple(int amount) {
        ItemStack gap = Utils.makeItem(Material.GOLDEN_APPLE, "§bGolden Apple", "§7Instantly heals you to full", "§7health and grants §aAbsorption", "§aI§7.");
        gap.setAmount(amount);
        return gap;
    }
    public static ItemStack getBoots(boolean isRedTeam) {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if(isRedTeam) {
            meta.setColor(Color.RED);
        } else {
            meta.setColor(Color.BLUE);
        }
        item.setItemMeta(meta);
        return Utils.getUnbreakable(item);
    }
    public static ItemStack getLeggings(boolean isRedTeam) {
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if(isRedTeam) {
            meta.setColor(Color.RED);
        } else {
            meta.setColor(Color.BLUE);
        }
        item.setItemMeta(meta);
        return Utils.getUnbreakable(item);
    }
    public static ItemStack getChestplate(boolean isRedTeam) {
        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if(isRedTeam) {
            meta.setColor(Color.RED);
        } else {
            meta.setColor(Color.BLUE);
        }
        item.setItemMeta(meta);
        return Utils.getUnbreakable(item);
    }
    public static void sendWebhookSync(JsonObject object) {
        // https://stackoverflow.com/a/35013372/13608595
        try {
            URL url = new URL(BPBridge.discordWebhook);
            URLConnection con = url.openConnection();
            HttpsURLConnection req = (HttpsURLConnection) con;
            req.setRequestMethod("POST");
            req.setDoOutput(true);
            byte[] out = object.toString().getBytes(StandardCharsets.UTF_8);
            req.setFixedLengthStreamingMode(out.length);
            req.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            req.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15");
            req.connect();
            OutputStream os = req.getOutputStream();
            os.write(out);
            os.flush();
            int responseCode = req.getResponseCode();
            if(responseCode < 200 || responseCode >= 300) {
                BPBridge.instance.getLogger().severe(responseCode + " " + req.getResponseMessage());
            }
            req.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String qualifyGameType(String lowercaseGameType) {
        if(lowercaseGameType.equals("unranked")) {
            return "Unranked";
        } else if(lowercaseGameType.equals("pvp")) {
            return "    PvP";
        }
        return "Unknown Mode";
    }
    public static void sendDebugErrorWebhook(String startMsg, Exception e) {
        sendDebugErrorWebhook(startMsg+"\n"+ExceptionUtils.getStackTrace(e));
    }

    public static void sendDebugErrorWebhook(String s) {
        (new BukkitRunnable() {
            @Override
            public void run() {
                // send a winstreak log message in the discord - this is so winstreak audits are possible
                JsonObject webhook = new JsonObject();
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();

                webhook.addProperty("content", "<@729682275897442344>");

                webhook.add("embeds", embeds);
                embeds.add(embed);

                embed.addProperty("color", 0xff0000);

                embed.addProperty("description", "```java\n" + s + "\n```");
                embed.addProperty("title", "<a:no_animated:908859577146159204> DEBUG ERROR!");

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "Something went very wrong!");
                footer.addProperty("icon_url", "https://cdn.discordapp.com/emojis/908859577146159204.gif?v=1");
                embed.add("footer", footer);
                embed.addProperty("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

                Utils.sendWebhookSync(webhook);
            }
        }).runTaskAsynchronously(BPBridge.instance);
    }

    public static String getGameDebugInfo(String worldName) {
        Game game = BPBridge.instance.gamesByWorld.get(worldName);
        if(game == null) {
            return "\nNo game with the world name "+worldName+" exists!";
        }
        World world = game.world;
        String curPlayers;
        if(world != null) {
            curPlayers = world.getPlayers().toString();
        } else {
            curPlayers = "world is null";
        }
        return "\nGame Info for world "+worldName+":\n**State**: "+game.state+"\n**All Players**: "+game.allPlayers+"\n**Cur Players**: "+curPlayers;
    }
}
