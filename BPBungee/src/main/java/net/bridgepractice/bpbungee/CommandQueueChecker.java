package net.bridgepractice.bpbungee;

import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.WeightNode;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CommandQueueChecker {
    public static void startChecking() {
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, CommandQueueChecker::checkQueue, 5, 5, TimeUnit.SECONDS);
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, CommandQueueChecker::checkTourneyQueue, 5, 5, TimeUnit.SECONDS);
    }
    private static void checkQueue() {
        try(PreparedStatement statement = BPBungee.connection.prepareStatement("SELECT * FROM commandQueue WHERE target='proxy';")) {
            ResultSet res = statement.executeQuery();
            while(res.next()) { // loop while results are available
                String type = res.getString("type");
                String content = res.getString("content");
                BPBungee.instance.getLogger().info("Found something in the command queue: Type="+type+" Content="+content);
                if(type.equals("excmd")) {
                    // content is a command
                    boolean successful = BPBungee.instance.getProxy().getPluginManager().dispatchCommand(BPBungee.instance.getProxy().getConsole(), content);
                    if(!successful) {
                        BPBungee.instance.getLogger().severe("Unable to execute command '"+content+"'");
                    }
                } else if(type.equals("srank")) {
                    // content is the uuid of the player who's rank changed
                    try(PreparedStatement getRank = BPBungee.connection.prepareStatement("SELECT * FROM rankedPlayers WHERE uuid = ?;")) {
                        getRank.setString(1, content);
                        ResultSet rankRes = getRank.executeQuery();
                        if(rankRes.next()) {
                            String permission = rankRes.getString("permission");
                            int approved = rankRes.getInt("approved");
                            String tag = rankRes.getString("tag");
                            boolean hasCustomTag = !rankRes.wasNull() && approved == 1;
                            String color = rankRes.getString("color");
                            int months = rankRes.getInt("months");

                            BPBungee.luckPerms.getUserManager().modifyUser(UUID.fromString(content), user -> {
                                // Add the permission
                                if(hasCustomTag) {
                                    // if custom, we need to give it an expiry
                                    user.data().add(Node.builder("group."+permission).expiry(months* 30L, TimeUnit.DAYS).build());
                                    user.data().add(PrefixNode.builder("§"+color+"["+tag+"] ", 25).expiry(months* 30L, TimeUnit.DAYS).build());
                                } else {
                                    // if regular dont make it expire
                                    user.data().add(Node.builder("group."+permission).build());
                                }
                            });
                        } else {
                            BPBungee.instance.getLogger().severe("Did not find a rank for uuid="+content);
                        }
                    }
                } else if(type.equals("settag")) {
                    // content is the uuid of the player who's rank changed
                    try(PreparedStatement getRank = BPBungee.connection.prepareStatement("SELECT * FROM rankedPlayers WHERE uuid = ?;")) {
                        getRank.setString(1, content);
                        ResultSet rankRes = getRank.executeQuery();
                        if(rankRes.next()) {
                            String tag = rankRes.getString("tag");
                            String color = rankRes.getString("color");
                            int months = rankRes.getInt("months");
                            Date dateBought = rankRes.getDate("boughtAt");
                            Date currentDate = new Date();
                            long timeSincePurchase = (currentDate.getTime() - dateBought.getTime()) / 1000 / 60 / 60 / 24;

                            // Delete old tags
                            UUID uuid = UUID.fromString(content);
                            if (!BPBungee.luckPerms.getUserManager().isLoaded(uuid)) {
                                BPBungee.luckPerms.getUserManager().loadUser(uuid);
                            }

                            User lpUser = BPBungee.luckPerms.getUserManager().getUser(uuid);

                            assert lpUser != null;
                            Collection<PrefixNode> nodes = lpUser.getNodes(NodeType.PREFIX);

                            for (PrefixNode node : nodes) {
                                if (node.hasExpiry()) {
                                    BPBungee.luckPerms.getUserManager().modifyUser(uuid, user -> {
                                        // remove node
                                        user.data().remove(node);
                                        if (user.data().contains(node, NodeEqualityPredicate.EXACT).asBoolean()) {
                                            // LuckPerms API bug - User still has prefix (try to remove again)
                                            user.data().remove(node);
                                        }
                                    });
                                }
                            }

                            BPBungee.luckPerms.getUserManager().modifyUser(UUID.fromString(content), user -> {
                                // change tag
                                user.data().add(PrefixNode.builder("§"+color+"["+tag+"] ", 25).expiry(months * 30L - timeSincePurchase, TimeUnit.DAYS).build());
                            });
                        } else {
                            BPBungee.instance.getLogger().severe("Did not find a rank for uuid="+content);
                        }
                    }
                }
            }
            // remove all of it
            try(PreparedStatement deleteStatement = BPBungee.connection.prepareStatement("DELETE FROM commandQueue WHERE target='proxy';")) {
                deleteStatement.executeUpdate();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static void checkTourneyQueue() {
        try(PreparedStatement statement = BPBungee.connection.prepareStatement("SELECT * FROM commandQueue WHERE target='tourneyproxy';")) {
            ResultSet res = statement.executeQuery();
            while(res.next()) { // loop while results are available
                String type = res.getString("type");
                String content = res.getString("content");
                BPBungee.instance.getLogger().info("Found something in the command queue: Type=" + type + " Content=" + content);
                switch (type) {
                    case "stour":
                        BPBungee.isTourneyRunning = true;
                        BPBungee.isTourneyGameRunning = false;
                        BPBungee.amountOfGamesEachCurrent = 1;
                        break;
                    case "atour":
                        for (ProxiedPlayer player : BPBungee.instance.getProxy().getPlayers()) {
                            if (Utils.isInTourney(player)) {
                                int skillLvl = Utils.getSkillLevel(player);
                                int gamesPlayed = Utils.getGamesPlayed(player);
                                if (skillLvl < 0 || gamesPlayed < 0) {
                                    player.sendMessage(new ComponentBuilder("Something went wrong registering you for the tournament. Please report this in #support on the discord!").color(ChatColor.RED).create());
                                } else {
                                    BPBungee.validTourneyPlayers.add(player.getUniqueId());
                                    BPBungee.playersInTourneySkillLevel.put(player.getUniqueId(), skillLvl);
                                    BPBungee.playersGamesPlayed.put(player.getUniqueId(), gamesPlayed);
                                    if (gamesPlayed < BPBungee.amountOfGamesEachCurrent) {
                                        BPBungee.tourneyPlayersNotPlayedYet.add(player.getUniqueId());
                                    }
                                }
                            }
                        }
                        Utils.showTourneyGameAnnouncement();
                        break;
                    case "ptour":
                        BPBungee.isTourneyRunning = false;
                        BPBungee.isTourneyGameRunning = false;
                        BPBungee.playersGamesPlayed.clear();
                        BPBungee.tourneyPlayersNotPlayedYet.clear();
                        BPBungee.validTourneyPlayers.clear();
                        BPBungee.playersInTourneySkillLevel.clear();
                        BPBungee.playersInGame.clear();
                        break;
                    case "rfngame":
                        BPBungee.playersInGame.clear();
                        BPBungee.isTourneyGameRunning = false;
                        Utils.checkTourneyGameAnnouncement();
                        break;
                    case "agame":
                        Utils.showTourneyGameAnnouncement();
                        break;
                    case "agametp":
                        Utils.showTourneyGameAnnouncementToPlayer(BPBungee.instance.getProxy().getPlayer(UUID.fromString(content)));
                        break;
                }
                // remove all of it
                try (PreparedStatement deleteStatement = BPBungee.connection.prepareStatement("DELETE FROM commandQueue WHERE target='tourneyproxy';")) {
                    deleteStatement.executeUpdate();
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
