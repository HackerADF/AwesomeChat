package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public class ClearSelfChatCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ClearSelfChatCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0) {
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Usage: /" + label);
            return true;
        }

        int lineCount = plugin.getConfig().getInt("clearchat.line-count", 100);

        String announcementSelf = plugin.getFormattedConfigString("clearchat.announcement-self",
                "{prefix}&eYour chat has been cleared.");


        String noClearChatSelfPermissionMessage = plugin.getFormattedConfigString("clearchat.no-permission-self",
                "{prefix}&cYou do not have permission to clear your own chat.");

        String senderName = sender instanceof Player ? sender.getName() : "Console";

        String msgSelf = announcementSelf
                .replace("{sender}", senderName)
                .replace("{player}", senderName)
                .replace("{prefix}", plugin.getChatPrefix());

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can clear their own chat.");
            return true;
        } else if (!(player.hasPermission("awesomechat.clearchat.self"))) {
            player.sendMessage(noClearChatSelfPermissionMessage
                    .replace("{prefix}", plugin.getChatPrefix()));
        }

        clearChat(player, lineCount);
        player.sendMessage(msgSelf);

        return true;
    }

    private void clearChat(Player player, int lines) {
        for (int i = 0; i < lines; i++) {
            player.sendMessage("");
        }
    }
}
