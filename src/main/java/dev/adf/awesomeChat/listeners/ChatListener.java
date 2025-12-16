package dev.adf.awesomeChat.listeners;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import dev.adf.awesomeChat.managers.ChatFilterManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
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
        if (!config.getBoolean("chat-format.enabled")) {
            chatFormat = config.getString("<{player}> {message}", "<{player}> {message}");
        } else {
            chatFormat = config.getString("chat-format.global_format", "{prefix} {player}: {message}");
        }

        if (config.getBoolean("chat-format.per-group-format.enabled")) {
            String groupFormat = config.getString("chat-format.per-group-format.groups." + playerGroup);
            if (groupFormat != null) {
                chatFormat = groupFormat;
            }
        }

        String metaColor = LuckPermsUtil.getChatMeta(player, "chat-color");
        String metaFormat = LuckPermsUtil.getChatMeta(player, "chat-format");
        if (metaColor != null) {
            plainMessage = metaColor + plainMessage;
        }

        if (metaFormat != null) {
            plainMessage = metaFormat + plainMessage;
        }

        boolean useMiniMessage = config.getBoolean("minimessage.enabled");
        String processedMessage;
        if (useMiniMessage) {
            processedMessage = convertToMiniMessage(plainMessage);
        } else {
            processedMessage = formatColors(plainMessage);
        }

        String formattedMessage = chatFormat
                .replace(customChatPlaceholderPrefix, prefix)
                .replace(customChatPlaceholderUsername, player.getName())
                .replace(customChatPlaceholderMessage, processedMessage)
                .replace(customChatPlaceholderSuffix, suffix);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedMessage = PlaceholderAPI.setPlaceholders(player, formattedMessage);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedMessage = PlaceholderAPI.setPlaceholders(player, formattedMessage);
        }

        Component chatComponent;

        if (useMiniMessage) {
            chatComponent = miniMessage.deserialize(formattedMessage);
        } else {
            chatComponent = Component.text(formatColors(formattedMessage));
        }

        if (config.getBoolean("clickable-messages.enabled") || config.getBoolean("hoverable-messages.enabled")) {
            if (config.getBoolean("clickable-messages.enabled")) {
                String clickCommand = config.getString("clickable-messages.command").replace("%player%", player.getName());
                String actionType = config.getString("clickable-messages.action", "fill").toLowerCase();

                chatComponent = switch (actionType) {
                    case "execute" -> chatComponent.clickEvent(ClickEvent.runCommand(clickCommand));
                    case "copy" -> chatComponent.clickEvent(ClickEvent.copyToClipboard(clickCommand));
                    default -> chatComponent.clickEvent(ClickEvent.suggestCommand(clickCommand));
                };
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
        if (!config.getBoolean("disable-chat-signing")) {
            event.renderer((source, displayName, message, audience) -> finalChatComponent);
        } else {
            event.setCancelled(true);
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player)) {
                    target.sendMessage(finalChatComponent);
                }
                player.sendMessage(finalChatComponent);
            }
        }

        if (config.isConfigurationSection("chat-format.sound")) {
            String soundName = config.getString("chat-format.sound.name", "ENTITY_CHICKEN_EGG");
            float volume = (float) config.getDouble("chat-format.sound.volume", 100);
            float pitch = (float) config.getDouble("chat-format.sound.pitch", 2.0);

            Sound sound;
            try {
                sound = Sound.valueOf(soundName.toUpperCase());
            } catch (IllegalArgumentException e) {
                sound = Sound.ENTITY_CHICKEN_EGG;
            }

            for (Player target : Bukkit.getOnlinePlayers()) {
                target.playSound(target.getLocation(), sound, volume, pitch);
            }
        }
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");

    public static String formatColors(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String raw = matcher.group();
            StringBuilder hex = new StringBuilder();
            String[] chars = raw.split("&");
            for (int i = 1; i <= 6; i++) {
                hex.append(chars[i]);
            }
            matcher.appendReplacement(sb, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }


    private static final Pattern LEGACY_RGB = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private String convertToMiniMessage(String msg) {
        Matcher matcher = LEGACY_RGB.matcher(msg);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group()
                    .replace("&x", "")
                    .replace("&", "");
            matcher.appendReplacement(sb, "<#" + hex + ">");
        }
        matcher.appendTail(sb);

        return sb.toString()
                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }
}
