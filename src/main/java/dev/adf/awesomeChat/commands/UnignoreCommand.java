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

import java.util.UUID;

public class UnignoreCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public UnignoreCommand(AwesomeChat plugin) {
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

        if (args.length != 1) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.GRAY + "Usage: /" + label + " <player>");
            return true;
        }

        // Resolve target — online first, then offline cache
        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target;

        if (onlineTarget != null) {
            target = onlineTarget;
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (!offline.hasPlayedBefore()) {
                player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Player not found or has never joined the server.");
                return true;
            }
            target = offline;
        }

        UUID targetId = target.getUniqueId();

        if (targetId.equals(player.getUniqueId())) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You cannot ignore nor unignore yourself.");
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : args[0];

        if (manager.isIgnoring(player.getUniqueId(), targetId)) {
            manager.setIgnore(player.getUniqueId(), targetId, false);
            player.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "You are no longer ignoring " + ChatColor.WHITE + targetName + ChatColor.GREEN + ".");
        } else {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.GRAY + "You are not currently ignoring " + ChatColor.WHITE + targetName + ChatColor.DARK_GRAY + ".");
        }

        return true;
    }
}
