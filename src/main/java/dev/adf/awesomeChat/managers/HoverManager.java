package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.storage.ViolationStorage;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

import static dev.adf.awesomeChat.listeners.ChatListener.formatColors;
import static dev.adf.awesomeChat.listeners.ChatListener.deserializeLegacy;

public class HoverManager {

    private final AwesomeChat plugin;

    public HoverManager(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    public Component getUsernameHover(Player player) {
        String group = LuckPermsUtil.getPlayerGroup(player);
        List<String> hoverLines = getHoverLines(group, "username");

        if (hoverLines == null || hoverLines.isEmpty()) {
            return Component.empty();
        }

        return buildHoverComponent(player, hoverLines);
    }

    public Component getMessageHover(Player player) {
        String group = LuckPermsUtil.getPlayerGroup(player);
        List<String> hoverLines = getHoverLines(group, "message");

        if (hoverLines == null || hoverLines.isEmpty()) {
            return Component.empty();
        }

        return buildHoverComponent(player, hoverLines);
    }

    private List<String> getHoverLines(String group, String component) {
        FileConfiguration config = plugin.getPluginConfig();

        if (config.getBoolean("hoverable-messages.per-group.enabled", false)) {
            String perGroupPath = "hoverable-messages.per-group.groups." + group + "." + component;
            if (config.isList(perGroupPath)) {
                return config.getStringList(perGroupPath);
            }
        }

        String globalPath = "hoverable-messages.global." + component;
        if (config.isList(globalPath)) {
            return config.getStringList(globalPath);
        }

        if ("username".equals(component) && config.isList("hoverable-messages.text-lines")) {
            return config.getStringList("hoverable-messages.text-lines");
        }

        return null;
    }

    public String getClickAction(String group, String component) {
        FileConfiguration config = plugin.getPluginConfig();

        if (config.getBoolean("hoverable-messages.per-group.enabled", false)) {
            String perGroupPath = "hoverable-messages.per-group.groups." + group + "." + component + "-click-action";
            if (config.contains(perGroupPath)) {
                return config.getString(perGroupPath);
            }
        }

        String globalPath = "hoverable-messages.global." + component + "-click-action";
        if (config.contains(globalPath)) {
            return config.getString(globalPath);
        }

        if ("username".equals(component)) {
            return config.getString("clickable-messages.command", "/msg %player% ");
        }

        return null;
    }

    public String getClickType(String group, String component) {
        FileConfiguration config = plugin.getPluginConfig();

        if (config.getBoolean("hoverable-messages.per-group.enabled", false)) {
            String perGroupPath = "hoverable-messages.per-group.groups." + group + "." + component + "-click-type";
            if (config.contains(perGroupPath)) {
                return config.getString(perGroupPath, "suggest");
            }
        }

        String globalPath = "hoverable-messages.global." + component + "-click-type";
        if (config.contains(globalPath)) {
            return config.getString(globalPath, "suggest");
        }

        if ("username".equals(component)) {
            return config.getString("clickable-messages.action", "suggest");
        }

        return "suggest";
    }

    private Component buildHoverComponent(Player player, List<String> hoverLines) {
        Component hoverText = Component.empty();

        for (int i = 0; i < hoverLines.size(); i++) {
            String line = hoverLines.get(i);

            line = line.replace("%player%", player.getName());

            line = replacePlaceholder(line, "%awesomechat_violations%",
                String.valueOf(ViolationStorage.getViolations(player.getUniqueId()).size()));

            String coloredLine = formatColors(line);

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                coloredLine = PlaceholderAPI.setPlaceholders(player, coloredLine);
            }

            hoverText = hoverText.append(deserializeLegacy(coloredLine));
            if (i < hoverLines.size() - 1) {
                hoverText = hoverText.append(Component.newline());
            }
        }

        return hoverText;
    }

    private String replacePlaceholder(String text, String placeholder, String replacement) {
        if (text.contains(placeholder)) {
            return text.replace(placeholder, replacement);
        }
        return text;
    }
}
