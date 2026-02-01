package dev.adf.awesomeChat;

import dev.adf.awesomeChat.commands.*;
import dev.adf.awesomeChat.listeners.ChatListener;
import dev.adf.awesomeChat.listeners.CommandListener;
import dev.adf.awesomeChat.listeners.JoinLeaveListener;
import dev.adf.awesomeChat.managers.*;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AwesomeChat extends JavaPlugin {

    // init manager instances
    private AutoBroadcasterManager autoBroadcasterManager;
    private ChatFilterManager chatFilterManager;
    private PrivateMessageManager privateMessageManager;
    private SocialSpyManager socialSpyManager;
    private ConfigManager configManager;
    private SoundManager soundManager;
    private HoverManager hoverManager;
    private ChannelManager channelManager;
    private IgnoreManager ignoreManager;
    private MentionManager mentionManager;
    private ItemDisplayManager itemDisplayManager;
    private ChatRadiusManager chatRadiusManager;
    private ChatLogManager chatLogManager;
    private dev.adf.awesomeChat.api.AwesomeChatAPIImpl api;

    // misc
    private boolean chatMuted = false;
    private final Map<UUID, Long> broadcastCooldowns = new HashMap<>();
    private File autoBroadcasterFile;
    private FileConfiguration autoBroadcasterConfig;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        checkHardDependencies();
        registerPluginDependencyChecks();

        saveDefaultConfig();

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

        try {
            soundManager = new SoundManager(this);
            getLogger().info("SoundManager loaded.");
        } catch (Exception e) {
            getLogger().warning("SoundManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            hoverManager = new HoverManager(this);
            getLogger().info("HoverManager loaded.");
        } catch (Exception e) {
            getLogger().warning("HoverManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            channelManager = new ChannelManager(this);
            getLogger().info("ChannelManager loaded. " + channelManager.getAllChannels().size() + " channels registered.");
        } catch (Exception e) {
            getLogger().warning("ChannelManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            ignoreManager = new IgnoreManager(this);
            getLogger().info("IgnoreManager loaded.");
        } catch (Exception e) {
            getLogger().warning("IgnoreManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            mentionManager = new MentionManager(this);
            getLogger().info("MentionManager loaded.");
        } catch (Exception e) {
            getLogger().warning("MentionManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            itemDisplayManager = new ItemDisplayManager(this);
            getLogger().info("ItemDisplayManager loaded.");
        } catch (Exception e) {
            getLogger().warning("ItemDisplayManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            chatRadiusManager = new ChatRadiusManager(this);
            getLogger().info("ChatRadiusManager loaded.");
        } catch (Exception e) {
            getLogger().warning("ChatRadiusManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            if (getPluginConfig().getBoolean("chat-logging.enabled", false)) {
                chatLogManager = new ChatLogManager(this);
                getLogger().info("ChatLogManager loaded (" + getPluginConfig().getString("chat-logging.storage-type", "sqlite") + ").");
            }
        } catch (Exception e) {
            getLogger().warning("ChatLogManager failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            api = new dev.adf.awesomeChat.api.AwesomeChatAPIImpl(this);
            getLogger().info("AwesomeChatAPI v" + api.getAPIVersion() + " loaded.");
        } catch (Exception e) {
            getLogger().warning("AwesomeChatAPI failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.adf.awesomeChat.listeners.ItemDisplayListener(this), this);

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
        getCommand("channel").setExecutor(new ChannelCommand(this));
        getCommand("channel").setTabCompleter(new ChannelTabCompleter(this));
        getCommand("ignore").setExecutor(new IgnoreCommand(this));
        getCommand("ignore").setTabCompleter(new IgnoreTabCompleter());
        getCommand("clearchat").setExecutor(new ClearChatCommand(this));
        getCommand("clearchat").setTabCompleter(new ClearChatTabCompleter());
        getCommand("mutechat").setExecutor(new MuteChatCommand(this));
        getCommand("chatlogs").setExecutor(new ChatLogCommand(this));
        getCommand("chatlogs").setTabCompleter(new ChatLogTabCompleter(this));

        getLogger().info("Attempting to hook into PlaceholderAPI...");
        getLogger().info("AwesomeChat has been enabled!");

        // Install PlaceholderAPI expansions, should find a better method to implement
        //installPlaceholderExpansions();
    }

    @Override
    public void onDisable() {
        if (chatLogManager != null) {
            chatLogManager.close();
        }
        getLogger().info("AwesomeChat has been disabled!");
    }

    private void registerPluginDependencyChecks() {
        checkPluginDependency("LuckPerms", "Successfully hooked into LuckPerms", "LuckPerms has not yet initialized, waiting to hook...");
        checkPluginDependency("PlaceholderAPI", "Successfully hooked into PlaceholderAPI", "PlaceholerAPI has not yet initialized, waiting to hook...");

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
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            String replacement = ChatColor.of("#" + colorCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String convertToMiniMessageFormat(String message) {
        return message
                .replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>")
                .replace("§b", "<aqua>")
                .replace("§c", "<red>")
                .replace("§d", "<light_purple>")
                .replace("§e", "<yellow>")
                .replace("§f", "<white>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>")
                .replace("§r", "<reset>");
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

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public HoverManager getHoverManager() {
        return hoverManager;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }

    public MentionManager getMentionManager() {
        return mentionManager;
    }

    public ItemDisplayManager getItemDisplayManager() {
        return itemDisplayManager;
    }

    public ChatRadiusManager getChatRadiusManager() {
        return chatRadiusManager;
    }

    public ChatLogManager getChatLogManager() {
        return chatLogManager;
    }

    public boolean isChatMuted() {
        return chatMuted;
    }

    public boolean toggleChatMuted() {
        chatMuted = !chatMuted;
        return chatMuted;
    }

    public dev.adf.awesomeChat.api.AwesomeChatAPI getAPI() {
        return api;
    }
}

