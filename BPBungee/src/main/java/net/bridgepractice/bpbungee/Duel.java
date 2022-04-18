package net.bridgepractice.bpbungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Duel extends Command {
    public static Duel instance;
    public Duel() {
        super("Duel", null, "d");
        instance = this;
    }

    private static final BaseComponent[] beforeDivider = new ComponentBuilder("----------------------------------------------------").color(ChatColor.GOLD).create();
    private static final BaseComponent[] afterDivider = new ComponentBuilder("\n----------------------------------------------------").color(ChatColor.GOLD).create();

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer)) return;
        if(args.length == 0) {
            sender.sendMessage(new ComponentBuilder("You must provide a player and game for this command!").color(ChatColor.RED).create());
        } else if(args.length == 1) {
            sender.sendMessage(new ComponentBuilder("You must provide a game for this command!").bold(true).color(ChatColor.RED)
                    .append(new ComponentBuilder("\nAvailable Games:").color(ChatColor.AQUA).bold(false).create())
                    .append(new ComponentBuilder("\n - Bridge").color(ChatColor.YELLOW).bold(false).create())
                    .append(new ComponentBuilder("\n - PvP_Duel").color(ChatColor.YELLOW).bold(false).create())
                    .append(new ComponentBuilder("\n - NoBridge").color(ChatColor.YELLOW).bold(false).create())
                    .create());
        } else {
            if(args[0].equals("accept")) {
                ArrayList<DuelRequest> requests = duelRequests.get(sender.getName());
                if(requests != null) {
                    for(DuelRequest request : requests) {
                        if(request.requester.getName().equals(args[1])) {
                            if(!request.requester.isConnected()) {
                                sender.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nThe player that sent this Duel request is no longer online!").color(ChatColor.RED).create()).append(afterDivider).create());
                                return;
                            }
                            // accept
                            removeFromRequestsIfCan(request);

                            BaseComponent[] cycleComponent = new ComponentBuilder().append(new ComponentBuilder("\n\n[CLICK HERE]").color(ChatColor.RED).append(new ComponentBuilder(" to CYCLE MAP").color(ChatColor.YELLOW).append(new ComponentBuilder(" (" + request.cycles + " remaining)").color(ChatColor.GREEN).create()).create()).create()).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§eClick to switch to a different map.\n §7" + request.cycles + " cycles remaining."))).create();
                            for(BaseComponent component : cycleComponent) {
                                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel cycle " + request.playerToDuel.getName()+" "+request.gameMode));
                            }
                            request.requester.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\n" + request.playerToDuel.getDisplayName() + "§a accepted the Duel request!").color(ChatColor.GREEN).append(cycleComponent).create()).event((ClickEvent) null).event((HoverEvent) null).append(afterDivider).create());
                            for(BaseComponent component : cycleComponent) {
                                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel cycle " + request.requester.getName()+" "+request.gameMode));
                            }
                            request.playerToDuel.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nYou accepted " + request.playerToDuel.getDisplayName() + "§a's Duel request!").color(ChatColor.GREEN).append(cycleComponent).create()).event((ClickEvent) null).event((HoverEvent) null).append(afterDivider).create());
                            request.accepted = true;
                            startPrivateGame(request);
                            return;
                        }
                    }
                }
                sender.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nYou haven't been invited to a Duel, or the invitation has expired!").color(ChatColor.RED).create()).append(afterDivider).create());
            } else if(args[0].equals("cycle")) {//TODO: Disallow cycling if map is set
                if(args.length <= 2) {
                    sender.sendMessage(new ComponentBuilder("You must provide a game type for this command!").bold(true).color(ChatColor.RED)
                            .append(new ComponentBuilder("\nAvailable Games:").color(ChatColor.AQUA).bold(false).create())
                            .append(new ComponentBuilder("\n - Bridge").color(ChatColor.YELLOW).bold(false).create())
                            .append(new ComponentBuilder("\n - PvP_Duel").color(ChatColor.YELLOW).bold(false).create())
                            .append(new ComponentBuilder("\n - NoBridge").color(ChatColor.YELLOW).bold(false).create())
                            .create());
                    return;
                }
                DuelRequest request = null;
                ArrayList<DuelRequest> senderRequests = duelRequests.get(sender.getName());
                if(senderRequests != null) {
                    for(DuelRequest senderRequest : senderRequests) {
                        if(senderRequest.requester.getName().equals(args[1]) && senderRequest.gameMode.equals(args[2])) {
                            request = senderRequest;
                        }
                    }
                }
                if(request == null) {
                    ArrayList<DuelRequest> argsRequests = duelRequests.get(args[1]);
                    if(argsRequests != null) {
                        for(DuelRequest argsRequest : argsRequests) {
                            if(argsRequest.requester.getName().equals(sender.getName()) && argsRequest.gameMode.equals(args[2])) {
                                request = argsRequest;
                            }
                        }
                    }
                }

                if(request == null) {
                    sender.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nYou haven't been invited to a Duel, or the invitation has expired!").color(ChatColor.RED).create()).append(afterDivider).create());
                    return;
                }
                if(!request.requester.isConnected()) {
                    sender.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nThe player that sent this Duel request is no longer online!").color(ChatColor.RED).create()).append(afterDivider).create());
                    return;
                }

                if(request.cycle()) {
                    removeFromRequestsIfCan(request);
                    BaseComponent[] cycleComponent = new ComponentBuilder().append(new ComponentBuilder("\n\n[CLICK HERE]").color(ChatColor.RED).append(new ComponentBuilder(" to CYCLE MAP").color(ChatColor.YELLOW).append(new ComponentBuilder(" (" + request.cycles + " remaining)").color(ChatColor.GREEN).create()).create()).create()).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§eClick to switch to a different map.\n §7" + request.cycles + " cycles remaining."))).create();
                    for(BaseComponent component : cycleComponent) {
                        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel cycle " + request.playerToDuel.getName()+" "+request.gameMode));
                    }
                    request.requester.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\n" + ((ProxiedPlayer) sender).getDisplayName() + "§a cycled!").color(ChatColor.GREEN).append(cycleComponent).create()).event((ClickEvent) null).event((HoverEvent) null).append(afterDivider).create());
                    for(BaseComponent component : cycleComponent) {
                        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel cycle " + request.requester.getName()+" "+request.gameMode));
                    }
                    request.playerToDuel.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\n" + ((ProxiedPlayer) sender).getDisplayName() + "§a cycled!").color(ChatColor.GREEN).append(cycleComponent).create()).event((ClickEvent) null).event((HoverEvent) null).append(afterDivider).create());
                    startPrivateGame(request);
                } else {
                    removeFromRequests(request);
                    sender.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nYou have already used all of your cycles!").color(ChatColor.RED).create()).append(afterDivider).create());
                }
            } else {
                String playerName = args[0];
                String gameMode = args[1];
                String mapName = args.length > 2 ? args[2] : null;
                if (mapName != null && !sender.hasPermission("group.admin")){
                    sender.sendMessage(new ComponentBuilder("Error: You do not have permission to set the map.").color(ChatColor.RED).create());
                    return;
                }
                sendDuelRequest(sender, playerName, gameMode, mapName);
            }
        }
    }

    public static void sendDuelRequest(CommandSender sender, String playerName, String gameType, String mapName) {
        ProxiedPlayer playerToDuel = BPBungee.instance.getProxy().getPlayer(playerName);
        if(playerToDuel == null) {
            sender.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nThere is no player named '" + playerName + "' online!").color(ChatColor.RED).append(afterDivider).create()).create());
            return;
        }
        if(playerToDuel.getName().equals(sender.getName())) {
            sender.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nYou cannot duel yourself!").color(ChatColor.RED).create()).append(afterDivider).create());
            return;
        }
        switch(gameType.toLowerCase()) {
            case "duels_bridge_duel": // -> This is from Hypixel
            case "unranked":
            case "bridge":
            case "bridge_duel":
            case "duel": {
                requestDuelOfPlayer(((ProxiedPlayer) sender), playerToDuel, "unranked", "Bridge Duel", mapName);
                break;
            }
            case "pvpduel":
            case "pvp_duel":
            case "pvp": {
                requestDuelOfPlayer(((ProxiedPlayer) sender), playerToDuel, "pvp", "PvP Duel", mapName);
                break;
            }
            case "bridgeless":
            case "nobridge": {
                requestDuelOfPlayer(((ProxiedPlayer) sender), playerToDuel, "nobridge", "NoBridge Duel", mapName);
                break;
            }
            default:
                sender.sendMessage(new ComponentBuilder("Unknown game \"" + gameType + "\"").color(ChatColor.RED).bold(true)
                        .append(new ComponentBuilder("\nAvailable Games:").color(ChatColor.AQUA).bold(false).create())
                        .append(new ComponentBuilder("\n - Bridge").color(ChatColor.YELLOW).bold(false).create())
                        .append(new ComponentBuilder("\n - PvP_Duel").color(ChatColor.YELLOW).bold(false).create())
                        .append(new ComponentBuilder("\n - NoBridge").color(ChatColor.YELLOW).bold(false).create())
                        .create());
                break;
        }
    }

    private static final HashMap<String, ArrayList<DuelRequest>> duelRequests = new HashMap<>(); // the key is the player the duel was sent to

    private static class DuelRequest {
        ProxiedPlayer requester;
        ProxiedPlayer playerToDuel;
        String gameMode;
        private int cycles = 5;
        boolean accepted = false;
        String map;
        public DuelRequest(ProxiedPlayer requester, ProxiedPlayer playerToDuel, String gameMode) {
            this.requester = requester;
            this.playerToDuel = playerToDuel;
            this.gameMode = gameMode;
        }
        public boolean cycle() {
            cycles--;
            return cycles >= 0;
        }
    }

    private static boolean removeFromRequests(DuelRequest request) {
        ArrayList<DuelRequest> requestsToPlayer = duelRequests.get(request.playerToDuel.getName());
        if(requestsToPlayer == null) return false;
        requestsToPlayer.removeIf(r -> r.requester == request.requester && r.playerToDuel == request.playerToDuel && r.gameMode.equals(request.gameMode));
        if(requestsToPlayer.size() == 0) {
            // if there are no more requests, allow for gc
            duelRequests.remove(request.playerToDuel.getName());
        }
        return true;
    }

    private void removeFromRequestsIfCan(DuelRequest request) {
        int originalCycles = request.cycles;
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
            if(originalCycles == request.cycles) {
                removeFromRequests(request);
            }
        }, 15, TimeUnit.SECONDS);
    }

    public static void requestDuelOfPlayer(ProxiedPlayer requester, ProxiedPlayer playerToDuel, String gameMode, String prettyGameName, String mapName) {
        if(duelRequests.get(playerToDuel.getName()) != null) {
            for(DuelRequest duelRequest : duelRequests.get(playerToDuel.getName())) {
                if(duelRequest.requester == requester) {
                    requester.sendMessage(new ComponentBuilder()
                            .append(beforeDivider)
                            .append(new ComponentBuilder("\nYou have already sent a duel request to that person!").color(ChatColor.RED).create())
                            .append(afterDivider)
                            .create());
                    return;
                }
            }
        }
        if(duelRequests.get(requester.getName()) != null) {
            for(DuelRequest duelRequest : duelRequests.get(requester.getName())) {
                if(duelRequest.playerToDuel == requester && duelRequest.gameMode.equals(gameMode)) {
                    if(duelRequest.accepted) {
                        instance.execute(requester, new String[]{"cycle", playerToDuel.getName()});
                    } else {
                        instance.execute(requester, new String[]{"accept", playerToDuel.getName()});
                    }
                    return;
                }
            }
        }
        BaseComponent[] onMapText;
        if (mapName != null) {
            onMapText = new ComponentBuilder(" on " + mapName.substring(0, 1).toUpperCase() + mapName.substring(1)).color(ChatColor.GREEN).create();
        } else {
            onMapText = new ComponentBuilder("").create();
        }

        // we can assume requester and playerToDuel are both online
        requester.sendMessage(new ComponentBuilder()
                .append(beforeDivider)
                .append(new ComponentBuilder("\nYou invited ").color(ChatColor.YELLOW).create())
                .append(new ComponentBuilder(playerToDuel.getDisplayName()).color(ChatColor.GREEN).create())
                .append(new ComponentBuilder(" to a " + prettyGameName).color(ChatColor.YELLOW).create())
                .append(onMapText)
                .append(new ComponentBuilder("!").color(ChatColor.YELLOW).create())
                .append(new ComponentBuilder(" They have 60 seconds to accept.").color(ChatColor.YELLOW).create())
                .append(afterDivider)
                .create());

        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to accept " + requester.getDisplayName() + "§a's duel\n§7(/duel accept " + requester.getName() + ")"));
        ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + requester.getName());

        playerToDuel.sendMessage(new ComponentBuilder()
                .append(beforeDivider)
                .append(new ComponentBuilder("\n        ").create())
                .append(new ComponentBuilder(requester.getDisplayName()).color(ChatColor.GREEN).create())
                .append(new ComponentBuilder(" has invited you to a " + prettyGameName).color(ChatColor.AQUA).create())
                .append(onMapText)
                .append(new ComponentBuilder("!").color(ChatColor.AQUA).create())
                .append(new ComponentBuilder("\n    CLICK HERE").event(hover).event(click).color(ChatColor.GOLD).bold(true).append(new ComponentBuilder(" to accept! You have 60 seconds to accept.").color(ChatColor.YELLOW).bold(false).event(hover).event(click).create()).create())
                .append(afterDivider.clone()).event((ClickEvent) null).event((HoverEvent) null)
                .create());

        DuelRequest request = new DuelRequest(requester, playerToDuel, gameMode);
        if(mapName != null) {
            request.map = mapName;
        }
        duelRequests.computeIfAbsent(playerToDuel.getName(), (ignored) -> new ArrayList<>());
        duelRequests.get(playerToDuel.getName()).add(request);
        BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> {
            if(removeFromRequests(request)) { // if there was a request to that player
                requester.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nThe Duel request to ").color(ChatColor.YELLOW).append(playerToDuel.getDisplayName()).color(ChatColor.GREEN).append(" has expired.").color(ChatColor.YELLOW).create()).append(afterDivider).create());
                playerToDuel.sendMessage(new ComponentBuilder().append(beforeDivider).append(new ComponentBuilder("\nThe Duel request from ").color(ChatColor.YELLOW).append(requester.getDisplayName()).color(ChatColor.GREEN).append(" has expired.").color(ChatColor.YELLOW).create()).append(afterDivider).create());
            }
        }, 1, TimeUnit.MINUTES);
    }

    public void startPrivateGame(DuelRequest request) {
        ServerInfo multiplayerServer = ProxyServer.getInstance().getServerInfo("multiplayer_1");
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("StartPrivateGame");
        out.writeUTF(request.gameMode);
        out.writeUTF(request.map == null ? "" : request.map);
        out.writeInt(2); // num of players, only ever 2 for now
        out.writeUTF(request.playerToDuel.getName());
        out.writeUTF(request.requester.getName());

        if(multiplayerServer.getPlayers().size() == 0) {
            // if there are no players online we need to connect them so we can send a message
            request.playerToDuel.connect(ProxyServer.getInstance().getServerInfo("multiplayer_1"));
            BPBungee.instance.getProxy().getScheduler().schedule(BPBungee.instance, () -> request.playerToDuel.getServer().getInfo().sendData("bp:messages", out.toByteArray()), 1000, TimeUnit.MILLISECONDS);
        } else {
            multiplayerServer.sendData("bp:messages", out.toByteArray());
        }
    }
}
