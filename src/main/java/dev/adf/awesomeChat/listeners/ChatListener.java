package dev.adf.awesomeChat.listeners;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import dev.adf.awesomeChat.managers.ChatFilterManager;
import dev.adf.awesomeChat.managers.ChannelManager;
import dev.adf.awesomeChat.managers.IgnoreManager;
import dev.adf.awesomeChat.managers.MentionManager;
import dev.adf.awesomeChat.managers.ItemDisplayManager;
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

        // check if chat is muted
        if (plugin.isChatMuted() && !player.hasPermission("awesomechat.mutechat.bypass")) {
            String mutedMsg = plugin.getFormattedConfigString("mutechat.player-message", "&cChat is currently muted.");
            player.sendMessage(mutedMsg);
            event.setCancelled(true);
            return;
        }

        // check the content first before filtering (supports block + censor modes)
        ChatFilterManager filter = plugin.getChatFilterManager();
        if (filter != null) {
            ChatFilterManager.FilterResult result = filter.checkAndCensor(player, plainMessage, "message");
            if (result.blocked) {
                event.setCancelled(true);
                return;
            }
            if (result.censored) {
                plainMessage = result.censoredMessage;
            }
        }

        // Emoji replacement
        if (config.getBoolean("emoji.enabled", false) && player.hasPermission("awesomechat.emoji")) {
            org.bukkit.configuration.ConfigurationSection emojiSection = config.getConfigurationSection("emoji.shortcuts");
            if (emojiSection != null) {
                for (String key : emojiSection.getKeys(false)) {
                    String shortcut = ":" + key + ":";
                    String symbol = emojiSection.getString(key, "");
                    if (!symbol.isEmpty()) {
                        plainMessage = plainMessage.replace(shortcut, symbol);
                    }
                }
            }
        }

        // Route to channel if player has an active channel
        ChannelManager channelManager = plugin.getChannelManager();
        if (channelManager != null && channelManager.hasActiveChannel(player)) {
            String activeChannel = channelManager.getActiveChannel(player);
            ChannelManager.ChatChannel channel = channelManager.getChannel(activeChannel);
            if (channel != null && channelManager.hasAccess(player, channel)) {
                event.setCancelled(true);
                channelManager.sendToChannel(player, activeChannel, plainMessage);
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

        boolean globalMiniMessage = config.getBoolean("minimessage.enabled");
        boolean playerCanUseMiniMessage = player.hasPermission("awesomechat.format.minimessage");
        boolean useMiniMessage = globalMiniMessage && playerCanUseMiniMessage;
        boolean permissionBasedFormatting = config.getBoolean("color-codes.permission-based", false);

        // Mention processing (before formatting so highlight codes get processed)
        MentionManager mentionManager = plugin.getMentionManager();
        MentionManager.MentionResult mentionResult = null;
        if (mentionManager != null) {
            mentionResult = mentionManager.processMentions(player, plainMessage, useMiniMessage);
            plainMessage = mentionResult.processedMessage;
        }

        // Item display processing (before color formatting so triggers are detected in plain text)
        ItemDisplayManager displayManager = plugin.getItemDisplayManager();
        Component itemDisplayComponent = null;
        if (displayManager != null && displayManager.hasTriggers(plainMessage)) {
            final boolean mm = useMiniMessage;
            final boolean pb = permissionBasedFormatting;
            itemDisplayComponent = displayManager.buildRichMessageComponent(player, plainMessage, text -> {
                if (mm) {
                    String filtered = pb
                        ? dev.adf.awesomeChat.utils.ChatFormatPermissionUtil.filterMiniMessageByPermission(player, text)
                        : text;
                    return miniMessage.deserialize(convertToMiniMessage(filtered));
                } else {
                    String filtered = pb
                        ? dev.adf.awesomeChat.utils.ChatFormatPermissionUtil.filterByPermission(player, text)
                        : text;
                    return Component.text(formatColors(filtered));
                }
            });
        }

        String processedMessage;
        if (useMiniMessage) {
            String filtered = permissionBasedFormatting
                ? dev.adf.awesomeChat.utils.ChatFormatPermissionUtil.filterMiniMessageByPermission(player, plainMessage)
                : plainMessage;
            processedMessage = convertToMiniMessage(filtered);
        } else {
            String filtered = permissionBasedFormatting
                ? dev.adf.awesomeChat.utils.ChatFormatPermissionUtil.filterByPermission(player, plainMessage)
                : plainMessage;
            processedMessage = formatColors(filtered);
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

        Component chatComponent = buildChatComponent(
            player,
            playerGroup,
            chatFormat,
            prefix,
            suffix,
            player.getName(),
            processedMessage,
            useMiniMessage,
            itemDisplayComponent
        );
        final Component finalChatComponent = chatComponent;
        IgnoreManager ignoreManager = plugin.getIgnoreManager();
        if (!config.getBoolean("disable-chat-signing")) {
            event.renderer((source, displayName, message, audience) -> {
                if (audience instanceof Player viewer && ignoreManager != null) {
                    if (ignoreManager.isIgnoring(viewer, source)) {
                        return Component.empty();
                    }
                }
                return finalChatComponent;
            });
        } else {
            event.setCancelled(true);
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (ignoreManager != null && ignoreManager.isIgnoring(target, player)) {
                    continue;
                }
                if (!target.equals(player)) {
                    target.sendMessage(finalChatComponent);
                }
            }
            player.sendMessage(finalChatComponent);
        }

        if (config.getBoolean("chat-format.sound.enabled", true)) {
            plugin.getSoundManager().playChatSound(player);
        }

        // Send mention notifications (sounds + action bar)
        if (mentionResult != null && !mentionResult.mentionedPlayers.isEmpty() && mentionManager != null) {
            mentionManager.sendNotifications(player, mentionResult);
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

    private Component buildChatComponent(
            Player player,
            String playerGroup,
            String chatFormat,
            String prefix,
            String suffix,
            String playerName,
            String processedMessage,
            boolean useMiniMessage,
            Component richMessage
    ) {
        FileConfiguration config = plugin.getPluginConfig();
        boolean hoverEnabled = config.getBoolean("hoverable-messages.enabled");
        boolean clickEnabled = config.getBoolean("clickable-messages.enabled");

        Component usernameComponent = Component.text(playerName);
        Component messageComponent;

        // Use rich message component (from item display) if available, otherwise build normally
        if (richMessage != null) {
            messageComponent = richMessage;
        } else if (useMiniMessage) {
            messageComponent = miniMessage.deserialize(processedMessage);
        } else {
            messageComponent = Component.text(processedMessage);
        }

        if (hoverEnabled) {
            Component usernameHover = plugin.getHoverManager().getUsernameHover(player);
            if (usernameHover != null && !usernameHover.equals(Component.empty())) {
                usernameComponent = usernameComponent.hoverEvent(HoverEvent.showText(usernameHover));
            }

            String usernameClickAction = plugin.getHoverManager().getClickAction(playerGroup, "username");
            String usernameClickType = plugin.getHoverManager().getClickType(playerGroup, "username");
            if (usernameClickAction != null) {
                usernameClickAction = usernameClickAction.replace("%player%", playerName);
                usernameComponent = applyClickEvent(usernameComponent, usernameClickAction, usernameClickType);
            }

            // Only apply message hover/click when not using rich message (item display has its own events)
            if (richMessage == null) {
                Component messageHover = plugin.getHoverManager().getMessageHover(player);
                if (messageHover != null && !messageHover.equals(Component.empty())) {
                    messageComponent = messageComponent.hoverEvent(HoverEvent.showText(messageHover));
                }

                String messageClickAction = plugin.getHoverManager().getClickAction(playerGroup, "message");
                String messageClickType = plugin.getHoverManager().getClickType(playerGroup, "message");
                if (messageClickAction != null) {
                    messageClickAction = messageClickAction
                            .replace("%player%", playerName)
                            .replace("%message%", PlainTextComponentSerializer.plainText().serialize(messageComponent));
                    messageComponent = applyClickEvent(messageComponent, messageClickAction, messageClickType);
                }
            }
        } else if (clickEnabled) {
            String clickCommand = config.getString("clickable-messages.command", "/msg %player% ").replace("%player%", playerName);
            String actionType = config.getString("clickable-messages.action", "suggest").toLowerCase();
            usernameComponent = applyClickEvent(usernameComponent, clickCommand, actionType);
        }

        String formattedFormat = chatFormat
                .replace(config.getString("placeholders.chat.prefix", "{prefix}"), prefix)
                .replace(config.getString("placeholders.chat.suffix", "{suffix}"), suffix);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedFormat = PlaceholderAPI.setPlaceholders(player, formattedFormat);
        }

        return buildComponentFromFormat(formattedFormat, usernameComponent, messageComponent, config);
    }

    private Component applyClickEvent(Component component, String clickAction, String clickType) {
        return switch (clickType.toLowerCase()) {
            case "execute" -> component.clickEvent(ClickEvent.runCommand(clickAction));
            case "copy" -> component.clickEvent(ClickEvent.copyToClipboard(clickAction));
            default -> component.clickEvent(ClickEvent.suggestCommand(clickAction));
        };
    }

    private Component buildComponentFromFormat(String format, Component usernameComponent, Component messageComponent, FileConfiguration config) {
        String usernamePlaceholder = config.getString("placeholders.chat.username", "{player}");
        String messagePlaceholder = config.getString("placeholders.chat.message", "{message}");

        int usernameIndex = format.indexOf(usernamePlaceholder);
        int messageIndex = format.indexOf(messagePlaceholder);

        if (usernameIndex == -1 && messageIndex == -1) {
            return Component.text(formatColors(format));
        }

        Component result = Component.empty();
        int lastIndex = 0;

        if (usernameIndex != -1 && (messageIndex == -1 || usernameIndex < messageIndex)) {
            if (usernameIndex > 0) {
                result = result.append(Component.text(formatColors(format.substring(0, usernameIndex))));
            }
            result = result.append(usernameComponent);
            lastIndex = usernameIndex + usernamePlaceholder.length();

            if (messageIndex != -1) {
                if (messageIndex > lastIndex) {
                    result = result.append(Component.text(formatColors(format.substring(lastIndex, messageIndex))));
                }
                result = result.append(messageComponent);
                lastIndex = messageIndex + messagePlaceholder.length();
            }
        } else if (messageIndex != -1) {
            if (messageIndex > 0) {
                result = result.append(Component.text(formatColors(format.substring(0, messageIndex))));
            }
            result = result.append(messageComponent);
            lastIndex = messageIndex + messagePlaceholder.length();

            if (usernameIndex != -1 && usernameIndex > messageIndex) {
                if (usernameIndex > lastIndex) {
                    result = result.append(Component.text(formatColors(format.substring(lastIndex, usernameIndex))));
                }
                result = result.append(usernameComponent);
                lastIndex = usernameIndex + usernamePlaceholder.length();
            }
        }

        if (lastIndex < format.length()) {
            result = result.append(Component.text(formatColors(format.substring(lastIndex))));
        }

        return result;
    }
}
/* TODO:
- Config manager
- Hex support
- Minimessage Migrations
 */