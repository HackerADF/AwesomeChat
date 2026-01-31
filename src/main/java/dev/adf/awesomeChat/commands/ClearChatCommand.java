package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChatCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ClearChatCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String mode = "all";
        if (args.length > 0) {
            mode = args[0].toLowerCase();
        }

        int lineCount = plugin.getConfig().getInt("clearchat.line-count", 100);
        String announcement = plugin.getFormattedConfigString("clearchat.announcement",
                "&7Chat has been cleared by &b{player}&7.");

        switch (mode) {
            case "self" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can clear their own chat.");
                    return true;
                }
                clearChat(player, lineCount);
                player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Your chat has been cleared.");
            }
            case "all" -> {
                if (!sender.hasPermission("awesomechat.clearchat.all")) {
                    sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You don't have permission to clear chat for everyone.");
                    return true;
                }

                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.hasPermission("awesomechat.clearchat.bypass")) {
                        clearChat(target, lineCount);
                    }
                }

                String senderName = sender instanceof Player ? sender.getName() : "Console";
                String msg = announcement.replace("{player}", senderName);

                if (plugin.getConfig().getBoolean("clearchat.show-announcement", true)) {
                    Bukkit.broadcastMessage(plugin.getChatPrefix() + msg);
                }

                if (plugin.getConfig().getBoolean("clearchat.log", true)) {
                    plugin.getLogger().info("Chat cleared by " + senderName);
                }
            }
            default -> {
                sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Usage: /" + label + " [self|all]");
            }
        }

        return true;
    }

    private void clearChat(Player player, int lines) {
        for (int i = 0; i < lines; i++) {
            player.sendMessage("");
        }
    }
}
