package dev.adf.awesomeChat;

import dev.adf.awesomeChat.commands.*;
import dev.adf.awesomeChat.managers.*;

import org.bukkit.Bukkit;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AwesomeChat extends JavaPlugin {

    // init manager instances
    private AutoBroadcasterManager autoBroadcasterManager;
    private ChatFilterManager chatFilterManager;
    private PrivateMessageManager privateMessageManager;
    private SocialSpyManager socialSpyManager;

    // misc
    private final Map<UUID, Long> broadcastCooldowns = new HashMap<>();
    private File autoBroadcasterFile;
    private FileConfiguration autoBroadcasterConfig;

    @Override
    public void onEnable() {
        checkHardDependencies();
        registerPluginDependencyChecks();

        // Save default config if it doesn't exist
        saveDefaultConfig();

        // initialize the manager classes safely
        try {
            privateMessageManager = new PrivateMessageManager();
            getLogger().info("PrivateMessageManager loaded.");
        } catch (Exception e) {
            getLogger().warning("PrivateMessageManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            socialSpyManager = new SocialSpyManager();
            getLogger().info("SocialSpyManager loaded.");
        } catch (Exception e) {
            getLogger().warning("SocialSpyManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            chatFilterManager = new ChatFilterManager(this);
            getLogger().info("ChatFilterManager loaded.");
        } catch (Exception e) {
            getLogger().warning("ChatFilterManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            autoBroadcasterManager = new AutoBroadcasterManager(this);
            autoBroadcasterManager.start();
            getLogger().info("AutoBroadcasterManager loaded.");
        } catch (Exception e) {
            getLogger().warning("AutoBroadcasterManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        // Register the event listener for chat
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);

        // Register commands
        getCommand("awesomechat").setExecutor(new AwesomeChatCommand(this));
        getCommand("awesomechat").setTabCompleter(new AwesomeChatTabCompleter());
        getCommand("broadcast").setExecutor(new BroadcastCommand(this));
        getCommand("msg").setExecutor(new MessageCommand(this));
        getCommand("message").setTabCompleter(new MessageTabCompleter());
        getCommand("reply").setExecutor(new ReplyCommand(this));
        getCommand("msgtoggle").setExecutor(new MsgToggleCommand(this));
        getCommand("msgtoggle").setTabCompleter(new MsgToggleTabCompleter());
        getCommand("socialspy").setExecutor(new SocialSpyCommand(this));
        getCommand("socialspy").setTabCompleter(new SocialSpyTabCompleter());

        getLogger().info("Attempting to hook into PlaceholderAPI...");
        getLogger().info("AwesomeChat has been enabled!");

        // Install PlaceholderAPI expansions, should find a better method to implement
        //installPlaceholderExpansions();
    }

    @Override
    public void onDisable() {
        getLogger().info("AwesomeChat has been disabled!");
    }

    private void registerPluginDependencyChecks() {
        // Check immediately for already loaded plugins
        checkPluginDependency("LuckPerms", "Successfully hooked into LuckPerms", "LuckPerms has not yet initialized, waiting to hook...");
        checkPluginDependency("PlaceholderAPI", "Successfully hooked into PlaceholderAPI", "PlaceholerAPI has not yet initialized, waiting to hook...");

        // Listen for plugins that enable after AwesomeChat
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                String name = event.getPlugin().getName();
                switch (name) {
                    case "LuckPerms" -> checkPluginDependency("LuckPerms", "Successfully hooked into LuckPerms", "LuckPerms is not installed. This is a dependency, AwesomeChat will be disabled.");
                    case "PlaceholderAPI" -> checkPluginDependency("PlaceholderAPI", "Successfully hooked into PlaceholderAPI", "PlaceholderAPI is not installed. Most placeholders will not work.");
                }
            }
        }, this);
    }

    private void checkHardDependencies() {
        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().severe("LuckPerms is not installed! AwesomeChat cannot run without it.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Successfully hooked into LuckPerms");
    }

    /**
     * Checks if a plugin is installed and logs its status
     */
    private void checkPluginDependency(String pluginName, String successMessage, String failureMessage) {
        getLogger().info("Pulling server plugins:");
        for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            getLogger().info(" - " + plugin.getName() + " v" + plugin.getDescription().getVersion());
        }
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
     * Applies a cooldown for a player
     *
     * @param playerId The player's UUID
     */
    public void setCooldown(UUID playerId) {
        broadcastCooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Gets the timestamp of when the player last triggered a broadcast cooldown.
     *
     * @param playerId The player's UUID
     * @return The timestamp in milliseconds since epoch of the player's last broadcast,
     *         or 0 if the player has never triggered a cooldown
     */
    public long getCooldown(UUID playerId) {
        return broadcastCooldowns.getOrDefault(playerId, 0L);
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
    public String getFormattedConfigString(String path, String defaultValue) {
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

    public void reloadFilterModule() {
        try {
            chatFilterManager = new ChatFilterManager(this);
            getLogger().info("ChatFilterManager reloaded.");
        } catch (Exception e) {
            getLogger().warning("ChatFilterManager failed to reload: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public ChatFilterManager getChatFilterManager() {
        return chatFilterManager;
    }

    public PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public SocialSpyManager getSocialSpyManager() {
        return socialSpyManager;
    }
    public AutoBroadcasterManager getAutoBroadcasterManager() {
        return autoBroadcasterManager;
    }
}

