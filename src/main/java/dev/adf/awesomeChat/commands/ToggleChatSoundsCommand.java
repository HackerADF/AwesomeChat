package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleChatSoundsCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ToggleChatSoundsCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        SoundManager soundManager = plugin.getSoundManager();
        if (soundManager == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Sound system is not enabled.");
            return true;
        }

        boolean nowDisabled = soundManager.toggleDisabled(player.getUniqueId());

        String key = nowDisabled ? "messages.toggle-chat-sounds.disabled" : "messages.toggle-chat-sounds.enabled";
        String raw = plugin.getPluginConfig().getString(key,
                nowDisabled ? "&7Chat sounds &fdisabled&8." : "&aChat sounds &fenabled&a.");

        player.sendMessage(plugin.getChatPrefix() + ChatColor.translateAlternateColorCodes('&', raw));
        return true;
    }
}
