package dev.adf.awesomeChat;

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

        // Handle "/awesomechat reload"
        if (args[0].equalsIgnoreCase("reload")) {
            // Check if the player has permission to reload the plugin
            if (!sender.hasPermission("awesomechat.reload")) {
                sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You do not have permission to reload AwesomeChat.");
                return true;
            }

            // Reload the main config file
            plugin.reloadConfig();

            // Reload the AutoBroadcaster settings and restart the task to apply changes
            plugin.loadAutoBroadcasterConfig();
            plugin.startAutoBroadcaster();

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
