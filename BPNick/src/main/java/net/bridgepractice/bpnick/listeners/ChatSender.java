package net.bridgepractice.bpnick.listeners;

import com.mojang.brigadier.suggestion.Suggestion;
import de.exceptionflug.protocolize.api.event.PacketSendEvent;
import de.exceptionflug.protocolize.api.handler.PacketAdapter;
import de.exceptionflug.protocolize.api.protocol.Stream;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;

import java.util.List;

public class ChatSender extends PacketAdapter<TabCompleteResponse> {
    public ChatSender() {
        super(Stream.DOWNSTREAM, TabCompleteResponse.class);
    }
    @Override
    public void send(PacketSendEvent<TabCompleteResponse> event) {
        TabCompleteResponse packet = event.getPacket();
        List<Suggestion> suggestions = packet.getSuggestions().getList();
        for(Suggestion sug : suggestions) {
            System.out.println(sug.getText());
        }
    }
}
