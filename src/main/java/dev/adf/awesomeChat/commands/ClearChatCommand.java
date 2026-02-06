package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public class ClearChatCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ClearChatCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String mode = "all";
        if (args.length > 0) {
            mode = args[0].toLowerCase();
        }

        int lineCount = plugin.getConfig().getInt("clearchat.line-count", 100);
        String announcement = plugin.getFormattedConfigString("clearchat.announcement",
                "{prefix}&7Chat has been cleared by &b{player}&7.");
        String announcementSelf = plugin.getFormattedConfigString("clearchat.announcement-self",
                "{prefix}&eYour chat has been cleared.");

        String bypassMessage = plugin.getFormattedConfigString("clearchat.bypass-message",
                "&7&oYour chat wasn't cleared due to the bypass permission.");

        String noClearChatSelfPermissionMessage = plugin.getFormattedConfigString("clearchat.no-permission-self",
                "{prefix}&cYou do not have permission to clear your own chat.");
        String noClearChatAllPermissionMessage = plugin.getFormattedConfigString("clearchat.no-permission-all",
                "{prefix}&cYou don't have permission to clear chat for everyone.");

        String senderName = sender instanceof Player ? sender.getName() : "Console";

        String msg = announcement
                .replace("{sender}", senderName)
                .replace("{player}", senderName)
                .replace("{prefix}", plugin.getChatPrefix());
        String msgSelf = announcementSelf
                .replace("{sender}", senderName)
                .replace("{player}", senderName)
                .replace("{prefix}", plugin.getChatPrefix());

        switch (mode) {
            case "self" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can clear their own chat.");
                    return true;
                } else if (!(player.hasPermission("awesomechat.clearchat.self"))) {
                    player.sendMessage(noClearChatSelfPermissionMessage
                            .replace("{prefix}", plugin.getChatPrefix()));
                }
                clearChat(player, lineCount);
                player.sendMessage(msgSelf);
            }
            case "all" -> {
                if (!sender.hasPermission("awesomechat.clearchat.all")) {
                    sender.sendMessage(noClearChatAllPermissionMessage
                            .replace("{prefix}", plugin.getChatPrefix())
                            .replace("{player}", senderName)
                            .replace("{sender}", senderName));
                    return true;
                }

                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.hasPermission("awesomechat.clearchat.bypass")) {
                        clearChat(target, lineCount);
                    }
                }

                if (plugin.getConfig().getBoolean("clearchat.show-announcement", true)) {
                    Bukkit.broadcastMessage(msg);
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        if (target.hasPermission("awesomechat.clearchat.bypass")) {
                            target.sendMessage(bypassMessage
                                    .replace("{prefix}", plugin.getChatPrefix())
                                    .replace("{player}", senderName)
                                    .replace("{sender}", senderName)
                            );
                        }
                    }
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
