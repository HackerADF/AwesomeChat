package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;

import java.io.File;

public class ConfigManager {

    private final AwesomeChat plugin;
    private final File configFile;

    public ConfigManager(AwesomeChat plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        this.configFile = new File(plugin.getDataFolder(), "config.yml");

        handleConfig();
    }

    private void handleConfig() {
        if (configFile.exists()) {
            File backup = new File(plugin.getDataFolder(), "config.yml.old");

            if (backup.exists()) {
                backup.delete(); // overwrite any old backup
            }

            boolean renamed = configFile.renameTo(backup);
            if (renamed) {
                plugin.getLogger().warning("Existing config.yml was moved to config.yml.old");
            } else {
                plugin.getLogger().severe("Failed to archive old config.yml!");
            }
        }

        plugin.saveDefaultConfig();
        plugin.getLogger().info("A new config.yml has been generated. Please re-apply your settings.");
    }
}
