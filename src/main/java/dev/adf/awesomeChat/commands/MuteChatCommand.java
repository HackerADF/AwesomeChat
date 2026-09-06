package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MuteChatCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public MuteChatCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean nowMuted = plugin.toggleChatMuted();

        String senderName = sender.getName();
        String muteMsg = plugin.getFormattedConfigString("mutechat.muted-message",
                "{prefix}&c&lChat has been muted by {player}.");
        String unmuteMsg = plugin.getFormattedConfigString("mutechat.unmuted-message",
                "{prefix}&a&lChat has been unmuted by {player}.");

        String announcement = (nowMuted ? muteMsg : unmuteMsg)
                .replace("{sender}", senderName)
                .replace("{player}", senderName)
                .replace("{prefix}", plugin.getChatPrefix());

        if (plugin.getConfig().getBoolean("mutechat.announce", true)) {
            Bukkit.broadcastMessage(announcement);
        } else {
            sender.sendMessage(announcement);
        }

        if (plugin.getConfig().getBoolean("mutechat.log", true)) {
            plugin.getLogger().info("Chat " + (nowMuted ? "muted" : "unmuted") + " by " + senderName);
        }

        return true;
    }
}
