package dev.adf.awesomeChat.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.adf.awesomeChat.logging.FilterLogger;
import dev.adf.awesomeChat.storage.ViolationStorage;

public class ChatFilterManager {

    private final JavaPlugin plugin;
    private final File dataFile;

    private final ConfigurationSection filterSection;
    private final boolean bypassEnabled;
    private final boolean announceActions;

    private final List<String> bannedWords;
    private final boolean antiAdvertisingEnabled;
    private final List<String> tlds;
    private final List<String> antiAdPhrases;

    private final Map<String, FilterRule> rules = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> offensesCache = new HashMap<>();

    public ChatFilterManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // ensure data folder exists
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        // data file (user requested "data/uuid.json")
        // Use JSON file for storing offenses
        this.dataFile = new File(plugin.getDataFolder(), "data/uuid.json");

        // Ensure parent folder exists
        if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();

        // Create the file if it doesn't exist
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                // Initialize with an empty JSON object to avoid parsing errors
                try (FileWriter writer = new FileWriter(dataFile)) {
                    writer.write("{}");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data/uuid.json: " + e.getMessage());
            }
        }

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection filterSection = config.getConfigurationSection("chat-filter");

        if (filterSection == null) {
            plugin.getLogger().warning("chat-filter section missing in config.yml! Using defaults.");
            ChatSettings.COOLDOWN_MS = 2000L;
            ChatSettings.SIMILARITY_THRESHOLD = 0.85;
            ChatSettings.SPAM_LIMIT = 3;
        } else {
            ChatSettings.COOLDOWN_MS = filterSection.getLong("cooldown-ms", 2000L);
            ChatSettings.SIMILARITY_THRESHOLD = filterSection.getDouble("similarity-threshold", 0.85);
            ChatSettings.SPAM_LIMIT = filterSection.getInt("spam-limit", 3);
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
            Map<String, Map<String, Integer>> snapshot = gson.fromJson(reader, type);
            if (snapshot != null) {
                for (Map.Entry<String, Map<String, Integer>> e : snapshot.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(e.getKey());
                        offensesCache.put(uuid, e.getValue());
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Invalid UUID in offenses file: " + e.getKey());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // config section
        FileConfiguration cfg = plugin.getConfig();
        this.filterSection = cfg.getConfigurationSection("chat-filter");
        if (filterSection == null) {
            plugin.getLogger().warning("chat-filter section is missing in config.");
            throw new IllegalStateException("chat-filter missing");
        }

        this.bypassEnabled = filterSection.getBoolean("bypass-permission", true);
        this.announceActions = filterSection.getBoolean("announce-actions", false);

        // banned words
        this.bannedWords = Optional.ofNullable(filterSection.getStringList("banned-words"))
                .orElse(Collections.emptyList())
                .stream().map(String::toLowerCase).collect(Collectors.toList());

        // anti-advertising
        ConfigurationSection anti = filterSection.getConfigurationSection("anti-advertising");
        this.antiAdvertisingEnabled = anti != null && anti.getBoolean("enabled", true);
        this.tlds = anti != null ? Optional.ofNullable(anti.getStringList("tlds")).orElse(Collections.emptyList()) : Collections.emptyList();
        this.antiAdPhrases = anti != null ? Optional.ofNullable(anti.getStringList("block-phrases")).orElse(Collections.emptyList()) : Collections.emptyList();

        // load rules
        List<Map<?,?>> rawRules = filterSection.getMapList("rules");
        for (Map<?,?> raw : rawRules) {
            String name = String.valueOf(raw.get("name"));
            String regex = raw.containsKey("regex") ? String.valueOf(raw.get("regex")) : null;
            String playerMsg = raw.containsKey("player-message") ? String.valueOf(raw.get("player-message")) : "";
            String staffMsg = raw.containsKey("staff-message") ? String.valueOf(raw.get("staff-message")) : "";
            Map<String, Object> punish = raw.containsKey("punishments") ? (Map<String, Object>) raw.get("punishments") : Collections.emptyMap();
            FilterRule fr = new FilterRule(name, regex, playerMsg, staffMsg, punish);
            rules.put(name, fr);
        }

        // load offenses into memory from file
        loadOffensesFromFile();
    }

    private static final File DATA_FOLDER = new File("plugins/AwesomeChat/data");
    private static final Gson GSON = new Gson();

    private void loadOffensesFromFile() {
        offensesCache.clear();

        if (!dataFile.exists()) return;

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
            Map<String, Map<String, Integer>> snapshot = GSON.fromJson(reader, type);

            if (snapshot != null) {
                for (Map.Entry<String, Map<String, Integer>> entry : snapshot.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        offensesCache.put(uuid, entry.getValue() != null ? entry.getValue() : new HashMap<>());
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Invalid UUID in offenses file: " + entry.getKey());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveOffensesToFileAsync() {
        Map<String, Map<String, Integer>> snapshot = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Integer>> e : offensesCache.entrySet()) {
            snapshot.put(e.getKey().toString(), new HashMap<>(e.getValue()));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileWriter writer = new FileWriter(dataFile)) {
                    GSON.toJson(snapshot, writer);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save offenses: " + e.getMessage());
                }
            }
        }.runTask(plugin);
    }

    /**
     * Main entry used by ChatListener. Returns true if the chat should be cancelled (violation) and nothing else should run.
     */
    // Cooldown and spam cache
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessageContent = new HashMap<>();
    private final Map<UUID, Integer> spamCount = new HashMap<>();

    public class ChatSettings {
        public static long COOLDOWN_MS;
        public static double SIMILARITY_THRESHOLD;
        public static int SPAM_LIMIT;
    }

    public boolean checkAndHandle(Player player, String message, String type) {
        if (bypassEnabled && player.hasPermission("awesomechat.filter.bypass")) return false;

        UUID uuid = player.getUniqueId();
        String normalized = message.toLowerCase();
        long now = System.currentTimeMillis();

        // cooldown check
        if (lastMessageTime.containsKey(uuid) && now - lastMessageTime.get(uuid) < ChatSettings.COOLDOWN_MS) {
            if (!type.equals("message")) {
                return true;
            }
            handleViolation(player, message, "cooldown", "too-fast", type);
            return true;
        }
        lastMessageTime.put(uuid, now);

        // 0.5) Spam / similarity check
        if (lastMessageContent.containsKey(uuid)) {
            if (!type.equals("message")) {
                return true;
            }
            String prev = lastMessageContent.get(uuid);
            double sim = similarity(normalized, prev);
            if (sim >= ChatSettings.SIMILARITY_THRESHOLD) {
                int count = spamCount.getOrDefault(uuid, 0) + 1;
                spamCount.put(uuid, count);
                if (count >= ChatSettings.SPAM_LIMIT) {
                    handleViolation(player, message, "spam", "similar-message", type);
                    spamCount.put(uuid, 0);
                    return true;
                }
            } else {
                spamCount.put(uuid, 0);
            }
        }
        lastMessageContent.put(uuid, normalized);

        // Banned words
        for (String bw : bannedWords) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(bw) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(message).find()) {
                handleBannedWord(player, message, bw, type);
                return true;
            }
        }

        // Anti-advertising
        if (antiAdvertisingEnabled) {
            // Blocked phrases
            for (String phrase : antiAdPhrases) {
                if (normalized.contains(phrase.toLowerCase())) {
                    handleAdvertising(player, message, phrase, type);
                    return true;
                }
            }

            // Explicit TLDs
            for (String tld : tlds) {
                Pattern p = Pattern.compile("\\b[a-zA-Z0-9-]+\\." + Pattern.quote(tld) + "\\b", Pattern.CASE_INSENSITIVE);
                if (p.matcher(message).find()) {
                    handleAdvertising(player, message, "." + tld, type);
                    return true;
                }
            }

            // Generic domain fallback
            Pattern domain = Pattern.compile("([a-zA-Z0-9-]+\\.[a-z]{2,})(:[0-9]{1,5})?", Pattern.CASE_INSENSITIVE);
            if (domain.matcher(message).find()) {
                handleAdvertising(player, message, "domain", type);
                return true;
            }
        }

        // regex
        for (FilterRule rule : rules.values()) {
            if (rule.regex == null || rule.regex.isEmpty()) continue;
            try {
                Pattern p = Pattern.compile(rule.regex, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(message);
                if (m.find()) {
                    handleViolation(player, message, rule.name, m.group(), type);
                    return true;
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid regex for rule " + rule.name + ": " + rule.regex);
            }
        }

        return false; // clean
    }

    /**
     * Jaro-Winkler similarity for spam detection
     */
    private double similarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int maxDist = Math.max(s1.length(), s2.length()) / 2 - 1;
        int[] matches = new int[2];
        int m = 0, t = 0;

        boolean[] s1Matches = new boolean[s1.length()];
        boolean[] s2Matches = new boolean[s2.length()];

        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - maxDist);
            int end = Math.min(i + maxDist + 1, s2.length());

            for (int j = start; j < end; j++) {
                if (s2Matches[j]) continue;
                if (s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                m++;
                break;
            }
        }
        if (m == 0) return 0.0;

        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) t++;
            k++;
        }
        double jaro = ((m / (double) s1.length()) + (m / (double) s2.length()) + ((m - t / 2.0) / m)) / 3.0;

        // Winkler adjustment
        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(s1.length(), s2.length())); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }
        return jaro + 0.1 * prefix * (1 - jaro);
    }

    private void handleViolation(Player player, String message, String ruleName, String matched, String type) {
        UUID id = player.getUniqueId();

        Map<String, Integer> userMap = offensesCache.computeIfAbsent(id, k -> new HashMap<>());
        int count = userMap.getOrDefault(ruleName, 0) + 1;
        userMap.put(ruleName, count);
        saveOffensesToFileAsync();

        FilterRule rule = rules.get(ruleName);
        if (rule == null) {
            rule = new FilterRule(
                    ruleName,
                    null,
                    "&cYour " + type + " was blocked.",
                    "&c{player} triggered " + ruleName + ": {message}",
                    Collections.emptyMap()
            );
        }

        ViolationStorage.addViolation(id, rule.name);
        FilterLogger.log(player.getName() + " violated rule " + rule.name + " with message: " + message);

        String prefix = plugin.getConfig().getString("chat-filter.prefix", "&8[&cChatFilter&8]&r ");
        String playerMessage = replacePlaceholders(prefix + rule.playerMessage, player, message);
        String staffMessage = replacePlaceholders(prefix + rule.staffMessage, player, message);

        // determine action based on config
        String action = null;
        if (rule.punishments != null && !rule.punishments.isEmpty()) {
            Object actionObj = rule.punishments.get(count);
            if (actionObj == null) {
                actionObj = rule.punishments.get(String.valueOf(count));
            }
            plugin.getLogger().info("Action object retrieved: " + actionObj);
            if (actionObj != null) action = String.valueOf(actionObj);

            // Fallback
            if (action == null) {
                int best = -1;
                boolean hasRepeat = false;

                for (Object keyObj : rule.punishments.keySet()) {
                    if ("repeat".equalsIgnoreCase(String.valueOf(keyObj))) {
                        hasRepeat = true;
                        continue;
                    }

                    int keyNum = -1;
                    if (keyObj instanceof Number) {
                        keyNum = ((Number) keyObj).intValue();
                    } else {
                        try {
                            keyNum = Integer.parseInt(String.valueOf(keyObj));
                        } catch (NumberFormatException ignored) {}
                    }
                    if (keyNum <= count && keyNum > best) {
                        best = keyNum;
                    }
                }
                if (best != -1) {
                    Object bestObj = rule.punishments.get(best);
                    if (bestObj == null) bestObj = rule.punishments.get(String.valueOf(best));
                    if (bestObj != null) action = String.valueOf(bestObj);
                }
                else if (hasRepeat) {
                    boolean hasFutureNonRepeat = false;
                    for (Object keyObj : rule.punishments.keySet()) {
                        if ("repeat".equalsIgnoreCase(String.valueOf(keyObj))) continue;
                        int keyNum = -1;
                        if (keyObj instanceof Number) {
                            keyNum = ((Number) keyObj).intValue();
                        } else {
                            try {
                                keyNum = Integer.parseInt(String.valueOf(keyObj));
                            } catch (NumberFormatException ignored) {}
                        }

                        if (keyNum > count) {
                            hasFutureNonRepeat = true;
                            break;
                        }
                    }

                    if (!hasFutureNonRepeat) {
                        Object repeatObj = rule.punishments.get("repeat");
                        if (repeatObj != null) action = String.valueOf(repeatObj);
                    }
                }
            }
        }

        final String finalAction = action;
        final String finalPlayerMessage = playerMessage;
        final String finalStaffMessage = staffMessage;

        // execute messages and commands asynchronousyl on the main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalPlayerMessage != null && !finalPlayerMessage.isEmpty()) {
                    player.sendMessage(ChatColorTranslate(finalPlayerMessage));
                }

                String notifyPerm = "awesomechat.filter.notify";
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission(notifyPerm))
                        .forEach(p -> p.sendMessage(ChatColorTranslate(finalStaffMessage)));

                if (finalAction != null && !finalAction.trim().isEmpty()) {
                    String consoleCommand = finalAction
                            .replace("{player}", player.getName())
                            .replace("{uuid}", player.getUniqueId().toString())
                            .replace("{message}", message)
                            .replace("{matched}", matched);

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCommand);
                }
            }
        }.runTask(plugin);
    }

    private void handleBannedWord(Player player, String message, String matchedWord, String type) {
        // Treat banned-word like a FilterRule
        String ruleName = "banned-word";
        FilterRule rule = rules.get(ruleName);

        // Fallback rule if none exists
        if (rule == null) {
            // Pull punishments from config
            Map<String, Object> punishments = Collections.emptyMap();
            if (plugin.getConfig().isConfigurationSection("chat-filter.punishments")) {
                punishments = new HashMap<>(plugin.getConfig()
                        .getConfigurationSection("chat-filter.punishments")
                        .getValues(false));
            }

            rule = new FilterRule(
                    ruleName,
                    null,
                    "&cPlease do not use profanity!",
                    "&c{player} used profanity in chat: &7{message}",
                    punishments
            );
        }

        handleViolation(player, message, rule.name, matchedWord, type);
    }

    private void handleAdvertising(Player player, String message, String matched, String type) {
        String ruleName = "advertising";

        // Fetch the rule from the config if it exists
        FilterRule rule = rules.get(ruleName);

        // Fallback rule if none exists
        if (rule == null) {
            ConfigurationSection antiAdSection = filterSection.getConfigurationSection("anti-advertising");
            Map<String, Object> punishmentsMap = Collections.emptyMap();
            String playerMsg = "&cAdvertising is not allowed.";
            String staffMsg = "&c{player} attempted to advertise in chat: {message}";

            if (antiAdSection != null) {
                punishmentsMap = antiAdSection.getConfigurationSection("punishments") != null
                        ? new HashMap<>(antiAdSection.getConfigurationSection("punishments").getValues(false))
                        : Collections.emptyMap();
                playerMsg = antiAdSection.getString("player-message", playerMsg);
                staffMsg = antiAdSection.getString("staff-message", staffMsg);
            }

            rule = new FilterRule(ruleName, null, playerMsg, staffMsg, punishmentsMap);
        }
        handleViolation(player, message, rule.name, matched, type);
    }

    private String replacePlaceholders(String template, Player player, String message) {
        if (template == null) return "";
        return template
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{message}", message);
    }

    private String ChatColorTranslate(String input) {
        // no import collision: use org.bukkit.ChatColor here
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    // Basic rule holder
    private static class FilterRule {
        final String name;
        final String regex;
        final String playerMessage;
        final String staffMessage;
        final Map<String, Object> punishments;

        FilterRule(String name, String regex, String playerMessage, String staffMessage, Map<String, Object> punishments) {
            this.name = name;
            this.regex = regex;
            this.playerMessage = playerMessage != null ? playerMessage : "";
            this.staffMessage = staffMessage != null ? staffMessage : "";
            this.punishments = punishments != null ? punishments : Collections.emptyMap();
        }
    }
}
