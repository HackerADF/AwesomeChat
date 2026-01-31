package dev.adf.awesomeChat.listeners;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

import static dev.adf.awesomeChat.listeners.ChatListener.formatColors;

public class JoinLeaveListener implements Listener {

    private final AwesomeChat plugin;

    public JoinLeaveListener(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getPluginConfig();

        if (!config.getBoolean("join-leave.enabled", false)) return;

        // Check vanish (EssentialsX)
        if (config.getBoolean("join-leave.hide-vanished", true) && isVanished(player)) {
            event.joinMessage(null);
            return;
        }

        // Determine join message
        boolean firstJoin = !player.hasPlayedBefore();
        String joinMsg = null;

        if (firstJoin && config.getBoolean("join-leave.first-join.enabled", true)) {
            joinMsg = config.getString("join-leave.first-join.message", "&e&l{player} joined for the first time!");
        }

        if (joinMsg == null) {
            String group = LuckPermsUtil.getPlayerGroup(player);

            if (config.getBoolean("join-leave.per-group.enabled", false)) {
                String groupPath = "join-leave.per-group.groups." + group + ".join";
                if (config.contains(groupPath)) {
                    joinMsg = config.getString(groupPath);
                }
            }

            if (joinMsg == null) {
                joinMsg = config.getString("join-leave.join-message", "&e{player} joined the game");
            }
        }

        joinMsg = applyPlaceholders(player, joinMsg);
        event.joinMessage(net.kyori.adventure.text.Component.text(formatColors(joinMsg)));

        // Join sound
        playSound(config, "join-leave.join-sound", player);

        // MOTD
        if (config.getBoolean("join-leave.motd.enabled", false)) {
            List<String> motdLines = config.getStringList("join-leave.motd.lines");
            if (!motdLines.isEmpty()) {
                // Delay MOTD slightly so it appears after join
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (String line : motdLines) {
                        String formatted = applyPlaceholders(player, line);
                        player.sendMessage(formatColors(formatted));
                    }
                }, 5L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getPluginConfig();

        if (!config.getBoolean("join-leave.enabled", false)) return;

        // Check vanish
        if (config.getBoolean("join-leave.hide-vanished", true) && isVanished(player)) {
            event.quitMessage(null);
            return;
        }

        String quitMsg = null;
        String group = LuckPermsUtil.getPlayerGroup(player);

        if (config.getBoolean("join-leave.per-group.enabled", false)) {
            String groupPath = "join-leave.per-group.groups." + group + ".leave";
            if (config.contains(groupPath)) {
                quitMsg = config.getString(groupPath);
            }
        }

        if (quitMsg == null) {
            quitMsg = config.getString("join-leave.leave-message", "&e{player} left the game");
        }

        quitMsg = applyPlaceholders(player, quitMsg);
        event.quitMessage(net.kyori.adventure.text.Component.text(formatColors(quitMsg)));

        // Leave sound
        playSound(config, "join-leave.leave-sound", null);

        // Clean up channel state on quit
        if (plugin.getChannelManager() != null) {
            plugin.getChannelManager().removePlayer(player.getUniqueId());
        }
    }

    private String applyPlaceholders(Player player, String message) {
        if (message == null) return "";
        message = message.replace("{player}", player.getName());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        return message;
    }

    private void playSound(FileConfiguration config, String path, Player joinedPlayer) {
        String soundName = config.getString(path + ".name", "none");
        if (soundName.equalsIgnoreCase("none")) return;

        float volume = (float) config.getDouble(path + ".volume", 1.0);
        float pitch = (float) config.getDouble(path + ".pitch", 1.0);

        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            target.playSound(target.getLocation(), sound, volume, pitch);
        }
    }

    private boolean isVanished(Player player) {
        // Check EssentialsX vanish
        if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
            try {
                com.earth2me.essentials.Essentials ess = (com.earth2me.essentials.Essentials)
                        Bukkit.getPluginManager().getPlugin("Essentials");
                if (ess != null) {
                    return ess.getUser(player).isVanished();
                }
            } catch (Exception ignored) {}
        }

        // Fallback: check metadata
        return player.getMetadata("vanished").stream().anyMatch(m -> m.asBoolean());
    }
}
