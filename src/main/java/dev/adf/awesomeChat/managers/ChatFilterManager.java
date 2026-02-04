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

    // Settings loaded from filter.yml
    private boolean enabled;
    private String prefix;
    private String filterMode;
    private String censorChar;
    private boolean bypassEnabled;
    private boolean filterCommands;
    private boolean announceActions;

    // Cooldown settings
    private boolean cooldownEnabled;
    private String cooldownMessage;
    private String cooldownBypassPermission;
    private long cooldownMs;
    private List<String> cooldownCommands;
    private List<String> cooldownWhitelist;

    // Spam settings
    private boolean spamEnabled;
    private int spamLimit;
    private boolean spamArgSensitive;
    private List<String> spamCommands;
    private List<String> spamWhitelist;

    // Similarity settings
    private boolean similarityEnabled;
    private double similarityThreshold;
    private List<String> similarityCommands;
    private List<String> similarityWhitelist;

    // Banned words
    private List<String> bannedWords = Collections.emptyList();
    private final List<Pattern> wildcardBannedPatterns = new ArrayList<>();
    private final List<String> wildcardBannedRaw = new ArrayList<>();

    // Anti-advertising
    private boolean antiAdvertisingEnabled;
    private String antiAdPlayerMessage;
    private String antiAdStaffMessage;
    private List<String> tlds;
    private List<String> antiAdPhrases;
    private Map<String, Object> antiAdPunishments;

    // Global punishments & messages
    private Map<String, Object> globalPunishments;
    private String playerMessage;
    private String staffMessage;

    // Rules
    private final Map<String, FilterRule> rules = new LinkedHashMap<>();

    // Offenses
    private final Map<UUID, Map<String, Integer>> offensesCache = new HashMap<>();

    // Cooldown and spam runtime caches
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessageContent = new HashMap<>();
    private final Map<UUID, Integer> spamCount = new HashMap<>();

    private static final Gson GSON = new Gson();

    public ChatFilterManager(JavaPlugin plugin, FileConfiguration filterConfig) {
        this.plugin = plugin;

        // ensure data folder exists
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        // data file for offense counts
        this.dataFile = new File(plugin.getDataFolder(), "data/uuid.json");
        if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                try (FileWriter writer = new FileWriter(dataFile)) {
                    writer.write("{}");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data/uuid.json: " + e.getMessage());
            }
        }

        loadFromConfig(filterConfig);
        loadOffensesFromFile();
    }

    /**
     * Reload the filter manager with a fresh filter config.
     */
    public void reload(FileConfiguration filterConfig) {
        loadFromConfig(filterConfig);
        // Clear runtime caches on reload
        lastMessageTime.clear();
        lastMessageContent.clear();
        spamCount.clear();
    }

    private void loadFromConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("enabled", true);

        if (!enabled) {
            plugin.getLogger().info("Chat filter is disabled in filter.yml — ChatFilterManager will not filter.");
            this.prefix = "";
            this.filterMode = "block";
            this.censorChar = "*";
            this.bypassEnabled = false;
            this.filterCommands = false;
            this.announceActions = false;
            this.cooldownEnabled = false;
            this.cooldownMessage = "";
            this.cooldownBypassPermission = "";
            this.cooldownCommands = Collections.emptyList();
            this.cooldownWhitelist = Collections.emptyList();
            this.spamEnabled = false;
            this.spamCommands = Collections.emptyList();
            this.spamWhitelist = Collections.emptyList();
            this.similarityEnabled = false;
            this.similarityCommands = Collections.emptyList();
            this.similarityWhitelist = Collections.emptyList();
            this.bannedWords = Collections.emptyList();
            this.antiAdvertisingEnabled = false;
            this.tlds = Collections.emptyList();
            this.antiAdPhrases = Collections.emptyList();
            this.antiAdPunishments = Collections.emptyMap();
            this.globalPunishments = Collections.emptyMap();
            this.playerMessage = "";
            this.staffMessage = "";
            this.antiAdPlayerMessage = "";
            this.antiAdStaffMessage = "";
            wildcardBannedPatterns.clear();
            wildcardBannedRaw.clear();
            rules.clear();
            return;
        }

        this.prefix = config.getString("prefix", "&8[&cFilter&8]&r ");
        this.filterMode = config.getString("mode", "block");
        this.censorChar = config.getString("censor-char", "*");
        this.bypassEnabled = config.getBoolean("bypass-permission", true);
        this.filterCommands = config.getBoolean("filter-commands", false);
        this.announceActions = config.getBoolean("announce-actions", false);

        this.cooldownEnabled = config.getBoolean("cooldown.enabled", true);
        this.cooldownMs = config.getLong("cooldown.time-ms", 2000L);
        List<String> cooldownCmdsList = config.getStringList("cooldown.commands");
        this.cooldownCommands = cooldownCmdsList.contains("*") ? Collections.emptyList() : cooldownCmdsList;
        this.cooldownWhitelist = config.getStringList("cooldown.command-whitelist");
        this.cooldownMessage = config.getString("cooldown.message",
                "&cPlease wait &e{time}s &cbefore sending another message.");
        this.cooldownBypassPermission = config.getString("cooldown.bypass-permission",
                "awesomechat.filter.cooldown.bypass");

        this.spamEnabled = config.getBoolean("spam.enabled", true);
        this.spamLimit = config.getInt("spam.limit", 3);
        this.spamArgSensitive = config.getBoolean("spam.arg-sensitive", false);
        List<String> spamCmdsList = config.getStringList("spam.commands");
        this.spamCommands = spamCmdsList.contains("*") ? Collections.emptyList() : spamCmdsList;
        this.spamWhitelist = config.getStringList("spam.command-whitelist");

        this.similarityEnabled = config.getBoolean("similarity.enabled", true);
        this.similarityThreshold = config.getDouble("similarity.threshold", 0.85);
        List<String> similarityCmdsList = config.getStringList("similarity.commands");
        this.similarityCommands = similarityCmdsList.contains("*") ? Collections.emptyList() : similarityCmdsList;
        this.similarityWhitelist = config.getStringList("similarity.command-whitelist");

        this.playerMessage = config.getString("player-message", "&cYou cannot send this message.");
        this.staffMessage = config.getString("staff-message", "&c{player} triggered filter: &7{message}");
        this.globalPunishments = Collections.emptyMap();
        if (config.isConfigurationSection("punishments")) {
            this.globalPunishments = new HashMap<>(config.getConfigurationSection("punishments").getValues(false));
        }

        Object bannedObj = config.get("banned-words");
        List<String> listFallback = Collections.emptyList();
        if (bannedObj instanceof List) {
            listFallback = config.getStringList("banned-words");
        }
        this.bannedWords = listFallback.stream().map(String::toLowerCase).collect(Collectors.toList());

        wildcardBannedPatterns.clear();
        wildcardBannedRaw.clear();
        if (bannedObj instanceof String) {
            String path = ((String) bannedObj).trim();
            if (!path.isEmpty()) {
                File target = new File(plugin.getDataFolder(), path);
                ensureDefaultWildcardFilters(target);
                loadWildcardBannedFromPath(target);
                plugin.getLogger().info("[ChatFilter] Loaded " + wildcardBannedPatterns.size()
                        + " wildcard banned patterns from: " + target.getPath());
            }
        }

        ConfigurationSection anti = config.getConfigurationSection("anti-advertising");
        this.antiAdvertisingEnabled = anti != null && anti.getBoolean("enabled", true);
        this.tlds = anti != null ? anti.getStringList("tlds") : Collections.emptyList();
        this.antiAdPhrases = anti != null ? anti.getStringList("block-phrases") : Collections.emptyList();
        this.antiAdPlayerMessage = anti != null ? anti.getString("player-message", "&cAdvertising is not allowed.") : "&cAdvertising is not allowed.";
        this.antiAdStaffMessage = anti != null ? anti.getString("staff-message", "&c{player} attempted to advertise: &7{message}") : "&c{player} attempted to advertise: &7{message}";
        this.antiAdPunishments = Collections.emptyMap();
        if (anti != null && anti.isConfigurationSection("punishments")) {
            this.antiAdPunishments = new HashMap<>(anti.getConfigurationSection("punishments").getValues(false));
        }

        rules.clear();
        List<Map<?, ?>> rawRules = config.getMapList("rules");
        for (Map<?, ?> raw : rawRules) {
            String name = String.valueOf(raw.get("name"));
            String regex = null;
            if (raw.containsKey("regex")) {
                Object regexObj = raw.get("regex");
                if (regexObj instanceof byte[]) {
                    regex = new String((byte[]) regexObj);
                } else if (regexObj != null) {
                    regex = String.valueOf(regexObj);
                }
            }
            String rulePlayerMsg = raw.containsKey("player-message") ? String.valueOf(raw.get("player-message")) : "";
            String ruleStaffMsg = raw.containsKey("staff-message") ? String.valueOf(raw.get("staff-message")) : "";
            @SuppressWarnings("unchecked")
            Map<String, Object> punish = raw.containsKey("punishments") ? (Map<String, Object>) raw.get("punishments") : Collections.emptyMap();
            FilterRule fr = new FilterRule(name, regex, rulePlayerMsg, ruleStaffMsg, punish);
            rules.put(name, fr);
        }
    }

    // =========================================================================
    //  FilterResult
    // =========================================================================

    public static class FilterResult {
        public final boolean blocked;
        public final boolean censored;
        public final String censoredMessage;

        private FilterResult(boolean blocked, boolean censored, String censoredMessage) {
            this.blocked = blocked;
            this.censored = censored;
            this.censoredMessage = censoredMessage;
        }

        public static FilterResult pass() { return new FilterResult(false, false, null); }
        public static FilterResult block() { return new FilterResult(true, false, null); }
        public static FilterResult censor(String msg) { return new FilterResult(false, true, msg); }
    }

    // =========================================================================
    //  Single entry point: checkAndCensor
    // =========================================================================

    /**
     * Single entry point for all filter checks. Works in both block and censor mode.
     * In block mode, returns FilterResult.block() on violation.
     * In censor mode, returns FilterResult.censor(censored) with bad words replaced.
     * Cooldown/spam/similarity always block regardless of mode.
     */
    public FilterResult checkAndCensor(Player player, String message, String type) {
        if (!enabled) return FilterResult.pass();
        if (bypassEnabled && player.hasPermission("awesomechat.filter.bypass")) return FilterResult.pass();

        UUID uuid = player.getUniqueId();
        String normalized = message.toLowerCase();
        long now = System.currentTimeMillis();
        boolean censorMode = filterMode.equalsIgnoreCase("censor");

        boolean isCommand = type.equalsIgnoreCase("command");
        String baseCommand = "";
        if (isCommand && message.startsWith("/")) {
            String[] parts = message.substring(1).split(" ");
            baseCommand = parts[0].toLowerCase();
        }

        // ================= Cooldown Check (always blocks) =================
        if (cooldownEnabled && (cooldownBypassPermission == null
                || !player.hasPermission(cooldownBypassPermission))) {

            boolean applyCooldown = !isCommand;

            if (isCommand) {
                boolean listed = cooldownCommands.isEmpty() || cooldownCommands.contains(baseCommand);
                boolean whitelisted = cooldownWhitelist.contains(baseCommand);
                applyCooldown = listed && !whitelisted;
            }

            if (applyCooldown) {
                Long last = lastMessageTime.get(uuid);

                if (last != null && now - last < cooldownMs) {
                    long remainingMs = cooldownMs - (now - last);
                    double remainingSeconds = Math.ceil(remainingMs / 100.0) / 10.0;

                    String raw = cooldownMessage
                            .replace("{prefix}", plugin.getConfig().getString("prefix", "&7[&bAwesomeChat&7] "))
                            .replace("{time}", String.valueOf(remainingSeconds));

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (String line : raw.split("\n")) {
                            player.sendMessage(chatColorTranslate(line));
                        }
                    });

                    return FilterResult.block();
                }

                lastMessageTime.put(uuid, now);
            }
        }

        // ================= Spam / Similarity Check (always blocks) =================
        if (spamEnabled || similarityEnabled) {
            boolean applyCheck = !isCommand;

            if (isCommand) {
                boolean spamListed = spamCommands.isEmpty() || spamCommands.contains(baseCommand);
                boolean simListed = similarityCommands.isEmpty() || similarityCommands.contains(baseCommand);
                boolean spamWhitelisted = spamWhitelist.contains(baseCommand);
                boolean simWhitelisted = similarityWhitelist.contains(baseCommand);

                boolean isSpamTarget = spamListed && !spamWhitelisted;
                boolean isSimilarityTarget = simListed && !simWhitelisted;

                applyCheck = isSpamTarget || isSimilarityTarget;
            }

            if (applyCheck) {
                String compareValue = spamArgSensitive ? normalized : (isCommand ? baseCommand : normalized);

                if (lastMessageContent.containsKey(uuid)) {
                    String prev = lastMessageContent.get(uuid);
                    double sim = similarity(compareValue, prev);

                    if (sim >= similarityThreshold) {
                        int count = spamCount.getOrDefault(uuid, 0) + 1;
                        spamCount.put(uuid, count);

                        if (count >= spamLimit) {
                            handleViolation(player, message, "spam", "similar-message", type);
                            spamCount.put(uuid, 0);
                            return FilterResult.block();
                        }
                    } else {
                        spamCount.put(uuid, 0);
                    }
                }
                lastMessageContent.put(uuid, compareValue);
            }
        }

        // ================= Content checks (block or censor) =================
        if (censorMode) {
            return doCensorChecks(player, message, type);
        } else {
            return doBlockChecks(player, message, type);
        }
    }

    // =========================================================================
    //  Block mode checks
    // =========================================================================

    private FilterResult doBlockChecks(Player player, String message, String type) {
        String normalized = message.toLowerCase();

        // Banned words
        for (String bw : bannedWords) {
            Pattern p = Pattern.compile("(?i)(^|\\W)" + Pattern.quote(bw) + "($|\\W)");
            if (p.matcher(message).find()) {
                handleBannedWord(player, message, bw, type);
                return FilterResult.block();
            }
        }

        // Wildcard banned patterns
        String[] words = message.split("\\s+");
        for (int i = 0; i < wildcardBannedPatterns.size(); i++) {
            Pattern pat = wildcardBannedPatterns.get(i);
            for (String word : words) {
                if (pat.matcher(word).matches()) {
                    handleBannedWord(player, message, wildcardBannedRaw.get(i), type);
                    return FilterResult.block();
                }
            }
        }

        // Anti-advertising
        if (antiAdvertisingEnabled) {
            for (String phrase : antiAdPhrases) {
                if (normalized.contains(phrase.toLowerCase())) {
                    handleAdvertising(player, message, phrase, type);
                    return FilterResult.block();
                }
            }

            for (String tld : tlds) {
                Pattern p = Pattern.compile("\\b[a-zA-Z0-9-]+" + Pattern.quote(tld) + "\\b", Pattern.CASE_INSENSITIVE);
                if (p.matcher(message).find()) {
                    handleAdvertising(player, message, tld, type);
                    return FilterResult.block();
                }
            }
        }

        // Regex rules
        for (FilterRule rule : rules.values()) {
            if (rule.regex == null || rule.regex.isEmpty()) continue;
            try {
                Pattern p = Pattern.compile(rule.regex, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(message);
                if (m.find()) {
                    handleViolation(player, message, rule.name, m.group(), type);
                    return FilterResult.block();
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid regex for rule " + rule.name + ": " + rule.regex);
            }
        }

        return FilterResult.pass();
    }

    // =========================================================================
    //  Censor mode checks
    // =========================================================================

    private FilterResult doCensorChecks(Player player, String message, String type) {
        String censored = message;
        boolean anyCensored = false;

        // Banned words censor
        for (String bw : bannedWords) {
            Pattern p = Pattern.compile("(?i)(^|\\W)(" + Pattern.quote(bw) + ")($|\\W)");
            Matcher m = p.matcher(censored);
            if (m.find()) {
                String replacement = censorChar.repeat(bw.length());
                censored = censored.replaceAll("(?i)" + Pattern.quote(bw), replacement);
                anyCensored = true;
                incrementOffense(player, "banned-word");
            }
        }

        // Wildcard patterns censor
        for (int i = 0; i < wildcardBannedPatterns.size(); i++) {
            Pattern pat = wildcardBannedPatterns.get(i);
            String[] words = censored.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int w = 0; w < words.length; w++) {
                if (w > 0) sb.append(" ");
                if (pat.matcher(words[w]).matches()) {
                    sb.append(censorChar.repeat(words[w].length()));
                    anyCensored = true;
                } else {
                    sb.append(words[w]);
                }
            }
            censored = sb.toString();
        }
        if (anyCensored && wildcardBannedPatterns.size() > 0) {
            incrementOffense(player, "banned-word");
        }

        // Anti-advertising censor
        if (antiAdvertisingEnabled) {
            String normalized = censored.toLowerCase();
            for (String phrase : antiAdPhrases) {
                if (normalized.contains(phrase.toLowerCase())) {
                    censored = censored.replaceAll("(?i)" + Pattern.quote(phrase), censorChar.repeat(phrase.length()));
                    anyCensored = true;
                    incrementOffense(player, "advertising");
                }
            }

            for (String tld : tlds) {
                Pattern p = Pattern.compile("(\\b[a-zA-Z0-9-]+" + Pattern.quote(tld) + "\\b)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(censored);
                if (m.find()) {
                    censored = m.replaceAll(matchResult -> censorChar.repeat(matchResult.group().length()));
                    anyCensored = true;
                    incrementOffense(player, "advertising");
                }
            }
        }

        // Regex rules censor
        for (FilterRule rule : rules.values()) {
            if (rule.regex == null || rule.regex.isEmpty()) continue;
            try {
                Pattern p = Pattern.compile(rule.regex, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(censored);
                if (m.find()) {
                    censored = m.replaceAll(matchResult -> censorChar.repeat(matchResult.group().length()));
                    anyCensored = true;
                    incrementOffense(player, rule.name);
                }
            } catch (Exception ignored) {}
        }

        if (anyCensored) {
            // Notify staff about the censor action
            String staffNotify = replacePlaceholders(prefix + staffMessage, player, message);
            notifyStaff(player, staffNotify);

            FilterLogger.log(player.getName() + " message censored: " + message);
            return FilterResult.censor(censored);
        }

        return FilterResult.pass();
    }

    /**
     * Increment offense count for a player+rule without triggering full violation handling.
     * Used in censor mode where the message is allowed through (censored) but offenses still tracked.
     */
    private void incrementOffense(Player player, String ruleName) {
        UUID id = player.getUniqueId();
        Map<String, Integer> userMap = offensesCache.computeIfAbsent(id, k -> new HashMap<>());
        int count = userMap.getOrDefault(ruleName, 0) + 1;
        userMap.put(ruleName, count);
        saveOffensesToFileAsync();

        ViolationStorage.addViolation(id, ruleName);

        ChatFilterViolationEvent event = new ChatFilterViolationEvent(player, "", ruleName, count);
        Bukkit.getPluginManager().callEvent(event);

        // Check if a punishment threshold is reached
        FilterRule rule = rules.get(ruleName);
        Map<String, Object> punishments;
        if (rule != null) {
            punishments = rule.punishments;
        } else if ("banned-word".equals(ruleName)) {
            punishments = globalPunishments;
        } else if ("advertising".equals(ruleName)) {
            punishments = antiAdPunishments;
        } else {
            punishments = Collections.emptyMap();
        }

        String action = resolvePunishment(punishments, count);
        if (action != null && !action.trim().isEmpty()) {
            final String cmd = action
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString());
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }.runTask(plugin);
        }
    }

    // =========================================================================
    //  Similarity algorithm
    // =========================================================================

    private double similarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int maxDist = Math.max(s1.length(), s2.length()) / 2 - 1;
        if (maxDist < 0) maxDist = 0;
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
        int winklerPrefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(s1.length(), s2.length())); i++) {
            if (s1.charAt(i) == s2.charAt(i)) winklerPrefix++;
            else break;
        }
        return jaro + 0.1 * winklerPrefix * (1 - jaro);
    }

    // =========================================================================
    //  Violation handling (block mode)
    // =========================================================================

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
                    playerMessage,
                    staffMessage,
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

        String resolvedPlayerMessage = replacePlaceholders(prefix + rule.playerMessage, player, message);
        String resolvedStaffMessage = replacePlaceholders(prefix + rule.staffMessage, player, message);

        String action = resolvePunishment(rule.punishments, count);

        final String finalAction = action;
        final String finalPlayerMessage = resolvedPlayerMessage;
        final String finalStaffMessage = resolvedStaffMessage;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalPlayerMessage != null && !finalPlayerMessage.isEmpty()) {
                    player.sendMessage(chatColorTranslate(finalPlayerMessage));
                }

                notifyStaff(player, finalStaffMessage);

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
        String ruleName = "banned-word";
        FilterRule rule = rules.get(ruleName);

        if (rule == null) {
            rule = new FilterRule(
                    ruleName,
                    null,
                    playerMessage,
                    staffMessage,
                    globalPunishments
            );
        }

        handleViolation(player, message, rule.name, matchedWord, type);
    }

    private void handleAdvertising(Player player, String message, String matched, String type) {
        String ruleName = "advertising";
        FilterRule rule = rules.get(ruleName);

        if (rule == null) {
            rule = new FilterRule(
                    ruleName,
                    null,
                    antiAdPlayerMessage,
                    antiAdStaffMessage,
                    antiAdPunishments
            );
        }

        handleViolation(player, message, rule.name, matched, type);
    }

    // =========================================================================
    //  Punishment resolution
    // =========================================================================

    private String resolvePunishment(Map<String, Object> punishments, int count) {
        if (punishments == null || punishments.isEmpty()) return null;

        // Direct match
        Object actionObj = punishments.get(count);
        if (actionObj == null) {
            actionObj = punishments.get(String.valueOf(count));
        }
        if (actionObj != null) return String.valueOf(actionObj);

        // Fallback: find best matching threshold
        int best = -1;
        boolean hasRepeat = false;

        for (Object keyObj : punishments.keySet()) {
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
            if (keyNum > 0 && keyNum <= count && keyNum > best) {
                best = keyNum;
            }
        }

        if (best != -1) {
            Object bestObj = punishments.get(best);
            if (bestObj == null) bestObj = punishments.get(String.valueOf(best));
            if (bestObj != null) return String.valueOf(bestObj);
        } else if (hasRepeat) {
            // Only use repeat if all numeric thresholds are below count
            boolean hasFutureNonRepeat = false;
            for (Object keyObj : punishments.keySet()) {
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
                Object repeatObj = punishments.get("repeat");
                if (repeatObj != null) return String.valueOf(repeatObj);
            }
        }

        return null;
    }

    // =========================================================================
    //  Offenses persistence
    // =========================================================================

    private void loadOffensesFromFile() {
        offensesCache.clear();
        if (dataFile == null || !dataFile.exists()) return;

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
        }.runTaskAsynchronously(plugin);
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    private void notifyStaff(Player source, String message) {
        if (!announceActions) return;
        String notifyPerm = "awesomechat.filter.notify";
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(notifyPerm))
                .forEach(p -> p.sendMessage(chatColorTranslate(message)));
    }

    private String replacePlaceholders(String template, Player player, String message) {
        if (template == null) return "";
        return template
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{message}", message);
    }

    private String chatColorTranslate(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFilterCommands() {
        return filterCommands;
    }

    // =========================================================================
    //  FilterRule
    // =========================================================================

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

    // =========================================================================
    //  Wildcard / banned-words file loading
    // =========================================================================

    private void ensureDefaultWildcardFilters(File target) {
        try {
            // Detect if target is meant to be a directory (path has no file extension or already is a directory)
            boolean isDir = target.isDirectory()
                    || (!target.exists() && !target.getName().contains("."));
            File dir = isDir ? target : target.getParentFile();
            if (dir == null) return;

            if (!dir.exists()) dir.mkdirs();

            File curse = new File(dir, "curse_words.txt");
            File sexual = new File(dir, "sexual.txt");
            File slurs = new File(dir, "slurs.txt");
            File abuse = new File(dir, "abuse_violence.txt");

            if (!curse.exists() && isDir) {
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

            if (!sexual.exists() && isDir) {
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

            if (!slurs.exists() && isDir) {
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

            if (!abuse.exists() && isDir) {
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
        if (file.exists()) return;
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
}
