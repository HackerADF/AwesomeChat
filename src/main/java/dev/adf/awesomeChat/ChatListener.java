package dev.adf.awesomeChat;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final AwesomeChat plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ChatListener(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getPluginConfig();

        if (!config.getBoolean("chat-formatting.enabled")) return;


        String messageContent = PlainTextComponentSerializer.plainText().serialize(event.message()); // Raw message text

        String prefix = "";
        String suffix = "";
        String chatFormat = config.getString("chat-format.global_format", "{prefix} {player}: {message}");

        String usernamePlaceholder = config.getString("placeholders.chat.username", "{player}");
        String messagePlaceholder = config.getString("placeholders.chat.message", "{message}");
        String prefixPlaceholder = config.getString("placeholders.chat.prefix", "{prefix}");
        String suffixPlaceholder = config.getString("placeholders.chat.suffix", "{suffix}");

        if (config.getBoolean("luckperms.enabled")) {
            prefix = formatColors(LuckPermsUtil.getPlayerPrefix(player));
            suffix = formatColors("%suffix%");
        }

        String playerGroup = LuckPermsUtil.getPlayerGroup(player);
        if (config.getBoolean("chat-format.per-group-format.enabled")) {
            String groupFormat = config.getString("chat-format.per-group-format.groups." + playerGroup);
            if (groupFormat != null) {
                chatFormat = groupFormat;
            }
        }

        String formatted = chatFormat
                .replace(prefixPlaceholder, prefix)
                .replace(usernamePlaceholder, player.getName())
                .replace(messagePlaceholder, messageContent)
                .replace(suffixPlaceholder, suffix);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }

        boolean useMiniMessage = config.getBoolean("minimessage.enabled", false);
        Component formattedComponent = useMiniMessage
                ? miniMessage.deserialize(convertLegacyToMiniMessage(formatted))
                : Component.text(formatColors(formatted));

        Component baseComponent = useMiniMessage
                ? miniMessage.deserialize(convertLegacyToMiniMessage(formatted))
                : Component.text(formatColors(formatted));

// Apply click event
        if (config.getBoolean("clickable-messages.enabled")) {
            String clickCommand = config.getString("clickable-messages.command", "")
                    .replace("%player%", player.getName());
            String action = config.getString("clickable-messages.action", "fill").toLowerCase();

            ClickEvent clickEvent = switch (action) {
                case "execute" -> ClickEvent.runCommand(clickCommand);
                case "copy" -> ClickEvent.copyToClipboard(clickCommand);
                default -> ClickEvent.suggestCommand(clickCommand);
            };
            baseComponent = baseComponent.clickEvent(clickEvent);
        }

// Apply hover event
        if (config.getBoolean("hoverable-messages.enabled")) {
            List<String> hoverLines = config.getStringList("hoverable-messages.text-lines");
            Component hoverText = Component.empty();

            for (int i = 0; i < hoverLines.size(); i++) {
                String line = hoverLines.get(i).replace("%player%", player.getName());
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    line = PlaceholderAPI.setPlaceholders(player, line);
                }
                hoverText = hoverText.append(Component.text(formatColors(line)));
                if (i < hoverLines.size() - 1) {
                    hoverText = hoverText.append(Component.newline());
                }
            }

            baseComponent = baseComponent.hoverEvent(HoverEvent.showText(hoverText));
        }

// Now baseComponent is effectively final
        Component finalComponent = baseComponent;
        event.renderer((source, displayName, message, viewer) -> finalComponent);

        // Set renderer instead of cancelling — preserves full compatibility
        event.renderer((source, displayName, message, viewer) -> formattedComponent);
    }

    public static String formatColors(String message) {
        if (message == null) return "";

        // Hex color support
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + colorCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private String convertLegacyToMiniMessage(String message) {
        return message
                // Format the legacy format into minimessage
                .replace("§c", "<red>").replace("§l", "<bold>").replace("§7", "<gray>")
                .replace("§3", "<dark_aqua>").replace("§e", "<yellow>").replace("§a", "<green>")
                .replace("§d", "<light_purple>").replace("§f", "<white>").replace("§8", "<dark_gray>")
                .replace("§9", "<blue>").replace("§0", "<black>").replace("§b", "<aqua>")
                .replace("§6", "<gold>").replace("§5", "<dark_purple>").replace("§4", "<dark_red>")
                .replace("§2", "<dark_green>").replace("§1", "<dark_blue>")
                .replace("§k", "<obfuscated>").replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>").replace("§o", "<italic>");
    }
}
