package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.AutoBroadcasterManager;
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
        // If the player just types "/awesomechat" with no arguments, show basic info
        if (args.length == 0) {
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "AwesomeChat Plugin v" + plugin.getDescription().getVersion());
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Use /awesomechat <reload|info> for more options.");
            return true;
        }

        // Handle internal "_view" subcommand (snapshot viewer from item display clicks)
        if (args[0].equalsIgnoreCase("_view")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
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

        // Handle "/awesomechat reload"
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("awesomechat.reload")) {
                sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You do not have permission to reload AwesomeChat.");
                return true;
            }

            // Reload the main config file
            plugin.reloadConfig();

            // Reload broadcaster manager
            AutoBroadcasterManager broadcaster = plugin.getAutoBroadcasterManager();
            if (broadcaster != null) {
                broadcaster.loadConfig();
                broadcaster.stop();
                broadcaster.start();
            }

            // Reload filter manager
            plugin.reloadFilterModule();

            // Reload channel manager
            if (plugin.getChannelManager() != null) {
                plugin.getChannelManager().loadChannels();
            }

            sender.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "AwesomeChat config reloaded and AutoBroadcaster restarted!");
            return true;
        }

        // Handle "/awesomechat info"
        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.GREEN + "AwesomeChat Plugin");
            sender.sendMessage(ChatColor.YELLOW + "Version: " + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Author: ADF");
            return true;
        }

        // If the player types an unknown argument, show a usage message
        sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Invalid usage. Try /awesomechat <reload|info>");
        return true;
    }
}
