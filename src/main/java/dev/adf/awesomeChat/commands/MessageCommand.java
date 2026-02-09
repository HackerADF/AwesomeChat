package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MessageCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public MessageCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /" + label + " <player> <message>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getFormattedConfigString("private-messages.errors.player-not-found", "&cThat player could not be found!"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getFormattedConfigString("private-messages.errors.cannot-message-yourself", "&cYou cannot message yourself!"));
            return true;
        }

        if (plugin.getIgnoreManager() != null && plugin.getIgnoreManager().isIgnoring(target, player)
                && !player.hasPermission("awesomechat.ignore.bypass")) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.messages-disabled",
                    "&cThat player has private messages disabled!"
            ));
            return true;
        }

        if (plugin.getPrivateMessageManager().hasMessagesDisabled(player)) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.self-messages-disabled",
                    "&cYou have private messages disabled!"
            ));
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        if (plugin.getPrivateMessageManager().hasMessagesDisabled(target)) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.messages-disabled",
                    "&cThat player has private messages disabled!"
            ));
            return true;
        }

        // Check if hover/click components are enabled
        boolean hoverEnabled = plugin.getConfig().getBoolean("private-messages.hover.enabled", false);

        if (hoverEnabled) {
            // Send messages with hover/click components
            Component senderComponent = buildPMComponent(player, target, message, true);
            Component receiverComponent = buildPMComponent(player, target, message, false);

            player.sendMessage(senderComponent);
            target.sendMessage(receiverComponent);
        } else {
            // Send simple messages
            String senderFormat = plugin.getFormattedConfigString(
                            "private-messages.format.sender",
                            "&7[&bYou &7-> &b{receiver}&7] &f{message}"
                    )
                    .replace("{receiver}", target.getName())
                    .replace("{message}", message);

            String receiverFormat = plugin.getFormattedConfigString(
                            "private-messages.format.receiver",
                            "&7[&b{sender} &7-> &bYou&7] &f{message}"
                    )
                    .replace("{sender}", player.getName())
                    .replace("{message}", message);

            player.sendMessage(AwesomeChat.formatColors(senderFormat));
            target.sendMessage(AwesomeChat.formatColors(receiverFormat));
        }

        if (plugin.getConfig().isConfigurationSection("private-messages.sound")) {
            if (!plugin.getConfig().getString("private-messages.sound.sent").equalsIgnoreCase("NONE")) {
                String sentSoundName = plugin.getConfig().getString("private-messages.sound.sent.name", "ENTITY_CHICKEN_EGG");
                float sentVolume = (float) plugin.getConfig().getDouble("private-messages.sound.sent.volume", 100);
                float sentPitch = (float) plugin.getConfig().getDouble("private-messages.sound.sent.pitch", 2.0);

                Sound sound;
                try {
                    sound = Sound.valueOf(sentSoundName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sound = Sound.ENTITY_CHICKEN_EGG;
                }
                player.playSound(target.getLocation(), sound, sentVolume, sentPitch);
            }
            if (!plugin.getConfig().getString("private-messages.sound.received").equalsIgnoreCase("NONE")) {
                String recSoundName = plugin.getConfig().getString("private-messages.sound.sent.name", "ENTITY_CHICKEN_EGG");
                float recVolume = (float) plugin.getConfig().getDouble("private-messages.sound.sent.volume", 100);
                float recPitch = (float) plugin.getConfig().getDouble("private-messages.sound.sent.pitch", 2.0);

                Sound sound;
                try {
                    sound = Sound.valueOf(recSoundName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sound = Sound.ENTITY_CHICKEN_EGG;
                }
                target.playSound(target.getLocation(), sound, recVolume, recPitch);
            }
        }

        plugin.getPrivateMessageManager().setLastMessaged(player, target);

        String spyFormat = plugin.getFormattedConfigString(
                "socialspy.format.spy",
                "&7[&cSpy&7] &b" + sender.getName() + " &7-> &b" + target.getName() + "&7: &f" + message
        );

        for (UUID uuid : plugin.getSocialSpyManager().getSpies()) {
            Player spy = plugin.getServer().getPlayer(uuid);
            if (spy != null && spy.isOnline() && spy.hasPermission("awesomechat.socialspy")) {
                spy.sendMessage(AwesomeChat.formatColors(spyFormat));
            }
        }

        return true;
    }

    /**
     * Builds a PM component with hover and click actions
     */
    private Component buildPMComponent(Player sender, Player receiver, String message, boolean isSender) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("private-messages.hover");
        if (config == null) {
            // Fallback to simple message
            String format = isSender ?
                    "&7[&bYou &7-> &b{receiver}&7] &f{message}" :
                    "&7[&b{sender} &7-> &bYou&7] &f{message}";
            format = format.replace("{sender}", sender.getName())
                    .replace("{receiver}", receiver.getName())
                    .replace("{message}", message);
            return LegacyComponentSerializer.legacySection().deserialize(AwesomeChat.formatColors(format));
        }

        // Parse format string to identify components
        String format = isSender ?
                plugin.getConfig().getString("private-messages.format.sender", "&7[&bYou &7-> &b{receiver}&7] &f{message}") :
                plugin.getConfig().getString("private-messages.format.receiver", "&7[&b{sender} &7-> &bYou&7] &f{message}");

        Component result = Component.empty();
        String[] parts = format.split("(\\{sender\\}|\\{receiver\\}|\\{message\\})");
        String[] placeholders = extractPlaceholders(format);

        int placeholderIndex = 0;
        for (int i = 0; i < parts.length; i++) {
            // Add static text
            if (!parts[i].isEmpty()) {
                result = result.append(parseColoredText(parts[i]));
            }

            // Add placeholder component with hover/click
            if (placeholderIndex < placeholders.length) {
                String placeholder = placeholders[placeholderIndex];
                Component placeholderComp = buildPlaceholderComponent(
                        placeholder, sender, receiver, message, config, isSender
                );
                result = result.append(placeholderComp);
                placeholderIndex++;
            }
        }

        return result;
    }

    /**
     * Builds a component for a placeholder with hover and click actions
     */
    private Component buildPlaceholderComponent(String placeholder, Player sender, Player receiver,
                                                 String message, ConfigurationSection config, boolean isSender) {
        String text;
        List<String> hoverLines = null;
        String clickAction = null;
        String clickType = null;

        switch (placeholder) {
            case "{sender}":
                text = sender.getName();
                hoverLines = config.getStringList("sender-hover");
                clickAction = config.getString("sender-click-action", "/msg %player% ");
                clickType = config.getString("sender-click-type", "suggest");
                clickAction = clickAction.replace("%player%", sender.getName());
                break;
            case "{receiver}":
                text = receiver.getName();
                hoverLines = config.getStringList("receiver-hover");
                clickAction = config.getString("receiver-click-action", "/msg %player% ");
                clickType = config.getString("receiver-click-type", "suggest");
                clickAction = clickAction.replace("%player%", receiver.getName());
                break;
            case "{message}":
                text = message;
                hoverLines = config.getStringList("message-hover");
                clickAction = config.getString("message-click-action", "%message%");
                clickType = config.getString("message-click-type", "copy");
                clickAction = clickAction.replace("%message%", message);
                break;
            default:
                return Component.text(placeholder);
        }

        Component comp = parseColoredText(text);

        // Add hover text
        if (hoverLines != null && !hoverLines.isEmpty()) {
            Component hoverText = Component.empty();
            for (int i = 0; i < hoverLines.size(); i++) {
                String line = hoverLines.get(i)
                        .replace("%sender%", sender.getName())
                        .replace("%receiver%", receiver.getName())
                        .replace("%player%", isSender ? receiver.getName() : sender.getName())
                        .replace("%message%", message);
                hoverText = hoverText.append(parseColoredText(line));
                if (i < hoverLines.size() - 1) {
                    hoverText = hoverText.append(Component.newline());
                }
            }
            comp = comp.hoverEvent(HoverEvent.showText(hoverText));
        }

        // Add click action
        if (clickAction != null && !clickAction.isEmpty() && clickType != null) {
            ClickEvent.Action action = switch (clickType.toLowerCase()) {
                case "execute", "run_command" -> ClickEvent.Action.RUN_COMMAND;
                case "suggest", "suggest_command" -> ClickEvent.Action.SUGGEST_COMMAND;
                case "copy", "copy_to_clipboard" -> ClickEvent.Action.COPY_TO_CLIPBOARD;
                case "open_url" -> ClickEvent.Action.OPEN_URL;
                default -> ClickEvent.Action.SUGGEST_COMMAND;
            };
            comp = comp.clickEvent(ClickEvent.clickEvent(action, clickAction));
        }

        return comp;
    }

    /**
     * Extracts placeholders from a format string
     */
    private String[] extractPlaceholders(String format) {
        return format.replaceAll("[^{}]+(?![^{]*})", "")
                .split("(?<=})(?=\\{)");
    }

    /**
     * Parses colored text (supports both MiniMessage and legacy codes)
     */
    private Component parseColoredText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Check if MiniMessage is enabled and text contains MiniMessage tags
        boolean miniMessageEnabled = plugin.getConfig().getBoolean("minimessage.enabled", false);
        if (miniMessageEnabled && (text.contains("<gradient") || (text.contains("<") && text.contains(">")))) {
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (Exception e) {
                // Fallback to legacy parsing
            }
        }

        // Parse as legacy color codes (including hex)
        String formatted = AwesomeChat.formatColors(text);
        return LegacyComponentSerializer.legacySection().deserialize(formatted);
    }
}