package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.IgnoreManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class IgnoreListCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public IgnoreListCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        IgnoreManager manager = plugin.getIgnoreManager();
        if (manager == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Ignore system is not enabled.");
            return true;
        }

        Set<UUID> ignored = manager.getIgnoredPlayers(player.getUniqueId());

        if (ignored.isEmpty()) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.GRAY + "You are not ignoring anyone.");
            return true;
        }

        player.sendMessage(plugin.getChatPrefix() + ChatColor.GRAY + "Ignored players " + ChatColor.DARK_GRAY + "(" + ChatColor.WHITE + ignored.size() + ChatColor.DARK_GRAY + "):");
        for (UUID id : ignored) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(id);
            String name = target.getName() != null ? target.getName() : id.toString();
            String status = target.isOnline() ? ChatColor.DARK_GRAY + " (" + ChatColor.GREEN + "online" + ChatColor.DARK_GRAY + ")" : "";
            player.sendMessage(ChatColor.DARK_GRAY + "  - " + ChatColor.WHITE + name + status);
        }

        return true;
    }
}
