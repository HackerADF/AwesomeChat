package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionManager {

    private final AwesomeChat plugin;

    // Combined pattern: @(role) | @everyone/@here | @player
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "@(?:\\((\\w+)\\)|(everyone|here)|(\\w{3,16}))", Pattern.CASE_INSENSITIVE
    );

    public MentionManager(AwesomeChat plugin) {
        this.plugin = plugin;
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

        Matcher matcher = MENTION_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replacement = matcher.group();

            if (matcher.group(1) != null) {
                // @(role) mention
                String roleName = matcher.group(1);
                if (canMentionRole) {
                    String color = config.getString("mentions.role.highlight-color", "&b");
                    replacement = formatHighlight(color, matcher.group(), useMiniMessage);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(sender)) {
                            String playerGroup = LuckPermsUtil.getPlayerGroup(online);
                            if (roleName.equalsIgnoreCase(playerGroup)) {
                                result.mentionedPlayers.add(online);
                                result.mentionTypes.putIfAbsent(online, "role");
                            }
                        }
                    }
                }
            } else if (matcher.group(2) != null) {
                // @everyone or @here
                String keyword = matcher.group(2).toLowerCase();
                if (keyword.equals("everyone") && canMentionEveryone) {
                    String color = config.getString("mentions.everyone.highlight-color", "&c");
                    replacement = formatHighlight(color, matcher.group(), useMiniMessage);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(sender)) {
                            result.mentionedPlayers.add(online);
                            result.mentionTypes.putIfAbsent(online, "everyone");
                        }
                    }
                } else if (keyword.equals("here") && canMentionHere) {
                    String color = config.getString("mentions.here.highlight-color", "&c");
                    replacement = formatHighlight(color, matcher.group(), useMiniMessage);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(sender)) {
                            result.mentionedPlayers.add(online);
                            result.mentionTypes.putIfAbsent(online, "here");
                        }
                    }
                }
            } else if (matcher.group(3) != null) {
                // @player mention
                String name = matcher.group(3);
                if (canMentionPlayer) {
                    Player target = Bukkit.getPlayerExact(name);
                    if (target != null && target.isOnline() && !target.equals(sender)) {
                        String color = config.getString("mentions.player.highlight-color", "&e");
                        replacement = formatHighlight(color, "@" + target.getName(), useMiniMessage);
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

            if (!target.isOnline()) continue;

            // Play sound
            String soundName = config.getString("mentions." + type + ".sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
            float volume = (float) config.getDouble("mentions." + type + ".sound.volume", 1.0);
            float pitch = (float) config.getDouble("mentions." + type + ".sound.pitch", 1.0);

            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                target.playSound(target.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ignored) {}

            // Send action bar
            if (config.getBoolean("mentions." + type + ".actionbar.enabled", true)) {
                String msg = config.getString("mentions." + type + ".actionbar.message", "&e%sender% mentioned you!");
                msg = msg.replace("%sender%", sender.getName());
                msg = AwesomeChat.formatColors(msg);
                target.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));
            }
        }
    }

    private String formatHighlight(String color, String text, boolean useMiniMessage) {
        if (useMiniMessage) {
            String mmColor = legacyToMiniMessageColor(color);
            return mmColor + text + "<reset>";
        } else {
            return color + text + "&r";
        }
    }

    private String legacyToMiniMessageColor(String legacy) {
        return switch (legacy.toLowerCase()) {
            case "&0" -> "<black>";
            case "&1" -> "<dark_blue>";
            case "&2" -> "<dark_green>";
            case "&3" -> "<dark_aqua>";
            case "&4" -> "<dark_red>";
            case "&5" -> "<dark_purple>";
            case "&6" -> "<gold>";
            case "&7" -> "<gray>";
            case "&8" -> "<dark_gray>";
            case "&9" -> "<blue>";
            case "&a" -> "<green>";
            case "&b" -> "<aqua>";
            case "&c" -> "<red>";
            case "&d" -> "<light_purple>";
            case "&e" -> "<yellow>";
            case "&f" -> "<white>";
            default -> {
                if (legacy.startsWith("&#") && legacy.length() == 8) {
                    yield "<" + legacy.substring(1) + ">";
                }
                yield "<yellow>";
            }
        };
    }
}
