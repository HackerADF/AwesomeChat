package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AwesomeChatCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public AwesomeChatCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No args, show plugin info
        if (args.length == 0) {
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "AwesomeChat Plugin v" + plugin.getDescription().getVersion());
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Use /awesomechat <reload|info> for more options.");
            return true;
        }

        // Internal _view subcommand, opens the item display snapshot GUI
        if (args[0].equalsIgnoreCase("_view")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                return true;
            }

            // Permission gate is optional, controlled by config
            boolean requirePermission = plugin.getConfig().getBoolean("item-display.require-permission-to-view", false);
            if (requirePermission && !player.hasPermission("awesomechat.display.view")) {
                player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You don't have permission to view item displays.");
                return true;
            }

            if (args.length < 2) return true;
            try {
                java.util.UUID snapshotId = java.util.UUID.fromString(args[1]);
                dev.adf.awesomeChat.managers.ItemDisplayManager manager = plugin.getItemDisplayManager();
                if (manager != null) {
                    manager.openSnapshot(player, snapshotId);
                }
            } catch (IllegalArgumentException ignored) {
            }
            return true;
        }

        // Reload everything
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("awesomechat.reload")) {
                sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You do not have permission to reload AwesomeChat.");
                return true;
            }

            plugin.reloadConfig();
            plugin.reloadFilterModule();

            // Broadcaster
            AutoBroadcasterManager broadcaster = plugin.getAutoBroadcasterManager();
            if (broadcaster != null) {
                broadcaster.loadConfig();
                broadcaster.stop();
                broadcaster.start();
            }

            // Channels
            if (plugin.getChannelManager() != null) {
                plugin.getChannelManager().loadChannels();
            }

            // Item display
            if (plugin.getItemDisplayManager() != null) {
                plugin.getItemDisplayManager().reloadConfig();
            }

            // Chat logs (db connection might have changed)
            plugin.reloadChatLogManager();

            sender.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "AwesomeChat reloaded (config.yml + modules/).");
            return true;
        }

        // Info command
        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.GREEN + "AwesomeChat Plugin");
            sender.sendMessage(ChatColor.YELLOW + "Version: " + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Author: ADF");
            return true;
        }

        // Unknown subcommand
        sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Invalid usage. Try /awesomechat <reload|info>");
        return true;
    }
}
