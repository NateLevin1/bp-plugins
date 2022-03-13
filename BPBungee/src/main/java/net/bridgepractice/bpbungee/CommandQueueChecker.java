package net.bridgepractice.bpbungee;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CommandQueueChecker {
    public static void startChecking() {
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, CommandQueueChecker::checkQueue, 5, 5, TimeUnit.SECONDS);
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
                            String boughtAt = rankRes.getString("boughtAt").substring(0,9);
                            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
                            Date dateBought = parser.parse(boughtAt);
                            Date currentDate = new Date();
                            int timeSincePurchase = dateBought.getDay() - currentDate.getDay();

                            BPBungee.luckPerms.getUserManager().modifyUser(UUID.fromString(content), user -> {
                                // change tag
                                user.data().add(PrefixNode.builder("§"+color+"["+tag+"] ", 25).expiry(months * 30L -timeSincePurchase , TimeUnit.DAYS).build());
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
        } catch (SQLException | ParseException exception) {
            exception.printStackTrace();
        }
    }
}
