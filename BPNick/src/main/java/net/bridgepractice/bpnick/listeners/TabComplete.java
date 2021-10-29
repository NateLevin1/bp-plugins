package net.bridgepractice.bpnick.listeners;

import net.bridgepractice.bpnick.NickManager;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class TabComplete implements Listener {
    @EventHandler
    public void onTab(TabCompleteResponseEvent event) {
        System.out.println("tab");
        System.out.println("event.getSuggestions() = " + event.getSuggestions());

        for(int i = 0; i < event.getSuggestions().size(); i++) {
            String suggestion = event.getSuggestions().get(i);
            System.out.println(suggestion);
            if(NickManager.isPlayerNicked(suggestion)) {
                event.getSuggestions().set(i, NickManager.getPlayerNick(suggestion).getNickName());
            }
        }
    }
}
