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

import dev.adf.awesomeChat.api.events.ChatFilterViolationEvent;
import dev.adf.awesomeChat.logging.FilterLogger;
import dev.adf.awesomeChat.storage.ViolationStorage;

public class ChatFilterManager {

    private final JavaPlugin plugin;
    private File dataFile;

    private final ConfigurationSection filterSection;
    private final boolean bypassEnabled;
    private final boolean announceActions;

    private final List<String> bannedWords;
    private final boolean antiAdvertisingEnabled;
    private final List<String> tlds;
    private final List<String> antiAdPhrases;

    private final List<Pattern> wildcardBannedPatterns = new ArrayList<>();
    private final List<String> wildcardBannedRaw = new ArrayList<>();

    private final Map<String, FilterRule> rules = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> offensesCache = new HashMap<>();

    private boolean enabled = true;

    public ChatFilterManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // ensure data folder exists
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        // data file
        this.dataFile = new File(plugin.getDataFolder(), "data/uuid.json");

        // ensure parent folder exists
        if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();

        // create the file if it does not exist
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

        if (filterSection == null || !filterSection.getBoolean("enabled", true)) {
            plugin.getLogger().info("Chat filter is disabled in config.yml — ChatFilterManager will not initialize.");
            this.enabled = false;
            this.dataFile = null;
            this.filterSection = null;
            this.bypassEnabled = false;
            this.announceActions = false;
            this.bannedWords = Collections.emptyList();
            this.antiAdvertisingEnabled = false;
            this.tlds = Collections.emptyList();
            this.antiAdPhrases = Collections.emptyList();
            return;
        }

