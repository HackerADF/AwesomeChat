package dev.adf.awesomeChat.utils;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatPermissionUtil {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(&[0-9a-fk-or])|(§[0-9a-fk-or])|(&#[A-Fa-f0-9]{6})");

    public static String filterByPermission(Player player, String message) {
        Matcher matcher = COLOR_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String code = matcher.group();

            if (!hasPermissionForCode(player, code)) {
                matcher.appendReplacement(buffer, "");
                continue;
            }

            matcher.appendReplacement(buffer, code);
        }

        matcher.appendTail(buffer);
        return buffer.toString();
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
