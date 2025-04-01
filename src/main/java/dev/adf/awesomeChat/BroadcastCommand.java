package dev.adf.awesomeChat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BroadcastCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public BroadcastCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "Command to broadcast announcements in chat.");
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Use /broadcast <message>");
            return true;
        }

        if (!plugin.getConfig().getBoolean("broadcast.enabled", true)) {
            sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Broadcasts are disabled in the config.");
            return true;
        }

        // Get cooldown duration from permission node (e.g., awesomechat.broadcast.cooldown.10)
        int cooldownSeconds = getCooldownFromPermissions(sender);
        if (sender instanceof Player player) {
            if (cooldownSeconds > 0 && plugin.isOnCooldown(player.getUniqueId(), cooldownSeconds)) {
                long timeLeft = (cooldownSeconds - (System.currentTimeMillis() - plugin.getCooldown(player.getUniqueId())) / 1000);
                sender.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You must wait " + timeLeft + " seconds before broadcasting again.");
                return true;
            }
            plugin.setCooldown(player.getUniqueId());
        }

        // Build the broadcast message
        String rawMessage = String.join(" ", args);
        List<String> formatLines = plugin.getBroadcastFormat();

        for (String line : formatLines) {
            String formattedLine = line.replace("%message%", rawMessage);
            formattedLine = ChatColor.translateAlternateColorCodes('&', formattedLine);
            Bukkit.broadcastMessage(formattedLine);
        }

        // Play sound if enabled
        String soundName = plugin.getBroadcastSound();
        if (!soundName.equalsIgnoreCase("none")) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound '" + soundName + "' in config. Skipping sound effect.");
            }
        }

        return true;
    }

    /**
     * Extracts cooldown duration from permissions (e.g., awesomechat.broadcast.cooldown.10)
     *
     * - Players with `awesomechat.broadcast.cooldown.5` will have a 5s cooldown.
     * - If no permission is found, cooldown defaults to 0 (no cooldown).
     */
    private int getCooldownFromPermissions(CommandSender sender) {
        if (sender instanceof Player player) {
            for (String permission : player.getEffectivePermissions().stream().map(p -> p.getPermission()).toList()) {
                Matcher matcher = Pattern.compile("awesomechat\\.broadcast\\.cooldown\\.(\\d+)").matcher(permission);
                if (matcher.matches()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        }
        return 0; // Default: No cooldown
    }
}