        if (filterSection == null) {
            plugin.getLogger().warning("chat-filter section missing in config.yml! Using defaults.");
            ChatSettings.COOLDOWN_MS = 2000L;
            ChatSettings.SIMILARITY_THRESHOLD = 0.85;
            ChatSettings.SPAM_LIMIT = 3;
        } else {
            ChatSettings.COOLDOWN_MS = filterSection.getLong("cooldown.time-ms", 2000L);
            ChatSettings.SIMILARITY_THRESHOLD = filterSection.getDouble("similarity.threshold", 0.85);
            ChatSettings.SPAM_LIMIT = filterSection.getInt("spam.limit", 3);
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
        Object bannedObj = filterSection.get("banned-words");

        List<String> listFallback = Collections.emptyList();
        if (bannedObj instanceof List) {
            // old format still supported
            listFallback = filterSection.getStringList("banned-words");
        }

        this.bannedWords = Optional.ofNullable(listFallback)
                .orElse(Collections.emptyList())
                .stream().map(String::toLowerCase).collect(Collectors.toList());

        if (bannedObj instanceof String) {
            String path = String.valueOf(bannedObj).trim();
            if (!path.isEmpty()) {
                File target = new File(plugin.getDataFolder(), path);

                ensureDefaultWildcardFilters(target);

                loadWildcardBannedFromPath(target);
                plugin.getLogger().info("[ChatFilter] Loaded " + wildcardBannedPatterns.size()
                        + " wildcard banned patterns from: " + target.getPath());
            }
        }

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

        FileConfiguration config = plugin.getConfig();

        boolean isCommand = type.equalsIgnoreCase("command");
        String baseCommand = "";
        if (isCommand && message.startsWith("/")) {
            String[] parts = message.substring(1).split(" ");
            baseCommand = parts[0].toLowerCase();
        }

        // ================= Cooldown Check =================
        if (config.getBoolean("cooldown.enabled")) {
            boolean applyCooldown = !isCommand;
            if (isCommand) {
                List<String> commands = config.getStringList("cooldown.commands");
                List<String> whitelist = config.getStringList("cooldown.command-whitelist");
                boolean listed = commands.isEmpty() || commands.contains(baseCommand);
                boolean whitelisted = whitelist.contains(baseCommand);
                applyCooldown = listed && !whitelisted;
            }

            if (applyCooldown) {
                long cooldownTime = config.getLong("cooldown.time-ms");
                if (lastMessageTime.containsKey(uuid) && now - lastMessageTime.get(uuid) < cooldownTime) {
                    handleViolation(player, message, "cooldown", "too-fast", type);
                    return true;
                }
                lastMessageTime.put(uuid, now);
            }
        }

        // ================= Spam / Similarity Check =================
        if (config.getBoolean("spam.enabled") || config.getBoolean("similarity.enabled")) {
            boolean applyCheck = !isCommand;

            if (isCommand) {
                List<String> spamCommands = config.getStringList("spam.commands");
                List<String> similarityCommands = config.getStringList("similarity.commands");
                List<String> spamWhitelist = config.getStringList("spam.command-whitelist");
                List<String> similarityWhitelist = config.getStringList("similarity.command-similarity-whitelist");

                boolean spamListed = spamCommands.isEmpty() || spamCommands.contains(baseCommand);
                boolean simListed = similarityCommands.isEmpty() || similarityCommands.contains(baseCommand);
                boolean spamWhitelisted = spamWhitelist.contains(baseCommand);
                boolean similarityWhitelisted = similarityWhitelist.contains(baseCommand);

                boolean isSpamTarget = spamListed && !spamWhitelisted;
                boolean isSimilarityTarget = simListed && !similarityWhitelisted;

                applyCheck = isSpamTarget || isSimilarityTarget;
            }

            if (applyCheck) {
                boolean argSensitive = config.getBoolean("spam.arg-sensitive");
                String compareValue = argSensitive ? normalized : baseCommand;

                if (lastMessageContent.containsKey(uuid)) {
                    String prev = lastMessageContent.get(uuid);
                    double sim = similarity(compareValue, prev);
                    double threshold = config.getDouble("similarity.threshold");
                    int limit = config.getInt("spam.limit");

                    if (sim >= threshold) {
                        int count = spamCount.getOrDefault(uuid, 0) + 1;
                        spamCount.put(uuid, count);

                        if (count >= limit) {
                            handleViolation(player, message, "spam", "similar-message", type);
                            spamCount.put(uuid, 0);
                            return true;
                        }
                    } else {
                        spamCount.put(uuid, 0);
                    }
                }
                lastMessageContent.put(uuid, compareValue);
            }
        }

        // ================= Banned Words =================
        for (String bw : bannedWords) {
            Pattern p = Pattern.compile("(?i)(^|\\W)" + Pattern.quote(bw) + "($|\\W)");
            if (p.matcher(message).find()) {
                Bukkit.getLogger().info("Profanity matched: word='" + bw + "' in message='" + message + "'");
                handleBannedWord(player, message, bw, type);
                return true;
            }
        }

        // ================= Anti-Advertising =================
        if (antiAdvertisingEnabled) {
            for (String phrase : antiAdPhrases) {
                if (normalized.contains(phrase.toLowerCase())) {
                    handleAdvertising(player, message, phrase, type);
                    return true;
                }
            }

            for (String tld : tlds) {
                Pattern p = Pattern.compile("\\b[a-zA-Z0-9-]+\\." + Pattern.quote(tld) + "\\b", Pattern.CASE_INSENSITIVE);
                if (p.matcher(message).find()) {
                    handleAdvertising(player, message, "." + tld, type);
                    return true;
                }
            }

            Pattern domain = Pattern.compile("([a-zA-Z0-9-]+\\.[a-z]{2,})(:[0-9]{1,5})?", Pattern.CASE_INSENSITIVE);
            if (domain.matcher(message).find()) {
                handleAdvertising(player, message, "domain", type);
                return true;
            }
        }

        // ================= Regex Rules =================
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

        ChatFilterViolationEvent event = new ChatFilterViolationEvent(player, message, rule.name, count);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

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

    private void ensureDefaultWildcardFilters(File target) {
        try {
            File dir = target.isDirectory() ? target : target.getParentFile();
            if (dir == null) return;

            if (!dir.exists()) dir.mkdirs();

            File curse = new File(dir, "curse_words.txt");
            File sexual = new File(dir, "sexual.txt");
            File slurs = new File(dir, "slurs.txt");
            File abuse = new File(dir, "abuse_violence.txt");

            if (!curse.exists() && target.isDirectory()) {
                writeTextFile(curse, String.join("\n",
                        "# Curse / general profanity",
                        "fuck*",
                        "fuk*",
                        "shit*",
                        "bullshit",
                        "bitch*",
                        "bastard*",
                        "asshole*",
                        "dick*",
                        "cock*",
                        "twat*",
                        "prick*",
                        "dipshit",
                        "jackass",
                        "dumbass",
                        "motherfucker*",
                        "sonofabitch",
                        ""
                ));
            }

            if (!sexual.exists() && target.isDirectory()) {
                writeTextFile(sexual, String.join("\n",
                        "# Sexual / anatomy / explicit",
                        "pussy*",
                        "cunt*",
                        "slut*",
                        "whore*",
                        "hoe",
                        "skank*",
                        "cum",
                        "semen",
                        "penis",
                        "vagina",
                        "boobs",
                        "tits",
                        "nipples",
                        "dildo",
                        "anal",
                        "sex",
                        "porn*",
                        "blowjob",
                        "handjob",
                        "boner*",
                        "jerkoff",
                        "masturbate*",
                        "balls",
                        "nuts",
                        ""
                ));
            }

            if (!slurs.exists() && target.isDirectory()) {
                writeTextFile(slurs, String.join("\n",
                        "# Slurs / hate language",
                        "fag*",
                        "faggot*",
                        "retard*",
                        "nigger*",
                        "nigga*",
                        "chink*",
                        "spic*",
                        "kike*",
                        "dyke*",
                        "tranny*",
                        "queer",
                        ""
                ));
            }

            if (!abuse.exists() && target.isDirectory()) {
                writeTextFile(abuse, String.join("\n",
                        "# Sexual violence / abuse terms",
                        "rape*",
                        "rapist*",
                        "molest*",
                        "incest",
                        ""
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ChatFilter] Failed to ensure default wildcard filters: " + e.getMessage());
        }
    }

    private void writeTextFile(File file, String content) throws IOException {
        if (file.exists()) return; // don't overwrite
        if (file.getParentFile() != null && !file.getParentFile().exists()) file.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        }
    }

    private void loadWildcardBannedFromPath(File target) {
        wildcardBannedPatterns.clear();
        wildcardBannedRaw.clear();

        if (!target.exists()) {
            plugin.getLogger().warning("[ChatFilter] Wildcard banned path does not exist: " + target.getPath());
            return;
        }

        List<File> files = new ArrayList<>();
        if (target.isDirectory()) {
            collectTxtFilesRecursive(target, files);
        } else {
            if (target.getName().toLowerCase().endsWith(".txt")) files.add(target);
        }

        for (File f : files) {
            try (Scanner sc = new Scanner(f, "UTF-8")) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine().trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("#")) continue;

                    wildcardBannedRaw.add(line);
                    wildcardBannedPatterns.add(wildcardToPattern(line));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[ChatFilter] Failed reading " + f.getPath() + ": " + e.getMessage());
            }
        }
    }

    private void collectTxtFilesRecursive(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;

        for (File f : kids) {
            if (f.isDirectory()) {
                collectTxtFilesRecursive(f, out);
            } else if (f.getName().toLowerCase().endsWith(".txt")) {
                out.add(f);
            }
        }
    }

    /**
     * Convert wildcard pattern to a safe regex pattern.
     */
    private Pattern wildcardToPattern(String wildcard) {
        String w = wildcard.trim();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else {
                if ("\\.^$|?+()[]{}".indexOf(c) >= 0) sb.append("\\");
                sb.append(c);
            }
        }

        return Pattern.compile("^" + sb + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * Tokenize message into "words" in such a way that avoids blocking inside other words unless wildcard requires it.
     */
    private List<String> tokenizeWords(String message) {
        String[] parts = message.split("[\\s\\p{Punct}]+");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
