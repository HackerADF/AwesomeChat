package dev.adf.awesomeChat.utils;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatPermissionUtil {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)(&[0-9a-fk-or])|(§[0-9a-fk-or])|(&#[A-Fa-f0-9]{6})");
    private static final Pattern MINIMESSAGE_TAG_PATTERN = Pattern.compile("<([^>]+)>");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[A-Fa-f0-9]{6}");

    public static String filterByPermission(Player player, String message) {
        Matcher matcher = COLOR_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String code = matcher.group();

            if (!hasPermissionForCode(player, code)) {
                matcher.appendReplacement(buffer, "");
                continue;
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(code));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String filterMiniMessageByPermission(Player player, String message) {
        if (!player.hasPermission("awesomechat.format.minimessage")) {
            return stripAllMiniMessageTags(message);
        }

        Matcher matcher = MINIMESSAGE_TAG_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        boolean hasAdvancedPermission = player.hasPermission("awesomechat.format.minimessage.advanced");

        while (matcher.find()) {
            String fullTag = matcher.group(0);
            String tagContent = matcher.group(1);

            if (!hasPermissionForMiniMessageTag(player, tagContent, hasAdvancedPermission)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(""));
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(fullTag));
            }
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static boolean hasPermissionForMiniMessageTag(Player player, String tagContent, boolean hasAdvanced) {
        String tagName = extractTagName(tagContent);

        switch (tagName.toLowerCase()) {
            case "bold", "b":
                return player.hasPermission("awesomechat.styling.bold");
            case "italic", "i", "em":
                return player.hasPermission("awesomechat.styling.italic");
            case "underlined", "u":
                return player.hasPermission("awesomechat.styling.underline");
            case "strikethrough", "st":
                return player.hasPermission("awesomechat.styling.strikethrough");
            case "obfuscated", "obf":
                return player.hasPermission("awesomechat.styling.obfuscated");

            case "color", "c", "colour":
                String colorValue = extractColorValue(tagContent);
                if (colorValue != null && HEX_COLOR_PATTERN.matcher(colorValue).matches()) {
                    return player.hasPermission("awesomechat.styling.color.hex");
                }
                return true;

            case "gradient":
            case "rainbow":
                return player.hasPermission("awesomechat.format.minimessage");

            case "click":
            case "insertion":
            case "hover":
                return hasAdvanced;

            case "reset":
                return true;

            default:
                if (tagContent.startsWith("#")) {
                    return player.hasPermission("awesomechat.styling.color.hex");
                }
                return true;
        }
    }

    private static String extractTagName(String tagContent) {
        if (tagContent.startsWith("/")) {
            return tagContent.substring(1).split(":")[0];
        }

        if (tagContent.startsWith("#")) {
            return "color";
        }

        return tagContent.split(":")[0].split(" ")[0];
    }

    private static String extractColorValue(String tagContent) {
        if (tagContent.contains(":")) {
            String[] parts = tagContent.split(":", 2);
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        return null;
    }

    private static String stripAllMiniMessageTags(String message) {
        return message.replaceAll("<[^>]+>", "");
    }

    private static boolean hasPermissionForCode(Player p, String code) {
        code = code.toLowerCase();
        if (code.startsWith("&#")) {
            return p.hasPermission("awesomechat.styling.color.hex");
        }

        char c = code.charAt(1);

        switch (c) {
            case '0','1','2','3','4','5','6','7','8','9',
                 'a','b','c','d','e','f':
                return p.hasPermission("awesomechat.styling.color." + c);

            case 'l': return p.hasPermission("awesomechat.styling.bold");
            case 'o': return p.hasPermission("awesomechat.styling.italic");
            case 'n': return p.hasPermission("awesomechat.styling.underline");
            case 'm': return p.hasPermission("awesomechat.styling.strikethrough");
            case 'k': return p.hasPermission("awesomechat.styling.obfuscated");
            case 'r': return true;

            default:
                return false;
        }
    }
}
