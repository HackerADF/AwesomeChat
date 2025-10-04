package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class AutoBroadcasterManager {
    private final AwesomeChat plugin;

    private File autoBroadcasterFile;
    private FileConfiguration autoBroadcasterConfig;
    private int taskId = -1;
    private int currentIndex = 0;

    public AutoBroadcasterManager(AwesomeChat plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // Load AutoBroadcaster.yml
    public void loadConfig() {
        autoBroadcasterFile = new File(plugin.getDataFolder(), "AutoBroadcaster.yml");

        if (!autoBroadcasterFile.exists()) {
            plugin.saveResource("AutoBroadcaster.yml", false);
        }

        autoBroadcasterConfig = YamlConfiguration.loadConfiguration(autoBroadcasterFile);
    }

    // Save AutoBroadcaster.yml
    public void saveConfig() {
        try {
            autoBroadcasterConfig.save(autoBroadcasterFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save AutoBroadcaster.yml!", e);
        }
    }

    public void start() {
        // Cancel previous task
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        // Check if the AutoBroadcaster is enabled
        if (!autoBroadcasterConfig.getBoolean("enabled")) return;
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                List<Map<?, ?>> broadcasts = getBroadcasts();
                if (!broadcasts.isEmpty()) {
                    Map<?, ?> current = broadcasts.get(currentIndex);
                    List<String> messages = (List<String>) current.get("message");
                    String soundName = (String) current.get("sound");

                    // Send message
                    String message = AwesomeChat.formatColors(String.join("\n", messages));
                    Bukkit.broadcastMessage(message);

                    // Play sound if valid
                    if (soundName != null && !soundName.equalsIgnoreCase("none")) {
                        try {
                            Sound sound = Sound.valueOf(soundName.toUpperCase());
                            Bukkit.getOnlinePlayers().forEach(player ->
                                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f)
                            );
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid sound name in AutoBroadcaster config: " + soundName);
                        }
                    }

                    // Move to next broadcast
                    currentIndex = (currentIndex + 1) % broadcasts.size();
                }
            }
        }.runTaskTimer(plugin, 0L, getInterval() * 20L).getTaskId();

        plugin.getLogger().info("AutoBroadcaster interval: " + getInterval() + " seconds");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public List<Map<?, ?>> getBroadcasts() {
        return autoBroadcasterConfig.getMapList("broadcasts");
    }

    public long getInterval() {
        return autoBroadcasterConfig.getLong("interval", 10);
    }

    public FileConfiguration getConfig() {
        return autoBroadcasterConfig;
    }
}
