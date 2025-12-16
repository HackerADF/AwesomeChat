package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.configuration.file.FileConfiguration;

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
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getLogger().info("Generated a new config.yml (no previous file existed).");
            return;
        }

        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        int version = cfg.getInt("config-version", -1);

        if (version == 3) {
            plugin.getLogger().info("Config version is up-to-date (3). No regeneration needed.");
            return;
        }
        plugin.getLogger().warning("Config version outdated (" + version + "). Upgrading to version 3...");

        File backup = new File(plugin.getDataFolder(), "config.yml.old");
        if (backup.exists()) backup.delete();

        boolean success = configFile.renameTo(backup);

        if (success) {
            plugin.getLogger().warning("Old config.yml has been moved to config.yml.old");
        } else {
            plugin.getLogger().severe("Failed to backup old config.yml!");
        }

        plugin.saveDefaultConfig();
        plugin.getLogger().info("A new config.yml has been generated. Please re-apply custom settings.");
    }
}
