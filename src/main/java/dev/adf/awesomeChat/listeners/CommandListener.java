package dev.adf.awesomeChat.listeners;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChatFilterManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    private final AwesomeChat plugin;

    public CommandListener(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String commandMessage = event.getMessage();

        ChatFilterManager filter = plugin.getChatFilterManager();
        if (filter == null || !filter.isFilterCommands()) {
            return;
        }

        ChatFilterManager.FilterResult result = filter.checkAndCensor(player, commandMessage, "command");
        if (result.blocked) {
            event.setCancelled(true);
        } else if (result.censored) {
            event.setMessage(result.censoredMessage);
        }
    }
}
