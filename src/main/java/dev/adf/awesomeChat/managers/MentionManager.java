package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionManager {

    private final AwesomeChat plugin;
    private final MiniMessage miniMessage;

    public MentionManager(AwesomeChat plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public static class MentionResult {
        public String processedMessage;
        public final Set<Player> mentionedPlayers = new HashSet<>();
        public final Map<Player, String> mentionTypes = new HashMap<>();

        public MentionResult(String message) {
            this.processedMessage = message;
        }
    }

    public MentionResult processMentions(Player sender, String message, boolean useMiniMessage) {
        FileConfiguration config = plugin.getPluginConfig();
        if (!config.getBoolean("mentions.enabled", false)) {
            return new MentionResult(message);
        }

        MentionResult result = new MentionResult(message);

        boolean canMentionPlayer = config.getBoolean("mentions.player.enabled", true)
                && sender.hasPermission("awesomechat.mention.player");
        boolean canMentionRole = config.getBoolean("mentions.role.enabled", true)
                && sender.hasPermission("awesomechat.mention.role");
        boolean canMentionEveryone = config.getBoolean("mentions.everyone.enabled", true)
                && sender.hasPermission("awesomechat.mention.everyone");
        boolean canMentionHere = config.getBoolean("mentions.here.enabled", true)
                && sender.hasPermission("awesomechat.mention.here");

        // Build dynamic pattern based on optional @ settings
        boolean playerRequiresAt = config.getBoolean("mentions.player.require-at-symbol", true);
        boolean roleRequiresAt = config.getBoolean("mentions.role.require-at-symbol", true);
        boolean everyoneRequiresAt = config.getBoolean("mentions.everyone.require-at-symbol", true);
        boolean hereRequiresAt = config.getBoolean("mentions.here.require-at-symbol", true);

        String rolePattern = roleRequiresAt ? "@?\\((\\w+)\\)" : "@?\\((\\w+)\\)";
        String everyonePattern = everyoneRequiresAt ? "@(everyone)" : "@?(everyone)";
        String herePattern = hereRequiresAt ? "@(here)" : "@?(here)";
        String playerPattern = playerRequiresAt ? "@(\\w{3,16})" : "@?(\\w{3,16})";

        // Combined pattern: @?(role) | @?everyone/@?here | @?player
        Pattern mentionPattern = Pattern.compile(
                "(?:" + rolePattern + "|" + everyonePattern + "|" + herePattern + "|" + playerPattern + ")",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = mentionPattern.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replacement = matcher.group();
            String fullMatch = matcher.group();

            // Check for role mention (group in parentheses)
            if (fullMatch.contains("(") && fullMatch.contains(")")) {
                String roleName = fullMatch.replaceAll("[()@]", "");
                if (canMentionRole) {
                    replacement = formatMention(config, "role", sender, fullMatch, useMiniMessage);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(sender)) {
                            String playerGroup = LuckPermsUtil.getPlayerGroup(online);
                            if (roleName.equalsIgnoreCase(playerGroup)) {
                                result.mentionedPlayers.add(online);
                                result.mentionTypes.putIfAbsent(online, "role:" + roleName);
                            }
                        }
                    }
                }
            } else if (fullMatch.toLowerCase().contains("everyone")) {
                if (canMentionEveryone) {
                    replacement = formatMention(config, "everyone", sender, fullMatch, useMiniMessage);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(sender)) {
                            result.mentionedPlayers.add(online);
                            result.mentionTypes.putIfAbsent(online, "everyone");
                        }
                    }
                }
            } else if (fullMatch.toLowerCase().contains("here")) {
                if (canMentionHere) {
                    replacement = formatMention(config, "here", sender, fullMatch, useMiniMessage);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(sender)) {
                            result.mentionedPlayers.add(online);
                            result.mentionTypes.putIfAbsent(online, "here");
                        }
                    }
                }
            } else {
                // Player mention
                String name = fullMatch.replace("@", "");
                if (canMentionPlayer) {
                    Player target = Bukkit.getPlayerExact(name);
                    if (target != null && target.isOnline() && !target.equals(sender)) {
                        replacement = formatMention(config, "player", sender,
                            (playerRequiresAt ? "@" : "") + target.getName(), useMiniMessage);
                        result.mentionedPlayers.add(target);
                        result.mentionTypes.putIfAbsent(target, "player");
                    }
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        result.processedMessage = sb.toString();

        return result;
    }

    public void sendNotifications(Player sender, MentionResult result) {
        FileConfiguration config = plugin.getPluginConfig();

        for (Map.Entry<Player, String> entry : result.mentionTypes.entrySet()) {
            Player target = entry.getKey();
            String type = entry.getValue();

            // Extract base type (before colon if role)
            String baseType = type.split(":")[0];
            String roleName = type.contains(":") ? type.split(":")[1] : null;

            if (!target.isOnline()) continue;

            String configPath = "mentions." + baseType;

            // Play sound
            if (config.getBoolean(configPath + ".sound.enabled", true)) {
                String soundName = config.getString(configPath + ".sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
                float volume = (float) config.getDouble(configPath + ".sound.volume", 1.0);
                float pitch = (float) config.getDouble(configPath + ".sound.pitch", 1.0);

                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    target.playSound(target.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException ignored) {}
            }

            // Send title and subtitle
            if (config.getBoolean(configPath + ".title.enabled", false)) {
                String title = config.getString(configPath + ".title.text", "");
                String subtitle = config.getString(configPath + ".title.subtitle", "");
                int fadeIn = config.getInt(configPath + ".title.fade-in", 10);
                int stay = config.getInt(configPath + ".title.stay", 70);
                int fadeOut = config.getInt(configPath + ".title.fade-out", 20);

                title = replacePlaceholders(title, sender, target, roleName);
                subtitle = replacePlaceholders(subtitle, sender, target, roleName);

                Component titleComp = parseColoredText(title);
                Component subtitleComp = parseColoredText(subtitle);

                Title titleObj = Title.title(
                        titleComp,
                        subtitleComp,
                        Title.Times.times(
                                Duration.ofMillis(fadeIn * 50L),
                                Duration.ofMillis(stay * 50L),
                                Duration.ofMillis(fadeOut * 50L)
                        )
                );

                target.showTitle(titleObj);
            }

            // Send action bar
            if (config.getBoolean(configPath + ".actionbar.enabled", false)) {
                String msg = config.getString(configPath + ".actionbar.message", "&e%sender% mentioned you!");
                msg = replacePlaceholders(msg, sender, target, roleName);
                target.sendActionBar(parseColoredText(msg));
            }

            // Send custom chat message
            if (config.getBoolean(configPath + ".chat-message.enabled", false)) {
                String msg = config.getString(configPath + ".chat-message.text", "&e%sender% mentioned you!");
                msg = replacePlaceholders(msg, sender, target, roleName);
                target.sendMessage(parseColoredText(msg));
            }
        }
    }

    /**
     * Formats the mention text according to config settings
     */
    private String formatMention(FileConfiguration config, String type, Player sender, String text, boolean useMiniMessage) {
        String configPath = "mentions." + type + ".format";
        String format = config.getString(configPath, text);

        // Replace placeholders
        format = format.replace("%mention%", text)
                .replace("%player%", text)
                .replace("%sender%", sender.getName());

        // Parse the format as colored text and return as string
        if (useMiniMessage || format.contains("<gradient") || format.contains("<")) {
            // Parse as MiniMessage
            Component comp = miniMessage.deserialize(format);
            return LegacyComponentSerializer.legacySection().serialize(comp);
        } else {
            // Parse as legacy color codes
            return AwesomeChat.formatColors(format);
        }
    }

    /**
     * Replaces placeholders in a message
     */
    private String replacePlaceholders(String text, Player sender, Player target, String roleName) {
        if (text == null || text.isEmpty()) return "";

        text = text.replace("%sender%", sender.getName())
                .replace("%target%", target.getName())
                .replace("%player%", target.getName());

        if (roleName != null) {
            text = text.replace("%role%", roleName);
        }

        // Use PlaceholderAPI if available
        if (plugin.isPluginEnabled("PlaceholderAPI")) {
            try {
                text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, text);
            } catch (Exception ignored) {
                // PlaceholderAPI not available
            }
        }

        return text;
    }

    /**
     * Parses colored text (supports both MiniMessage and legacy codes)
     */
    private Component parseColoredText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Check if text contains MiniMessage tags
        if (text.contains("<gradient") || (text.contains("<") && text.contains(">"))) {
            try {
                return miniMessage.deserialize(text);
            } catch (Exception e) {
                // Fallback to legacy parsing
            }
        }

        // Parse as legacy color codes (including hex)
        String formatted = AwesomeChat.formatColors(text);
        return LegacyComponentSerializer.legacySection().deserialize(formatted);
    }
}
