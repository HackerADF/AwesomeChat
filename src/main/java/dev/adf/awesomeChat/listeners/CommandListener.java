package dev.adf.awesomeChat.listeners;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChatFilterManager;
import org.bukkit.configuration.file.FileConfiguration;
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

        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("chat-filter.filter-commands", false)) {
            return;
        }

        ChatFilterManager filter = plugin.getChatFilterManager();
        if (filter != null) {
            boolean blocked = filter.checkAndHandle(player, commandMessage, "command");
            if (blocked) {
                event.setCancelled(true);
            }
        }
    }
}
