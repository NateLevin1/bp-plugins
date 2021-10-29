package net.bridgepractice.bpnick;

import de.exceptionflug.protocolize.api.protocol.ProtocolAPI;
import net.bridgepractice.bpnick.commands.Nick;
import net.bridgepractice.bpnick.commands.Unnick;
import net.bridgepractice.bpnick.listeners.ChatSender;
import net.bridgepractice.bpnick.listeners.TabComplete;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class BPNick extends Plugin {
    @Override
    public void onEnable() {
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Nick());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new Unnick());

//        ProtocolAPI.getEventManager().registerListener(new ChatSender());
        getProxy().getPluginManager().registerListener(this, new TabComplete());
    }
}
