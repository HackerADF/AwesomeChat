package dev.adf.awesomeChat;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        FileConfiguration config = plugin.getPluginConfig();

        // Check if chat formatting is enabled in conf
        if (config.getBoolean("chat-formatting.enabled")) {
            String prefix = "";
            String suffix = "";
            String customChatPlaceholderUsername = "";
            String customChatPlaceholderMessage = "";
            String customChatPlaceholderPrefix = "";
            String customChatPlaceholderSuffix = "";

            // Handle LuckPerms prefix/suffix for luckperms
            if (config.getBoolean("luckperms.enabled")) {
                prefix = formatColors(LuckPermsUtil.getPlayerPrefix(player));
                suffix = formatColors("%suffix%");
            }

            if (config.getBoolean("placeholders.enabled")) {
                customChatPlaceholderUsername = config.getString("placeholders.chat.username", "{player}");
                customChatPlaceholderMessage = config.getString("placeholders.chat.message", "{message}");
                customChatPlaceholderPrefix = config.getString("placeholders.chat.prefix", "{prefix}");
                customChatPlaceholderSuffix = config.getString("placeholders.chat.suffix", "{suffix}");
            }

            // Get player's LuckPerms group - Fixed from last issue
            String playerGroup = LuckPermsUtil.getPlayerGroup(player);
            String chatFormat = config.getString("chat-format.global_format", "{prefix} {player}: {message}");

            // Apply per-group format if enabled (sorted by: Weight first then alphapetically)
            if (config.getBoolean("chat-format.per-group-format.enabled")) {
                String groupFormat = config.getString("chat-format.per-group-format.groups." + playerGroup);
                if (groupFormat != null) {
                    chatFormat = groupFormat;
                }
            }

            // Format chat message
            String formattedMessage = chatFormat
                    .replace(customChatPlaceholderPrefix, prefix)
                    .replace(customChatPlaceholderUsername, player.getName())
                    .replace(customChatPlaceholderMessage, message)
                    .replace(customChatPlaceholderSuffix, suffix);

            // Replace the other placeholders with ones from PAPI
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                formattedMessage = PlaceholderAPI.setPlaceholders(player, formattedMessage);
            }

            // If MiniMessage is enabled, use it for formatting rather than &
            boolean useMiniMessage = config.getBoolean("minimessage.enabled", false);
            Component chatComponent;

            if (useMiniMessage) {
                chatComponent = miniMessage.deserialize(convertLegacyToMiniMessage(formattedMessage));
            } else {
                // Translate both Bukkit (&) color codes and hex (&#RRGGBB) color codes - Hexs; to be fixed
                formattedMessage = formatColors(formattedMessage);
                chatComponent = Component.text(formattedMessage);
            }

            // Create clickable and hoverable messages
            if (config.getBoolean("clickable-messages.enabled") || config.getBoolean("hoverable-messages.enabled")) {

                // Add click event
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

                // Add hover event
                if (config.getBoolean("hoverable-messages.enabled")) {
                    List<String> hoverLines = config.getStringList("hoverable-messages.text-lines");
                    Component hoverText = Component.empty();

                    for (int i = 0; i < hoverLines.size(); i++) {
                        String coloredLine = formatColors(hoverLines.get(i).replace("%player%", player.getName()));

                        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                            coloredLine = PlaceholderAPI.setPlaceholders(player, coloredLine);
                        }

                        hoverText = hoverText.append(Component.text(coloredLine));

                        // Append newline only if it's not the last line
                        if (i < hoverLines.size() - 1) {
                            hoverText = hoverText.append(Component.newline());
                        }
                    }

                    chatComponent = chatComponent.hoverEvent(HoverEvent.showText(hoverText));
                }


                // Send formatted message
                event.setCancelled(true);
                for (Player recipient : event.getRecipients()) {
                    recipient.sendMessage(chatComponent);
                }
            } else {
                // Send formatted message
                event.setCancelled(true);
                player.sendMessage(chatComponent);
            }
        }
    }

    /**
     * Converts color codes in messages, supporting both Bukkit (&) color codes and hex (&#RRGGBB) colors
     * Last checked there was an issue with hex codes - Reminder to look into it later.
     *
     * @param message The message containing color codes
     * @return A fully formatted colorized message
     */
    public static String formatColors(String message) {
        if (message == null) return "";

        // Convert hex colors (&#RRGGBB) to Bukkit format
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + colorCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        // Convert Bukkit color codes (&a, &b, &l, etc.)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Converts legacy format (e.g., §c) into MiniMessage format (e.g., <red>)
     * Used to a certain degree, only works with section symbol, rather than &
     *
     * @param message The message with legacy color codes
     * @return The message converted to MiniMessage format
     */
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
