package dev.adf.awesomeChat;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AwesomeChat extends JavaPlugin {

    // Cooldowns for broadcast
    private final Map<UUID, Long> broadcastCooldowns = new HashMap<>();

    private int currentAutoBroadcastIndex = 0;

    // AutoBroadcaster configuration file
    private File autoBroadcasterFile;
    private FileConfiguration autoBroadcasterConfig;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Load AutoBroadcaster.yml
        loadAutoBroadcasterConfig();

        // Register the event listener for chat
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Register commands
        getCommand("awesomechat").setExecutor(new AwesomeChatCommand(this));
        getCommand("awesomechat").setTabCompleter(new AwesomeChatTabCompleter());
        getCommand("broadcast").setExecutor(new BroadcastCommand(this));

        // Start AutoBroadcaster
        startAutoBroadcaster();

        // Check fir LuckPerms and PAPI
        checkPluginDependency("LuckPerms", "Successfully hooked into LuckPerms", "LuckPerms is not installed. Prefixes will not work.");
        checkPluginDependency("PlaceholderAPI", "Successfully hooked into PlaceholderAPI", "PlaceholderAPI is not installed. Most placeholders will not work.");

        getLogger().info("AwesomeChat has been enabled!");

        // Install PlaceholderAPI expansions, should find a better method to implement
        //installPlaceholderExpansions();
    }

    @Override
    public void onDisable() {
        getLogger().info("AwesomeChat has been disabled!");
    }


    /**
     * Checks if a plugin is installed and logs its status
     */
    private void checkPluginDependency(String pluginName, String successMessage, String failureMessage) {
        if (isPluginEnabled(pluginName)) {
            getLogger().info(successMessage);
        } else {
            getLogger().warning(failureMessage);
        }
    }

    /**
     * Checks if a plugin is enabled
     *
     * @param pluginName Name of the plugin to check
     * @return True if the plugin is enabled, false otherwise
     */
    public boolean isPluginEnabled(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Returns the chat prefix from the config
     *
     * @return The formatted chat prefix
     */
    public String getChatPrefix() {
        return getFormattedConfigString("prefix", "&7[&bAwesomeChat&7] ");
    }

    /**
     * Returns the formatted broadcast message template
     *
     * @return List of formatted broadcast messages
     */
    public List<String> getBroadcastFormat() {
        return getConfig().getStringList("broadcast.format");
    }

    /**
     * Returns the broadcast sound effect from config
     *
     * @return The sound as a string
     */
    public String getBroadcastSound() {
        return getConfig().getString("broadcast.sound", "none");
    }

    /**
     * Get all the broadcast entries from AutoBroadcaster.yml
     *
     * @return List of broadcasts with message and sound info
     */
    public List<Map<?, ?>> getAutoBroadcasterBroadcasts() {
        return getAutoBroadcasterConfig().getMapList("broadcasts");
    }

    /**
     * Get the interval for the AutoBroadcaster (in seconds)
     *
     * @return The interval for broadcasting
     */
    public long getAutoBroadcasterInterval() {
        return getAutoBroadcasterConfig().getLong("interval", 10); // Default is 10 seconds
    }

    /**
     * Applies a cooldown for a player
     *
     * @param playerId The player's UUID
     */
    public void setCooldown(UUID playerId) {
        broadcastCooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Checks if a player is on cooldown
     *
     * @param playerId         The player's UUID
     * @param cooldownSeconds  Cooldown time in seconds
     * @return True if the player is still on cooldown, false otherwise
     */
    public boolean isOnCooldown(UUID playerId, int cooldownSeconds) {
        long currentTime = System.currentTimeMillis();
        if (broadcastCooldowns.containsKey(playerId)) {
            long lastUse = broadcastCooldowns.get(playerId);
            long elapsedTime = (currentTime - lastUse) / 1000;
            return elapsedTime < cooldownSeconds;
        }
        return false;
    }

    /**
     * Formats a string from the config with color codes
     *
     * @param path         Config path
     * @param defaultValue Default value if not found
     * @return The formatted string
     */
    private String getFormattedConfigString(String path, String defaultValue) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', getConfig().getString(path, defaultValue));
    }

    public static String formatColors(String message) {
        // Convert hex colors (&#RRGGBB) to Bukkit format
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            String replacement = ChatColor.of("#" + colorCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        // Convert Bukkit color codes (&a, &b, &l, and so on)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String convertToMiniMessageFormat(String message) {
        // Replace legacy color codes with MiniMessage format
        return message
                .replace("§c", "<red>")
                .replace("§l", "<bold>")
                .replace("§7", "<gray>")
                .replace("§3", "<blue>") // Add all other necessary replacements here
                ;
    }

    /**
     * Executes a command from the console
     *
     * @param command The command to execute
     */
    private void runConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * Downloads PlaceholderAPI expansions
     */
    private void installPlaceholderExpansions() {
        if (isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("Downloading PlaceholderAPI Expansions...");
            runConsoleCommand("papi ecloud download Player");
            runConsoleCommand("papi ecloud download Essentials");
            runConsoleCommand("papi ecloud download LuckPerms");
            runConsoleCommand("papi ecloud download Vault");
            runConsoleCommand("papi reload");
        }
    }

    /**
     * Get the configuration for this plugin
     *
     * @return FileConfiguration instance
     */
    public FileConfiguration getPluginConfig() {
        return getConfig();
    }

    public long getCooldown(UUID playerId) {
        return broadcastCooldowns.getOrDefault(playerId, 0L);
    }

    public void startAutoBroadcaster() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Map<?, ?>> broadcasts = getAutoBroadcasterBroadcasts();
                if (!broadcasts.isEmpty()) {
                    Map<?, ?> currentBroadcast = broadcasts.get(currentAutoBroadcastIndex);
                    List<String> messages = (List<String>) currentBroadcast.get("message");
                    String soundName = (String) currentBroadcast.get("sound");

                    // Format the messages (colors will be applied)
                    String message = formatColors(String.join("\n", messages));
                    Bukkit.broadcastMessage(message);

                    // Play sound if valid
                    if (!soundName.equalsIgnoreCase("none")) {
                        try {
                            Sound sound = Sound.valueOf(soundName.toUpperCase());
                            Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), sound, 1.0f, 1.0f));
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("Invalid sound name in AutoBroadcaster config: " + soundName);
                        }
                    }

                    // Move to the next broadcast in the list
                    currentAutoBroadcastIndex = (currentAutoBroadcastIndex + 1) % broadcasts.size();
                }
            }
        }.runTaskTimer(this, 0L, getAutoBroadcasterInterval() * 20L); // Interval is in seconds
    }


    // Load AutoBroadcaster.yml file
    public void loadAutoBroadcasterConfig() {
        autoBroadcasterFile = new File(getDataFolder(), "AutoBroadcaster.yml");

        if (!autoBroadcasterFile.exists()) {
            saveResource("AutoBroadcaster.yml", false);
        }

        autoBroadcasterConfig = YamlConfiguration.loadConfiguration(autoBroadcasterFile);
    }

    // Save AutoBroadcaster.yml
    public void saveAutoBroadcasterConfig() {
        try {
            autoBroadcasterConfig.save(autoBroadcasterFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save AutoBroadcaster.yml!", e);
        }
    }


    /**
     * Retrieves AutoBroadcaster config
     *
     * @return AutoBroadcaster FileConfiguration
     */
    public FileConfiguration getAutoBroadcasterConfig() {
        return autoBroadcasterConfig;
    }
}
