package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static final int CURRENT_VERSION = 17;

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
            plugin.getLogger().info("Generated new config.yml (v" + CURRENT_VERSION + ").");
            return;
        }

        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        int version = config.getInt("config-version", -1);

        if (version >= CURRENT_VERSION) {
            plugin.getLogger().info("Config is up to date (v" + version + ").");
            return;
        }

        if (version == -1) {
            version = 1;
        }

        plugin.getLogger().info("Migrating config from v" + version + " to v" + CURRENT_VERSION + "...");

        backupConfig(version);

        if (version < 4) migrateToV4(config);
        if (version < 5) migrateToV5(config);
        if (version < 6) migrateToV6(config);
        if (version < 7) migrateToV7(config);
        if (version < 8) migrateToV8(config);
        if (version < 9) migrateToV9(config);
        if (version < 10) migrateToV10(config);
        if (version < 15) migrateToV15(config);
        if (version < 16) migrateToV16(config);
        if (version < 17) migrateToV17(config);

        config.set("config-version", CURRENT_VERSION);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save migrated config: " + e.getMessage());
            return;
        }

        plugin.reloadConfig();
        plugin.getLogger().info("Config migrated to v" + CURRENT_VERSION + " successfully.");
    }

    private void backupConfig(int oldVersion) {
        File backup = new File(plugin.getDataFolder(), "config.yml.v" + oldVersion + ".bak");
        try {
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Config backed up to " + backup.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to backup config: " + e.getMessage());
        }
    }

    // =========================================================================
    //  v3 -> v4: Hover restructure, sound restructure, channels, join/leave
    // =========================================================================
    private void migrateToV4(FileConfiguration config) {
        plugin.getLogger().info("  Running v3 -> v4 migration...");

        // --- color-codes.permission-based ---
        setIfAbsent(config, "color-codes.permission-based", false);

        // --- Restructure hoverable-messages ---
        if (config.contains("hoverable-messages.text-lines") && !config.contains("hoverable-messages.global")) {
            List<String> oldLines = config.getStringList("hoverable-messages.text-lines");
            config.set("hoverable-messages.text-lines", null);

            config.set("hoverable-messages.global.username", oldLines);
            config.set("hoverable-messages.global.username-click-action", "/msg %player% ");
            config.set("hoverable-messages.global.username-click-type", "suggest");
            config.set("hoverable-messages.global.message", Arrays.asList(
                "&7Message sent at: &f%server_time_HH:mm:ss%",
                "&7Click to copy message"
            ));
            config.set("hoverable-messages.global.message-click-action", "%message%");
            config.set("hoverable-messages.global.message-click-type", "copy");

            config.set("hoverable-messages.per-group.enabled", false);
            plugin.getLogger().info("    Migrated hoverable-messages to component-based structure.");
        } else if (!config.contains("hoverable-messages.global")) {
            config.set("hoverable-messages.global.username", Arrays.asList(
                "&bPlayer&f: &f%player%",
                "&bGroup&f: &f%luckperms_primary_group_name%",
                "",
                "&aClick to message!"
            ));
            config.set("hoverable-messages.global.username-click-action", "/msg %player% ");
            config.set("hoverable-messages.global.username-click-type", "suggest");
            config.set("hoverable-messages.global.message", Arrays.asList(
                "&7Message sent at: &f%server_time_HH:mm:ss%",
                "&7Click to copy message"
            ));
            config.set("hoverable-messages.global.message-click-action", "%message%");
            config.set("hoverable-messages.global.message-click-type", "copy");
            config.set("hoverable-messages.per-group.enabled", false);
        }

        // --- Restructure chat-format.sound ---
        if (config.contains("chat-format.sound.name") && !config.contains("chat-format.sound.global")) {
            String oldName = config.getString("chat-format.sound.name", "ENTITY_CHICKEN_EGG");
            Object oldVolume = config.get("chat-format.sound.volume", 100);
            Object oldPitch = config.get("chat-format.sound.pitch", 1);

            config.set("chat-format.sound.name", null);
            config.set("chat-format.sound.volume", null);
            config.set("chat-format.sound.pitch", null);

            config.set("chat-format.sound.enabled", true);
            config.set("chat-format.sound.global.name", oldName);
            config.set("chat-format.sound.global.volume", oldVolume);
            config.set("chat-format.sound.global.pitch", oldPitch);
            config.set("chat-format.sound.per-group.enabled", false);
            plugin.getLogger().info("    Migrated chat-format.sound to global/per-group structure.");
        } else if (!config.contains("chat-format.sound.global")) {
            config.set("chat-format.sound.enabled", true);
            config.set("chat-format.sound.global.name", "ENTITY_CHICKEN_EGG");
            config.set("chat-format.sound.global.volume", 100);
            config.set("chat-format.sound.global.pitch", 2);
            config.set("chat-format.sound.per-group.enabled", false);
        }

        // --- Add join-leave section ---
        if (!config.contains("join-leave")) {
            config.set("join-leave.enabled", false);
            config.set("join-leave.join-message", "&8[&a+&8] &7{player}");
            config.set("join-leave.leave-message", "&8[&c-&8] &7{player}");
            config.set("join-leave.hide-vanished", true);

            config.set("join-leave.first-join.enabled", true);
            config.set("join-leave.first-join.message", "&e&lWelcome &b{player} &e&lto the server for the first time!");

            config.set("join-leave.per-group.enabled", false);
            config.set("join-leave.per-group.groups.owner.join", "&8[&a+&8] &c&lOwner &f{player} &7has joined");
            config.set("join-leave.per-group.groups.owner.leave", "&8[&c-&8] &c&lOwner &f{player} &7has left");
            config.set("join-leave.per-group.groups.admin.join", "&8[&a+&8] &6Admin &f{player} &7has joined");
            config.set("join-leave.per-group.groups.admin.leave", "&8[&c-&8] &6Admin &f{player} &7has left");
            config.set("join-leave.per-group.groups.default.join", "&8[&a+&8] &7{player}");
            config.set("join-leave.per-group.groups.default.leave", "&8[&c-&8] &7{player}");

            config.set("join-leave.join-sound.name", "none");
            config.set("join-leave.join-sound.volume", 1.0);
            config.set("join-leave.join-sound.pitch", 1.0);
            config.set("join-leave.leave-sound.name", "none");
            config.set("join-leave.leave-sound.volume", 1.0);
            config.set("join-leave.leave-sound.pitch", 1.0);

            config.set("join-leave.motd.enabled", false);
            config.set("join-leave.motd.lines", Arrays.asList(
                "",
                "&8&l&m[                                                 ]",
                "&7  Welcome to &b&lYour Server&7!",
                "&7  Type &e/help &7to get started.",
                "&8&l&m[                                                 ]",
                ""
            ));
            plugin.getLogger().info("    Added join-leave section.");
        }

        // --- Add channels section ---
        if (!config.contains("channels")) {
            config.set("channels.staff.display-name", "Staff");
            config.set("channels.staff.prefix", "&8[&c&lSTAFF&8]&r ");
            config.set("channels.staff.permission", "awesomechat.channel.staff");
            config.set("channels.staff.format", "{prefix}{player}&7: &f{message}");
            config.set("channels.staff.sound", "BLOCK_NOTE_BLOCK_PLING");
            config.set("channels.staff.volume", 0.5);
            config.set("channels.staff.pitch", 1.5);
            config.set("channels.staff.auto-join", false);

            config.set("channels.admin.display-name", "Admin");
            config.set("channels.admin.prefix", "&8[&4&lADMIN&8]&r ");
            config.set("channels.admin.permission", "awesomechat.channel.admin");
            config.set("channels.admin.format", "{prefix}{player}&7: &f{message}");
            config.set("channels.admin.sound", "BLOCK_NOTE_BLOCK_BELL");
            config.set("channels.admin.volume", 0.5);
            config.set("channels.admin.pitch", 1.0);
            config.set("channels.admin.auto-join", false);

            config.set("channels.vip.display-name", "VIP");
            config.set("channels.vip.prefix", "&8[&6&lVIP&8]&r ");
            config.set("channels.vip.permission", "awesomechat.channel.vip");
            config.set("channels.vip.format", "{prefix}{player}&7: &e{message}");
            config.set("channels.vip.sound", "none");
            config.set("channels.vip.volume", 1.0);
            config.set("channels.vip.pitch", 1.0);
            config.set("channels.vip.auto-join", false);
            plugin.getLogger().info("    Added channels section (staff, admin, vip).");
        }
    }

    // =========================================================================
    //  v4 -> v5: Mutechat, clearchat, emoji, censor mode
    // =========================================================================
    private void migrateToV5(FileConfiguration config) {
        plugin.getLogger().info("  Running v4 -> v5 migration...");

        // --- Add mutechat section ---
        if (!config.contains("mutechat")) {
            config.set("mutechat.announce", true);
            config.set("mutechat.log", true);
            config.set("mutechat.muted-message", "&c&lChat has been muted by {player}.");
            config.set("mutechat.unmuted-message", "&a&lChat has been unmuted by {player}.");
            config.set("mutechat.player-message", "&cChat is currently muted.");
            plugin.getLogger().info("    Added mutechat section.");
        }

        // --- Add clearchat section ---
        if (!config.contains("clearchat")) {
            config.set("clearchat.line-count", 100);
            config.set("clearchat.show-announcement", true);
            config.set("clearchat.announcement", "&7Chat has been cleared by &b{player}&7.");
            config.set("clearchat.log", true);
            plugin.getLogger().info("    Added clearchat section.");
        }

        // --- Add emoji section ---
        if (!config.contains("emoji")) {
            config.set("emoji.enabled", false);
            Map<String, String> shortcuts = new LinkedHashMap<>();
            shortcuts.put("heart", "\u2764");
            shortcuts.put("star", "\u2B50");
            shortcuts.put("skull", "\u2620");
            shortcuts.put("check", "\u2714");
            shortcuts.put("cross", "\u2716");
            shortcuts.put("fire", "\uD83D\uDD25");
            shortcuts.put("smile", "\u263A");
            shortcuts.put("sad", "\u2639");
            shortcuts.put("arrow", "\u27A4");
            shortcuts.put("music", "\u266B");
            shortcuts.put("diamond", "\u2666");
            shortcuts.put("crown", "\u2654");
            shortcuts.put("sword", "\u2694");
            shortcuts.put("shield", "\uD83D\uDEE1");
            shortcuts.put("wave", "\uD83D\uDC4B");
            shortcuts.put("thumbsup", "\uD83D\uDC4D");
            shortcuts.put("thumbsdown", "\uD83D\uDC4E");
            shortcuts.put("eyes", "\uD83D\uDC40");
            shortcuts.put("warning", "\u26A0");
            shortcuts.put("sparkle", "\u2728");
            for (Map.Entry<String, String> entry : shortcuts.entrySet()) {
                config.set("emoji.shortcuts." + entry.getKey(), entry.getValue());
            }
            plugin.getLogger().info("    Added emoji section with 20 default shortcuts.");
        }

        // --- Add filter-mode and censor-char to chat-filter ---
        setIfAbsent(config, "chat-filter.filter-mode", "block");
        setIfAbsent(config, "chat-filter.censor-char", "*");
    }

    // =========================================================================
    //  v5 -> v6: Mentions, chat radius, item display, chat logging, API
    // =========================================================================
    private void migrateToV6(FileConfiguration config) {
        plugin.getLogger().info("  Running v5 -> v6 migration...");

        // --- Add mentions section ---
        if (!config.contains("mentions")) {
            config.set("mentions.enabled", false);

            config.set("mentions.player.enabled", true);
            config.set("mentions.player.highlight-color", "&e");
            config.set("mentions.player.sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
            config.set("mentions.player.sound.volume", 1.0);
            config.set("mentions.player.sound.pitch", 1.0);
            config.set("mentions.player.actionbar.enabled", true);
            config.set("mentions.player.actionbar.message", "&e%sender% mentioned you!");

            config.set("mentions.role.enabled", true);
            config.set("mentions.role.highlight-color", "&b");
            config.set("mentions.role.sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
            config.set("mentions.role.sound.volume", 1.0);
            config.set("mentions.role.sound.pitch", 1.2);
            config.set("mentions.role.actionbar.enabled", true);
            config.set("mentions.role.actionbar.message", "&b%sender% mentioned your role!");

            config.set("mentions.everyone.enabled", true);
            config.set("mentions.everyone.highlight-color", "&c");
            config.set("mentions.everyone.sound.name", "BLOCK_NOTE_BLOCK_PLING");
            config.set("mentions.everyone.sound.volume", 1.0);
            config.set("mentions.everyone.sound.pitch", 1.0);
            config.set("mentions.everyone.actionbar.enabled", true);
            config.set("mentions.everyone.actionbar.message", "&c%sender% mentioned @everyone!");

            config.set("mentions.here.enabled", true);
            config.set("mentions.here.highlight-color", "&c");
            config.set("mentions.here.sound.name", "BLOCK_NOTE_BLOCK_PLING");
            config.set("mentions.here.sound.volume", 1.0);
            config.set("mentions.here.sound.pitch", 0.8);
            config.set("mentions.here.actionbar.enabled", true);
            config.set("mentions.here.actionbar.message", "&c%sender% mentioned @here!");
            plugin.getLogger().info("    Added mentions section.");
        }

        // --- Add chat-radius section ---
        if (!config.contains("chat-radius")) {
            config.set("chat-radius.enabled", false);
            config.set("chat-radius.default-radius", 100);
            config.set("chat-radius.cross-world", false);
            config.set("chat-radius.shout-prefix", "!");
            config.set("chat-radius.shout-format", "&c[SHOUT] &r");
            config.set("chat-radius.per-group.enabled", false);
            config.set("chat-radius.per-group.groups.owner", -1);
            config.set("chat-radius.per-group.groups.admin", -1);
            config.set("chat-radius.per-group.groups.moderator", 500);
            config.set("chat-radius.per-group.groups.default", 100);
            plugin.getLogger().info("    Added chat-radius section.");
        }

        // --- Add item-display section ---
        if (!config.contains("item-display")) {
            config.set("item-display.enabled", false);
            config.set("item-display.snapshot-ttl-seconds", 300);
            config.set("item-display.expired-message", "&cThis inventory snapshot has expired.");
            plugin.getLogger().info("    Added item-display section.");
        }

        // --- Add chat-logging section ---
        if (!config.contains("chat-logging")) {
            config.set("chat-logging.enabled", false);
            config.set("chat-logging.storage-type", "sqlite");
            config.set("chat-logging.sqlite.file", "chat-logs.db");
            config.set("chat-logging.mysql.host", "localhost");
            config.set("chat-logging.mysql.port", 3306);
            config.set("chat-logging.mysql.database", "awesomechat");
            config.set("chat-logging.mysql.username", "root");
            config.set("chat-logging.mysql.password", "");
            config.set("chat-logging.page-size", 10);
            config.set("chat-logging.log-format", "&7{timestamp} &8- &f{player}&7: &f{message}");
            config.set("chat-logging.log-filtered", true);
            config.set("chat-logging.log-commands", false);
            plugin.getLogger().info("    Added chat-logging section.");
        }

        // --- Add API section ---
        if (!config.contains("api")) {
            config.set("api.enabled", true);
            config.set("api.moderator-permission", "awesomechat.moderator");
            plugin.getLogger().info("    Added API section.");
        }
    }

    // =========================================================================
    //  v6 -> v7: Chat color picker
    // =========================================================================
    private void migrateToV7(FileConfiguration config) {
        plugin.getLogger().info("  Running v6 -> v7 migration...");

        if (!config.contains("chatcolor")) {
            config.set("chatcolor.enabled", true);
            plugin.getLogger().info("    Added chatcolor section.");
        }
    }

    // =========================================================================
    //  v7 -> v8: Item display configurable formats, hover text, GUI titles
    // =========================================================================
    private void migrateToV8(FileConfiguration config) {
        plugin.getLogger().info("  Running v7 -> v8 migration...");

        setIfAbsent(config, "item-display.formats.item", "&f[{item}&f x{count}&f]");
        setIfAbsent(config, "item-display.formats.empty-hand", "&7[Empty Hand]");
        setIfAbsent(config, "item-display.formats.inventory", "&a[{player}'s Inventory]");
        setIfAbsent(config, "item-display.formats.enderchest", "&5[{player}'s Ender Chest]");
        setIfAbsent(config, "item-display.formats.command", "&6[{command}]");

        setIfAbsent(config, "item-display.hover-text.inventory", "&eClick to view inventory");
        setIfAbsent(config, "item-display.hover-text.enderchest", "&eClick to view ender chest");
        setIfAbsent(config, "item-display.hover-text.command", "&eClick to use {command}");

        setIfAbsent(config, "item-display.gui-titles.inventory", "{player}'s Inventory");
        setIfAbsent(config, "item-display.gui-titles.enderchest", "{player}'s Ender Chest");

        plugin.getLogger().info("    Added item-display format, hover-text, and gui-titles settings.");
    }

    // =========================================================================
    //  v8 -> v9: Chat color custom-gradients section
    // =========================================================================
    private void migrateToV9(FileConfiguration config) {
        plugin.getLogger().info("  Running v8 -> v9 migration...");

        if (!config.contains("chatcolor.custom-gradients")) {
            config.createSection("chatcolor.custom-gradients");
            plugin.getLogger().info("    Added chatcolor.custom-gradients section.");
        }
    }

    // =========================================================================
    //  v9 -> v10: Item display trigger prefix/suffix, item GUI title
    // =========================================================================
    private void migrateToV10(FileConfiguration config) {
        plugin.getLogger().info("  Running v9 -> v10 migration...");

        setIfAbsent(config, "item-display.trigger-prefix", "[");
        setIfAbsent(config, "item-display.trigger-suffix", "]");
        setIfAbsent(config, "item-display.gui-titles.item", "{player}'s Item");

        plugin.getLogger().info("    Added item-display trigger prefix/suffix and item GUI title.");
    }

    // =========================================================================
    //  v10-14 -> v15: Extract chat-filter to filter.yml
    // =========================================================================
    private void migrateToV15(FileConfiguration config) {
        plugin.getLogger().info("  Running v10-14 -> v15 migration (extracting chat-filter to modules/filter.yml)...");

        File modulesDir = new File(plugin.getDataFolder(), "modules");
        if (!modulesDir.exists()) modulesDir.mkdirs();
        File filterFile = new File(modulesDir, "filter.yml");

        // Only migrate if chat-filter section exists in config.yml
        ConfigurationSection chatFilter = config.getConfigurationSection("chat-filter");
        if (chatFilter != null && !filterFile.exists()) {
            // Build filter.yml from the existing chat-filter section
            YamlConfiguration filterConfig = new YamlConfiguration();

            filterConfig.set("enabled", chatFilter.getBoolean("enabled", true));
            filterConfig.set("prefix", chatFilter.getString("prefix", "&8[&cFilter&8]&r "));

            // Map old filter-mode to new mode key
            filterConfig.set("mode", chatFilter.getString("filter-mode", "block"));
            filterConfig.set("censor-char", chatFilter.getString("censor-char", "*"));
            filterConfig.set("bypass-permission", chatFilter.getBoolean("bypass-permission", true));
            filterConfig.set("filter-commands", chatFilter.getBoolean("filter-commands", false));
            filterConfig.set("announce-actions", chatFilter.getBoolean("announce-actions", false));

            // Cooldown
            ConfigurationSection cooldown = chatFilter.getConfigurationSection("cooldown");
            if (cooldown != null) {
                filterConfig.set("cooldown.enabled", cooldown.getBoolean("enabled", true));
                filterConfig.set("cooldown.time-ms", cooldown.getLong("time-ms", 2000L));
                filterConfig.set("cooldown.commands", cooldown.getStringList("commands"));
                filterConfig.set("cooldown.command-whitelist", cooldown.getStringList("command-whitelist"));
            }

            // Spam
            ConfigurationSection spam = chatFilter.getConfigurationSection("spam");
            if (spam != null) {
                filterConfig.set("spam.enabled", spam.getBoolean("enabled", true));
                filterConfig.set("spam.limit", spam.getInt("limit", 3));
                filterConfig.set("spam.arg-sensitive", spam.getBoolean("arg-sensitive", false));
                filterConfig.set("spam.commands", spam.getStringList("commands"));
                filterConfig.set("spam.command-whitelist", spam.getStringList("command-whitelist"));
            }

            // Similarity
            ConfigurationSection similarity = chatFilter.getConfigurationSection("similarity");
            if (similarity != null) {
                filterConfig.set("similarity.enabled", similarity.getBoolean("enabled", true));
                filterConfig.set("similarity.threshold", similarity.getDouble("threshold", 0.85));
                filterConfig.set("similarity.commands", similarity.getStringList("commands"));
                // Handle old key name
                List<String> simWhitelist = similarity.getStringList("command-whitelist");
                if (simWhitelist.isEmpty()) {
                    simWhitelist = similarity.getStringList("command-similarity-whitelist");
                }
                filterConfig.set("similarity.command-whitelist", simWhitelist);
            }

            // Banned words
            Object bannedObj = chatFilter.get("banned-words");
            if (bannedObj != null) {
                filterConfig.set("banned-words", bannedObj);
            }

            // Anti-advertising
            ConfigurationSection anti = chatFilter.getConfigurationSection("anti-advertising");
            if (anti != null) {
                filterConfig.set("anti-advertising.enabled", anti.getBoolean("enabled", true));
                filterConfig.set("anti-advertising.player-message", anti.getString("player-message", "&cAdvertising is not allowed."));
                filterConfig.set("anti-advertising.staff-message", anti.getString("staff-message", "&c{player} attempted to advertise: &7{message}"));
                filterConfig.set("anti-advertising.tlds", anti.getStringList("tlds"));
                filterConfig.set("anti-advertising.block-phrases", anti.getStringList("block-phrases"));
                if (anti.isConfigurationSection("punishments")) {
                    for (String key : anti.getConfigurationSection("punishments").getKeys(false)) {
                        filterConfig.set("anti-advertising.punishments." + key, anti.get("punishments." + key));
                    }
                }
            }

            // Global punishments
            ConfigurationSection punishments = chatFilter.getConfigurationSection("punishments");
            if (punishments != null) {
                for (String key : punishments.getKeys(false)) {
                    filterConfig.set("punishments." + key, punishments.get(key));
                }
            }

            // Messages
            filterConfig.set("player-message", chatFilter.getString("player-message", "&cYou cannot send this message."));
            filterConfig.set("staff-message", chatFilter.getString("staff-message", "&c{player} triggered filter: &7{message}"));

            // Rules
            List<Map<?, ?>> rules = chatFilter.getMapList("rules");
            if (!rules.isEmpty()) {
                filterConfig.set("rules", rules);
            }

            // Logging
            ConfigurationSection logging = chatFilter.getConfigurationSection("logging");
            if (logging != null) {
                filterConfig.set("logging.enabled", logging.getBoolean("enabled", true));
                filterConfig.set("logging.file", logging.getString("file", "filter-violations.log"));
            }

            try {
                filterConfig.save(filterFile);
                plugin.getLogger().info("    Migrated chat-filter settings to modules/filter.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("    Failed to save filter.yml during migration: " + e.getMessage());
            }
        }

        // Remove chat-filter section from config.yml
        if (config.contains("chat-filter")) {
            config.set("chat-filter", null);
            plugin.getLogger().info("    Removed chat-filter section from config.yml");
        }

        plugin.getLogger().info("    Chat filter settings have moved to modules/filter.yml");
    }

    // =========================================================================
    //  v15 -> v16: Channel command declarations & Chat filter cooldown
    // =========================================================================
    private void migrateToV16(FileConfiguration config) {
        plugin.getLogger().info("  Running v15 -> v16 migration...");

        FileConfiguration filter = plugin.getFilterConfig();

        setIfAbsent(filter, "cooldown.bypass-permission", "awesomechat.filter.bypass.cooldown");
        setIfAbsent(filter, "cooldown.message", "{prefix} &7Please wait &b{time}&7s before sending another message.");
        plugin.getLogger().info("    Added cooldown message & bypass permission sections to filter manager");

        setIfAbsent(config, "channels.staff.command", "/staffchat");
        setIfAbsent(config, "channels.admin.command", "/adminchat");
        setIfAbsent(config, "channels.vip.command", "none");
        plugin.getLogger().info("    Added command section to channel configuration");
    }

    // =========================================================================
    //  v16 -> v17: Chat management Improvements
    // =========================================================================
    private void migrateToV17(FileConfiguration config) {
        plugin.getLogger().info("  Running v16 -> v17 migration...");

        setIfAbsent(config, "clearchat.announcement-self", "{prefix}&eYour chat has been cleared.");

        setIfAbsent(config, "clearchat.bypass-message", "&7&oYour chat wasn't cleared due to the bypass permission.");

        setIfAbsent(config, "clearchat.no-permission-all", "{prefix}&cYou don't have permission to clear chat for everyone.");
        setIfAbsent(config, "clearchat.no-permission-all", "{prefix}&cYou do not have permission to clear your own chat.");

    }

    /**
     * Ensures modules/filter.yml exists. Called during plugin startup.
     * Migrates from old filter.yml location if present, otherwise saves the default.
     * Also ensures the default banned-word list files exist.
     */
    public void ensureFilterConfig() {
        File modulesDir = new File(plugin.getDataFolder(), "modules");
        if (!modulesDir.exists()) modulesDir.mkdirs();

        File filterFile = new File(modulesDir, "filter.yml");

        if (!filterFile.exists()) {
            // Migrate from old location if present
            File oldFilterFile = new File(plugin.getDataFolder(), "filter.yml");
            if (oldFilterFile.exists()) {
                oldFilterFile.renameTo(filterFile);
                plugin.getLogger().info("Migrated filter.yml -> modules/filter.yml");

                // Update banned-words path if it still references the old location
                try {
                    org.bukkit.configuration.file.YamlConfiguration filterConfig =
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(filterFile);
                    String bannedPath = filterConfig.getString("banned-words", "");
                    if ("filter/".equals(bannedPath) || "filter".equals(bannedPath)) {
                        filterConfig.set("banned-words", "modules/filter/");
                        filterConfig.save(filterFile);
                        plugin.getLogger().info("Updated banned-words path to modules/filter/");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to update banned-words path: " + e.getMessage());
                }
            } else {
                plugin.saveResource("modules/filter.yml", false);
                plugin.getLogger().info("Generated default modules/filter.yml.");
            }
        }

        // Migrate old filter/ directory to modules/filter/
        File oldFilterDir = new File(plugin.getDataFolder(), "filter");
        File newFilterDir = new File(modulesDir, "filter");
        if (oldFilterDir.exists() && oldFilterDir.isDirectory()) {
            if (!newFilterDir.exists()) newFilterDir.mkdirs();
            File[] files = oldFilterDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    File dest = new File(newFilterDir, f.getName());
                    if (!dest.exists()) {
                        f.renameTo(dest);
                    }
                }
            }
            // Remove old directory if empty
            String[] remaining = oldFilterDir.list();
            if (remaining != null && remaining.length == 0) {
                oldFilterDir.delete();
            }
            plugin.getLogger().info("Migrated filter/ -> modules/filter/");
        }

        // Save default banned-word list files from jar if they don't exist
        saveDefaultFilterFiles(newFilterDir);
    }

    private void saveDefaultFilterFiles(File filterDir) {
        String[] defaultFiles = {
                "modules/filter/curse_words.txt",
                "modules/filter/sexual.txt",
                "modules/filter/slurs.txt",
                "modules/filter/abuse_violence.txt"
        };
        for (String resourcePath : defaultFiles) {
            File target = new File(plugin.getDataFolder(), resourcePath);
            if (!target.exists()) {
                try {
                    plugin.saveResource(resourcePath, false);
                } catch (Exception e) {
                    // Resource may not exist in jar; will be created by ChatFilterManager fallback
                }
            }
        }
    }

    // =========================================================================
    //  Utility
    // =========================================================================
    private void setIfAbsent(FileConfiguration config, String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }
}
