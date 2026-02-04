package dev.adf.awesomeChat.listeners;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import dev.adf.awesomeChat.managers.ChatFilterManager;
import dev.adf.awesomeChat.managers.ChannelManager;
import dev.adf.awesomeChat.managers.IgnoreManager;
import dev.adf.awesomeChat.managers.MentionManager;
import dev.adf.awesomeChat.managers.ItemDisplayManager;
import dev.adf.awesomeChat.managers.ChatRadiusManager;
import dev.adf.awesomeChat.managers.ChatLogManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .character('\u00a7')
                    .build();

    public ChatListener(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getPluginConfig();
        if (!config.getBoolean("chat-formatting.enabled")) return;
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        final String originalMessage = plainMessage;

        // Custom gradient chat input interception
        dev.adf.awesomeChat.managers.ChatColorManager chatColorMgr = plugin.getChatColorManager();
        if (chatColorMgr != null && chatColorMgr.isAwaitingCustomInput(player.getUniqueId())) {
            event.setCancelled(true);
            chatColorMgr.setAwaitingCustomInput(player.getUniqueId(), false);

            if (plainMessage.equalsIgnoreCase("cancel")) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        plugin.getChatPrefix() + "&cCustom gradient cancelled."));
                return;
            }

            String[] parts = plainMessage.trim().split("\\s+");
            java.util.List<String> customColors = new java.util.ArrayList<>();
            java.util.regex.Pattern hexPat = java.util.regex.Pattern.compile("^#?([A-Fa-f0-9]{6})$");
            for (String part : parts) {
                java.util.regex.Matcher m = hexPat.matcher(part);
                if (m.matches()) {
                    customColors.add("#" + m.group(1).toUpperCase());
                }
            }

            if (customColors.size() < 2 || customColors.size() > 4) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        plugin.getChatPrefix() + "&cPlease provide 2 to 4 valid hex codes. Example: &f#FF0000 #00FF00"));
                return;
            }

            dev.adf.awesomeChat.managers.ChatColorManager.ChatColorData data = chatColorMgr.getPlayerColor(player.getUniqueId());
            if (data == null) {
                data = new dev.adf.awesomeChat.managers.ChatColorManager.ChatColorData();
            }
            data.setType("gradient");
            data.setColors(customColors);
            chatColorMgr.setPlayerColor(player.getUniqueId(), data);

            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&aCustom gradient set: &f" + String.join(" &7-> &f", customColors)));
            return;
        }

        if (plugin.isChatMuted() && !player.hasPermission("awesomechat.mutechat.bypass")) {
            String mutedMsg = plugin.getFormattedConfigString("mutechat.player-message", "&cChat is currently muted.");
            player.sendMessage(mutedMsg);
            event.setCancelled(true);
            return;
        }

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

        ChatRadiusManager radiusManager = plugin.getChatRadiusManager();
        boolean isShout = false;
        if (radiusManager != null && radiusManager.isEnabled()) {
            if (radiusManager.isShout(plainMessage) && player.hasPermission("awesomechat.chat.shout")) {
                isShout = true;
                plainMessage = radiusManager.stripShout(plainMessage);
            }
        }

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

        String customChatPlaceholderUsername = "{player}";
        String customChatPlaceholderMessage = "{message}";
        String customChatPlaceholderPrefix = "{prefix}";
        String customChatPlaceholderSuffix = "{suffix}";

        String prefix = "";
        String suffix = "";

        if (config.getBoolean("luckperms.enabled")) {
            prefix = LuckPermsUtil.getPlayerPrefix(player);
            suffix = LuckPermsUtil.getPlayerSuffix(player);
        }

        if (config.getBoolean("placeholders.enabled")) {
            customChatPlaceholderUsername = config.getString("placeholders.chat.username", "{player}");
            customChatPlaceholderMessage = config.getString("placeholders.chat.message", "{message}");
            customChatPlaceholderPrefix = config.getString("placeholders.chat.prefix", "{prefix}");
            customChatPlaceholderSuffix = config.getString("placeholders.chat.suffix", "{suffix}");
        }

        String playerGroup = LuckPermsUtil.getPlayerGroup(player);

        String chatFormat;
        if (!config.getBoolean("chat-format.enabled")) {
            chatFormat = config.getString("chat-format.disabled_format", "<{player}> {message}");
        } else {
            chatFormat = config.getString("chat-format.global_format", "{prefix} {player}: {message}");
        }

        if (config.getBoolean("chat-format.per-group-format.enabled")) {
            String groupFormat = config.getString("chat-format.per-group-format.groups." + playerGroup);
            if (groupFormat != null) {
                chatFormat = groupFormat;
            }
        }

        if (isShout && radiusManager != null) {
            chatFormat = radiusManager.getShoutFormat() + chatFormat;
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
        boolean canUseColorCodes = player.hasPermission("awesomechat.chat.color") || player.hasPermission("awesomechat.format.color");

        MentionManager mentionManager = plugin.getMentionManager();
        MentionManager.MentionResult mentionResult = null;
        if (mentionManager != null) {
            mentionResult = mentionManager.processMentions(player, plainMessage, useMiniMessage);
            plainMessage = mentionResult.processedMessage;
        }

        ItemDisplayManager displayManager = plugin.getItemDisplayManager();
        Component itemDisplayComponent = null;
        if (displayManager != null && displayManager.hasTriggers(plainMessage)) {
            final boolean mm = useMiniMessage;
            final boolean pb = permissionBasedFormatting;
            final String msgForLambda = plainMessage;
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
                    return deserializeLegacy(formatColors(filtered));
                }
            });
        }

        String processedMessage;
        if (useMiniMessage) {
            String filtered = permissionBasedFormatting
                    ? dev.adf.awesomeChat.utils.ChatFormatPermissionUtil.filterMiniMessageByPermission(player, plainMessage)
                    : plainMessage;
            processedMessage = convertToMiniMessage(filtered);
        } else if (canUseColorCodes && !permissionBasedFormatting) {
            processedMessage = plainMessage;
        } else if (permissionBasedFormatting) {
            processedMessage = dev.adf.awesomeChat.utils.ChatFormatPermissionUtil.filterByPermission(player, plainMessage);
        } else {
            processedMessage = stripColorCodes(plainMessage);
        }

        // Apply persistent chat color (LuckPerms meta chat-color takes priority)
        if (chatColorMgr != null && metaColor == null) {
            processedMessage = chatColorMgr.applyColor(player.getUniqueId(), processedMessage, useMiniMessage);
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
                itemDisplayComponent,
                customChatPlaceholderPrefix,
                customChatPlaceholderSuffix,
                customChatPlaceholderUsername,
                customChatPlaceholderMessage
        );

        final Component finalChatComponent = chatComponent;
        final boolean radiusEnabled = radiusManager != null && radiusManager.isEnabled();
        final boolean finalIsShout = isShout;
        IgnoreManager ignoreManager = plugin.getIgnoreManager();

        if (!config.getBoolean("disable-chat-signing")) {
            // Remove ignored and out-of-range players from viewers entirely
            // so they receive no message at all (no empty space, no hover)
            event.viewers().removeIf(audience -> {
                if (audience instanceof Player viewer && !viewer.equals(player)) {
                    if (ignoreManager != null && ignoreManager.isIgnoring(viewer, player)) {
                        return true;
                    }
                    if (radiusEnabled && !finalIsShout && !radiusManager.isInRange(player, viewer)) {
                        return true;
                    }
                }
                return false;
            });

            event.renderer((source, displayName, message, audience) -> finalChatComponent);
        } else {
            event.setCancelled(true);
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (ignoreManager != null && ignoreManager.isIgnoring(target, player)) {
                    continue;
                }
                if (radiusEnabled && !finalIsShout && !radiusManager.isInRange(player, target)) {
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

        if (mentionResult != null && !mentionResult.mentionedPlayers.isEmpty() && mentionManager != null) {
            mentionManager.sendNotifications(player, mentionResult);
        }

        ChatLogManager chatLogManager = plugin.getChatLogManager();
        if (chatLogManager != null) {
            ChannelManager chMgr = plugin.getChannelManager();
            String channel = (chMgr != null && chMgr.hasActiveChannel(player)) ? chMgr.getActiveChannel(player) : null;
            chatLogManager.logMessage(player.getUniqueId(), player.getName(), originalMessage, channel, false);
        }
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private static final Pattern SHORTHAND_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern ALL_COLOR_CODES = Pattern.compile("(?i)(&[0-9A-FK-ORX]|&#[A-Fa-f0-9]{6}|&x(&[A-Fa-f0-9]){6})");

    private String stripColorCodes(String message) {
        if (message == null) return "";
        return ALL_COLOR_CODES.matcher(message).replaceAll("");
    }

    public static String formatColors(String message) {
        if (message == null) return "";

        Matcher shortMatcher = SHORTHAND_HEX.matcher(message);
        StringBuffer shortSb = new StringBuffer();
        while (shortMatcher.find()) {
            shortMatcher.appendReplacement(shortSb, net.md_5.bungee.api.ChatColor.of("#" + shortMatcher.group(1)).toString());
        }
        shortMatcher.appendTail(shortSb);
        message = shortSb.toString();

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String raw = matcher.group();
            StringBuilder hex = new StringBuilder();
            for (int i = 3; i < raw.length(); i += 2) {
                if (i < raw.length()) {
                    hex.append(raw.charAt(i));
                }
            }
            if (hex.length() == 6) {
                matcher.appendReplacement(sb, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
            }
        }
        matcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static Component deserializeLegacy(String formattedMessage) {
        return LEGACY_SERIALIZER.deserialize(formattedMessage != null ? formattedMessage : "");
    }

    private static final Pattern LEGACY_RGB = Pattern.compile("(?i)&x(&[A-Fa-f0-9]){6}");
    private static final Pattern SHORTHAND_HEX_MM = Pattern.compile("(?i)&#([A-Fa-f0-9]{6})");

    private String convertToMiniMessage(String msg) {
        Matcher shortMatcher = SHORTHAND_HEX_MM.matcher(msg);
        StringBuffer shortSb = new StringBuffer();
        while (shortMatcher.find()) {
            shortMatcher.appendReplacement(shortSb, "<#" + shortMatcher.group(1).toUpperCase() + ">");
        }
        shortMatcher.appendTail(shortSb);
        msg = shortSb.toString();

        Matcher matcher = LEGACY_RGB.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group().toLowerCase()
                    .replace("&x", "")
                    .replace("&", "")
                    .toUpperCase();
            matcher.appendReplacement(sb, "<#" + hex + ">");
        }
        matcher.appendTail(sb);

        String result = sb.toString();
        result = result.replace("&0", "<black>").replace("&0", "<black>");
        result = result.replace("&1", "<dark_blue>").replace("&1", "<dark_blue>");
        result = result.replace("&2", "<dark_green>").replace("&2", "<dark_green>");
        result = result.replace("&3", "<dark_aqua>").replace("&3", "<dark_aqua>");
        result = result.replace("&4", "<dark_red>").replace("&4", "<dark_red>");
        result = result.replace("&5", "<dark_purple>").replace("&5", "<dark_purple>");
        result = result.replace("&6", "<gold>").replace("&6", "<gold>");
        result = result.replace("&7", "<gray>").replace("&7", "<gray>");
        result = result.replace("&8", "<dark_gray>").replace("&8", "<dark_gray>");
        result = result.replace("&9", "<blue>").replace("&9", "<blue>");
        result = result.replace("&a", "<green>").replace("&A", "<green>");
        result = result.replace("&b", "<aqua>").replace("&B", "<aqua>");
        result = result.replace("&c", "<red>").replace("&C", "<red>");
        result = result.replace("&d", "<light_purple>").replace("&D", "<light_purple>");
        result = result.replace("&e", "<yellow>").replace("&E", "<yellow>");
        result = result.replace("&f", "<white>").replace("&F", "<white>");
        result = result.replace("&l", "<bold>").replace("&L", "<bold>");
        result = result.replace("&o", "<italic>").replace("&O", "<italic>");
        result = result.replace("&n", "<underlined>").replace("&N", "<underlined>");
        result = result.replace("&m", "<strikethrough>").replace("&M", "<strikethrough>");
        result = result.replace("&k", "<obfuscated>").replace("&K", "<obfuscated>");
        result = result.replace("&r", "<reset>").replace("&R", "<reset>");
        return result;
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
            Component richMessage,
            String prefixPlaceholder,
            String suffixPlaceholder,
            String usernamePlaceholder,
            String messagePlaceholder
    ) {
        FileConfiguration config = plugin.getPluginConfig();
        boolean hoverEnabled = config.getBoolean("hoverable-messages.enabled");
        boolean clickEnabled = config.getBoolean("clickable-messages.enabled");

        Component usernameComponent = Component.text(playerName);
        Component messageComponent;

        if (richMessage != null) {
            messageComponent = richMessage;
        } else if (useMiniMessage) {
            messageComponent = miniMessage.deserialize(processedMessage);
        } else {
            messageComponent = deserializeLegacy(formatColors(processedMessage));
        }

        if (hoverEnabled) {
            Component usernameHover = plugin.getHoverManager().getUsernameHover(player, playerGroup);
            if (usernameHover != null && !usernameHover.equals(Component.empty())) {
                usernameComponent = usernameComponent.hoverEvent(HoverEvent.showText(usernameHover));
            }

            String usernameClickAction = plugin.getHoverManager().getClickAction(playerGroup, "username");
            String usernameClickType = plugin.getHoverManager().getClickType(playerGroup, "username");
            if (usernameClickAction != null) {
                usernameClickAction = usernameClickAction.replace("%player%", playerName);
                usernameComponent = applyClickEvent(usernameComponent, usernameClickAction, usernameClickType);
            }

            if (richMessage == null) {
                Component messageHover = plugin.getHoverManager().getMessageHover(player, playerGroup);
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

        String formattedPrefix;
        String formattedSuffix;
        if (useMiniMessage) {
            formattedPrefix = convertToMiniMessage(prefix);
            formattedSuffix = convertToMiniMessage(suffix);
        } else {
            formattedPrefix = formatColors(prefix);
            formattedSuffix = formatColors(suffix);
        }

        String formattedFormat = chatFormat
                .replace(prefixPlaceholder, formattedPrefix)
                .replace(suffixPlaceholder, formattedSuffix);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedFormat = PlaceholderAPI.setPlaceholders(player, formattedFormat);
        }

        if (useMiniMessage) {
            formattedFormat = convertToMiniMessage(formattedFormat);
        } else {
            formattedFormat = formatColors(formattedFormat);
        }

        return buildComponentFromFormat(formattedFormat, usernameComponent, messageComponent, useMiniMessage, usernamePlaceholder, messagePlaceholder);
    }

    private Component applyClickEvent(Component component, String clickAction, String clickType) {
        if (clickType == null) {
            clickType = "suggest";
        }
        return switch (clickType.toLowerCase()) {
            case "execute" -> component.clickEvent(ClickEvent.runCommand(clickAction));
            case "copy" -> component.clickEvent(ClickEvent.copyToClipboard(clickAction));
            default -> component.clickEvent(ClickEvent.suggestCommand(clickAction));
        };
    }

    private Component buildComponentFromFormat(
            String format,
            Component usernameComponent,
            Component messageComponent,
            boolean useMiniMessage,
            String usernamePlaceholder,
            String messagePlaceholder
    ) {
        int usernameIndex = format.indexOf(usernamePlaceholder);
        int messageIndex = format.indexOf(messagePlaceholder);

        if (usernameIndex == -1 && messageIndex == -1) {
            if (useMiniMessage) {
                return miniMessage.deserialize(format);
            }
            return deserializeLegacy(format);
        }

        Component result = Component.empty();
        int lastIndex = 0;

        if (usernameIndex != -1 && (messageIndex == -1 || usernameIndex < messageIndex)) {
            if (usernameIndex > 0) {
                String beforeUsername = format.substring(0, usernameIndex);
                if (useMiniMessage) {
                    result = result.append(miniMessage.deserialize(beforeUsername));
                } else {
                    result = result.append(deserializeLegacy(beforeUsername));
                }
                String trailingColor = useMiniMessage ? extractTrailingColorMiniMessage(beforeUsername) : extractTrailingColor(beforeUsername);
                if (!trailingColor.isEmpty()) {
                    String usernameWithColor = trailingColor + PlainTextComponentSerializer.plainText().serialize(usernameComponent);
                    if (useMiniMessage) {
                        Component styledUsername = miniMessage.deserialize(usernameWithColor);
                        usernameComponent = styledUsername.hoverEvent(usernameComponent.hoverEvent()).clickEvent(usernameComponent.clickEvent());
                    } else {
                        Component styledUsername = deserializeLegacy(usernameWithColor);
                        usernameComponent = styledUsername.hoverEvent(usernameComponent.hoverEvent()).clickEvent(usernameComponent.clickEvent());
                    }
                }
            }
            result = result.append(usernameComponent);
            lastIndex = usernameIndex + usernamePlaceholder.length();

            if (messageIndex != -1) {
                if (messageIndex > lastIndex) {
                    String betweenParts = format.substring(lastIndex, messageIndex);
                    if (useMiniMessage) {
                        result = result.append(miniMessage.deserialize(betweenParts));
                    } else {
                        result = result.append(deserializeLegacy(betweenParts));
                    }
                    String trailingColor = useMiniMessage ? extractTrailingColorMiniMessage(betweenParts) : extractTrailingColor(betweenParts);
                    if (!trailingColor.isEmpty()) {
                        Component colorPrefix = useMiniMessage ? miniMessage.deserialize(trailingColor) : deserializeLegacy(trailingColor);
                        messageComponent = colorPrefix.append(messageComponent);
                    }
                }
                result = result.append(messageComponent);
                lastIndex = messageIndex + messagePlaceholder.length();
            }
        } else if (messageIndex != -1) {
            if (messageIndex > 0) {
                String beforeMessage = format.substring(0, messageIndex);
                if (useMiniMessage) {
                    result = result.append(miniMessage.deserialize(beforeMessage));
                } else {
                    result = result.append(deserializeLegacy(beforeMessage));
                }
                String trailingColor = useMiniMessage ? extractTrailingColorMiniMessage(beforeMessage) : extractTrailingColor(beforeMessage);
                if (!trailingColor.isEmpty()) {
                    Component colorPrefix = useMiniMessage ? miniMessage.deserialize(trailingColor) : deserializeLegacy(trailingColor);
                    messageComponent = colorPrefix.append(messageComponent);
                }
            }
            result = result.append(messageComponent);
            lastIndex = messageIndex + messagePlaceholder.length();

            if (usernameIndex != -1 && usernameIndex > messageIndex) {
                if (usernameIndex > lastIndex) {
                    String betweenParts = format.substring(lastIndex, usernameIndex);
                    if (useMiniMessage) {
                        result = result.append(miniMessage.deserialize(betweenParts));
                    } else {
                        result = result.append(deserializeLegacy(betweenParts));
                    }
                    String trailingColor = useMiniMessage ? extractTrailingColorMiniMessage(betweenParts) : extractTrailingColor(betweenParts);
                    if (!trailingColor.isEmpty()) {
                        String usernameWithColor = trailingColor + PlainTextComponentSerializer.plainText().serialize(usernameComponent);
                        if (useMiniMessage) {
                            Component styledUsername = miniMessage.deserialize(usernameWithColor);
                            usernameComponent = styledUsername.hoverEvent(usernameComponent.hoverEvent()).clickEvent(usernameComponent.clickEvent());
                        } else {
                            Component styledUsername = deserializeLegacy(usernameWithColor);
                            usernameComponent = styledUsername.hoverEvent(usernameComponent.hoverEvent()).clickEvent(usernameComponent.clickEvent());
                        }
                    }
                }
                result = result.append(usernameComponent);
                lastIndex = usernameIndex + usernamePlaceholder.length();
            }
        }

        if (lastIndex < format.length()) {
            String remaining = format.substring(lastIndex);
            if (useMiniMessage) {
                result = result.append(miniMessage.deserialize(remaining));
            } else {
                result = result.append(deserializeLegacy(remaining));
            }
        }

        return result;
    }

    private static final Pattern MM_COLOR_TAG = Pattern.compile("<(#[A-Fa-f0-9]{6}|black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>");
    private static final Pattern MM_FORMAT_TAG = Pattern.compile("<(bold|italic|underlined|strikethrough|obfuscated)>");
    private static final Pattern MM_RESET_TAG = Pattern.compile("<reset>");

    private String extractTrailingColorMiniMessage(String text) {
        StringBuilder formatTags = new StringBuilder();
        String lastColorTag = "";

        Matcher resetMatcher = MM_RESET_TAG.matcher(text);
        int lastResetEnd = -1;
        while (resetMatcher.find()) {
            lastResetEnd = resetMatcher.end();
        }

        String searchText = lastResetEnd > 0 ? text.substring(lastResetEnd) : text;

        Matcher colorMatcher = MM_COLOR_TAG.matcher(searchText);
        while (colorMatcher.find()) {
            lastColorTag = colorMatcher.group();
        }

        Matcher formatMatcher = MM_FORMAT_TAG.matcher(searchText);
        int colorEnd = 0;
        if (!lastColorTag.isEmpty()) {
            int idx = searchText.lastIndexOf(lastColorTag);
            if (idx >= 0) {
                colorEnd = idx + lastColorTag.length();
            }
        }

        while (formatMatcher.find()) {
            if (formatMatcher.start() >= colorEnd) {
                formatTags.append(formatMatcher.group());
            }
        }

        return lastColorTag + formatTags.toString();
    }

    private String extractTrailingColor(String text) {
        StringBuilder formatCodes = new StringBuilder();
        String lastColor = "";

        for (int i = text.length() - 1; i >= 0; i--) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));

                if (code == 'r') {
                    return "";
                }

                if (code == 'x' && i + 13 < text.length()) {
                    boolean isHexColor = true;
                    for (int j = 0; j < 6; j++) {
                        int pos = i + 2 + (j * 2);
                        if (pos + 1 >= text.length() || text.charAt(pos) != '§') {
                            isHexColor = false;
                            break;
                        }
                        char hexChar = Character.toLowerCase(text.charAt(pos + 1));
                        if (!((hexChar >= '0' && hexChar <= '9') || (hexChar >= 'a' && hexChar <= 'f'))) {
                            isHexColor = false;
                            break;
                        }
                    }

                    if (isHexColor && lastColor.isEmpty()) {
                        lastColor = text.substring(i, i + 14);
                        i -= 13;
                        continue;
                    }
                }

                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    if (lastColor.isEmpty()) {
                        // Check if this §[hex] is part of a §x§.§.§.§.§.§. hex color sequence
                        int hexStart = i - 12;
                        if (hexStart >= 0 && text.charAt(hexStart) == '§'
                                && Character.toLowerCase(text.charAt(hexStart + 1)) == 'x') {
                            boolean isHexColor = true;
                            for (int j = 0; j < 6; j++) {
                                int pos = hexStart + 2 + (j * 2);
                                if (pos + 1 >= text.length() || text.charAt(pos) != '§') {
                                    isHexColor = false;
                                    break;
                                }
                                char hexChar = Character.toLowerCase(text.charAt(pos + 1));
                                if (!((hexChar >= '0' && hexChar <= '9') || (hexChar >= 'a' && hexChar <= 'f'))) {
                                    isHexColor = false;
                                    break;
                                }
                            }
                            if (isHexColor) {
                                lastColor = text.substring(hexStart, hexStart + 14);
                                break;
                            }
                        }
                        lastColor = "§" + code;
                    }
                    break;
                }

                if (code >= 'k' && code <= 'o') {
                    if (lastColor.isEmpty()) {
                        formatCodes.insert(0, "§" + code);
                    }
                }
            }
        }

        return lastColor + formatCodes.toString();
    }
}