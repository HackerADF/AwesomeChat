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

    public void loadConfig() {
        autoBroadcasterFile = new File(plugin.getDataFolder(), "modules/autobroadcaster.yml");

        if (!autoBroadcasterFile.exists()) {
            // Migrate from old location if present
            File oldFile = new File(plugin.getDataFolder(), "AutoBroadcaster.yml");
            if (oldFile.exists()) {
                oldFile.getParentFile().mkdirs();
                autoBroadcasterFile.getParentFile().mkdirs();
                oldFile.renameTo(autoBroadcasterFile);
                plugin.getLogger().info("Migrated AutoBroadcaster.yml -> modules/autobroadcaster.yml");
            } else {
                plugin.saveResource("modules/autobroadcaster.yml", false);
            }
        }

        autoBroadcasterConfig = YamlConfiguration.loadConfiguration(autoBroadcasterFile);
    }

    public void saveConfig() {
        try {
            autoBroadcasterConfig.save(autoBroadcasterFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save AutoBroadcaster.yml!", e);
        }
    }

    public void start() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (!autoBroadcasterConfig.getBoolean("enabled")) return;

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                List<Map<?, ?>> broadcasts = getBroadcasts();
                if (broadcasts.isEmpty()) return;

                try {
                    Map<?, ?> current = broadcasts.get(currentIndex);
                    List<String> messages = (List<String>) current.get("message");

                    if (messages == null || messages.isEmpty()) {
                        currentIndex = (currentIndex + 1) % broadcasts.size();
                        return;
                    }

                    String soundName = (String) current.get("sound");

                    String message = AwesomeChat.formatColors(String.join("\n", messages));
                    Bukkit.broadcastMessage(message);

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

                    currentIndex = (currentIndex + 1) % broadcasts.size();

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error processing AutoBroadcaster broadcast: " + e.getMessage(), e);
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
