package dev.adf.awesomeChat;

import dev.adf.awesomeChat.managers.ChatFilterManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class ChatListener implements Listener {

    private final AwesomeChat plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ChatListener(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getPluginConfig();
        if (!config.getBoolean("chat-formatting.enabled")) return;
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        // check the content first before filtering
        ChatFilterManager filter = plugin.getChatFilterManager();
        if (filter != null) {
            boolean blocked = filter.checkAndHandle(player, plainMessage, "message");
            if (blocked) {
                event.setCancelled(true);
                return;
            }
        }

        String prefix = "";
        String suffix = "";
        String customChatPlaceholderUsername = "";
        String customChatPlaceholderMessage = "";
        String customChatPlaceholderPrefix = "";
        String customChatPlaceholderSuffix = "";

        if (config.getBoolean("luckperms.enabled")) {
            prefix = formatColors(LuckPermsUtil.getPlayerPrefix(player));
            suffix = formatColors(LuckPermsUtil.getPlayerSuffix(player));
        }

        if (config.getBoolean("placeholders.enabled")) {
            customChatPlaceholderUsername = config.getString("placeholders.chat.username", "{player}");
            customChatPlaceholderMessage = config.getString("placeholders.chat.message", "{message}");
            customChatPlaceholderPrefix = config.getString("placeholders.chat.prefix", "{prefix}");
            customChatPlaceholderSuffix = config.getString("placeholders.chat.suffix", "{suffix}");
        }

        String playerGroup = LuckPermsUtil.getPlayerGroup(player);
        String chatFormat = config.getString("chat-format.global_format", "{prefix} {player}: {message}");

        if (config.getBoolean("chat-format.per-group-format.enabled")) {
            String groupFormat = config.getString("chat-format.per-group-format.groups." + playerGroup);
            if (groupFormat != null) {
                chatFormat = groupFormat;
            }
        }

        String formattedMessage = chatFormat
                .replace(customChatPlaceholderPrefix, prefix)
                .replace(customChatPlaceholderUsername, player.getName())
                .replace(customChatPlaceholderMessage, plainMessage)
                .replace(customChatPlaceholderSuffix, suffix);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedMessage = PlaceholderAPI.setPlaceholders(player, formattedMessage);
        }

        boolean useMiniMessage = config.getBoolean("minimessage.enabled", false);
        Component chatComponent;

        if (useMiniMessage) {
            chatComponent = miniMessage.deserialize(convertLegacyToMiniMessage(formattedMessage));
        } else {
            formattedMessage = formatColors(formattedMessage);
            chatComponent = Component.text(formattedMessage);
        }

        Bukkit.getConsoleSender().sendMessage(formatColors(formattedMessage));

        if (config.getBoolean("clickable-messages.enabled") || config.getBoolean("hoverable-messages.enabled")) {
            if (config.getBoolean("clickable-messages.enabled")) {
                String clickCommand = config.getString("clickable-messages.command").replace("%player%", player.getName());
                String actionType = config.getString("clickable-messages.action", "fill").toLowerCase();

                switch (actionType) {
                    case "execute":
                        chatComponent = chatComponent.clickEvent(ClickEvent.runCommand(clickCommand));
                        break;
                    case "copy":
                        chatComponent = chatComponent.clickEvent(ClickEvent.copyToClipboard(clickCommand));
                        break;
                    default:
                        chatComponent = chatComponent.clickEvent(ClickEvent.suggestCommand(clickCommand));
                        break;
                }
            }

            if (config.getBoolean("hoverable-messages.enabled")) {
                List<String> hoverLines = config.getStringList("hoverable-messages.text-lines");
                Component hoverText = Component.empty();

                for (int i = 0; i < hoverLines.size(); i++) {
                    String coloredLine = formatColors(hoverLines.get(i).replace("%player%", player.getName()));

                    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                        coloredLine = PlaceholderAPI.setPlaceholders(player, coloredLine);
                    }

                    hoverText = hoverText.append(Component.text(coloredLine));
                    if (i < hoverLines.size() - 1) {
                        hoverText = hoverText.append(Component.newline());
                    }
                }
                chatComponent = chatComponent.hoverEvent(HoverEvent.showText(hoverText));
            }
        }
        final Component finalChatComponent = chatComponent;
        event.renderer((source, displayName, message, audience) -> finalChatComponent);
    }

    public static String formatColors(String message) {
        if (message == null) return "";

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
                .replace("§c", "<red>")
                .replace("§l", "<bold>")
                .replace("§7", "<gray>")
                .replace("§3", "<blue>")
                .replace("§e", "<yellow>")
                .replace("§a", "<green>")
                .replace("§d", "<light_purple>")
                .replace("§f", "<white>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<dark_blue>")
                .replace("§0", "<black>")
                .replace("§b", "<aqua>")
                .replace("§6", "<gold>")
                .replace("§5", "<dark_purple>")
                .replace("§4", "<dark_red>")
                .replace("§2", "<dark_green>")
                .replace("§1", "<dark_aqua>")
                .replace("§3", "<dark_blue>")
                .replace("§k", "<obfuscated>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>");
    }
}
