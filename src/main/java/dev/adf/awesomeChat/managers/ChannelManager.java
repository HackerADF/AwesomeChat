package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

import static dev.adf.awesomeChat.listeners.ChatListener.formatColors;

public class ChannelManager {

    private final AwesomeChat plugin;
    private final Map<String, ChatChannel> channels = new LinkedHashMap<>();
    private final Map<UUID, String> activeChannel = new HashMap<>();
    private final List<String> registeredCommands = new ArrayList<>();

    public ChannelManager(AwesomeChat plugin) {
        this.plugin = plugin;
        loadChannels();
    }

    public void loadChannels() {
        channels.clear();
        unregisterChannelCommands();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("channels");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection ch = section.getConfigurationSection(key);
            if (ch == null) continue;

            String displayName = ch.getString("display-name", key);
            String prefix = ch.getString("prefix", "&8[&b" + displayName + "&8]&r ");
            String permission = ch.getString("permission", "");
            String format = ch.getString("format", "{prefix}{player}&7: &f{message}");
            String soundName = ch.getString("sound", "none");
            float volume = (float) ch.getDouble("volume", 1.0);
            float pitch = (float) ch.getDouble("pitch", 1.0);
            boolean autoJoin = ch.getBoolean("auto-join", false);
            String command = ch.getString("command", "none");
            String joinMessage = ch.getString("join-message", "&aSwitched to channel &f{channel}&a. All messages will go to this channel.");
            String leaveMessage = ch.getString("leave-message", "&eLeft channel &f{channel}&e. You are now in global chat.");
            boolean alertsEnabled = ch.getBoolean("alerts.enabled", false);
            String alertJoin = ch.getString("alerts.join", "&7[&a+&7] &f{player} &7joined &f{channel}&7.");
            String alertLeave = ch.getString("alerts.leave", "&7[&c-&7] &f{player} &7left &f{channel}&7.");

            ChatChannel channel = new ChatChannel(key, displayName, prefix, permission, format, soundName, volume, pitch, autoJoin, command, joinMessage, leaveMessage, alertsEnabled, alertJoin, alertLeave);
            channels.put(key.toLowerCase(), channel);
        }

        registerChannelCommands();
    }

    private void registerChannelCommands() {
        var commandMap = Bukkit.getServer().getCommandMap();

        for (ChatChannel channel : channels.values()) {
            String cmd = channel.getCommand();
            if (cmd == null || cmd.equalsIgnoreCase("none") || cmd.isEmpty()) continue;

            // Drop the leading slash if they included one
            if (cmd.startsWith("/")) cmd = cmd.substring(1);

            String commandName = cmd.toLowerCase();
            String channelName = channel.getName();
            String channelPermission = channel.getPermission();

            BukkitCommand bukkitCommand = new BukkitCommand(commandName) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Only players can use this command.");
                        return true;
                    }

                    ChannelManager manager = plugin.getChannelManager();
                    if (manager == null) return true;

                    ChatChannel ch = manager.getChannel(channelName);
                    if (ch == null) return true;

                    if (!manager.hasAccess(player, ch)) {
                        player.sendMessage(plugin.getChatPrefix() + org.bukkit.ChatColor.RED + "You don't have permission to use this channel.");
                        return true;
                    }

                    if (args.length == 0) {
                        // Toggle channel
                        manager.joinChannel(player, ch);
                    } else {
                        // Send one-off message
                        String message = String.join(" ", args);
                        manager.sendToChannel(player, channelName, message);
                    }

                    return true;
                }
            };

            bukkitCommand.setDescription("Toggle or send messages to the " + channel.getDisplayName() + " channel");
            bukkitCommand.setUsage("/" + commandName + " [message]");
            if (!channelPermission.isEmpty()) {
                bukkitCommand.setPermission(channelPermission);
            }

            commandMap.register("awesomechat", bukkitCommand);
            registeredCommands.add("awesomechat:" + commandName);
            registeredCommands.add(commandName);
        }
    }

    private void unregisterChannelCommands() {
        if (registeredCommands.isEmpty()) return;

        var commandMap = Bukkit.getServer().getCommandMap();
        var knownCommands = commandMap.getKnownCommands();

        for (String cmd : registeredCommands) {
            Command existing = knownCommands.get(cmd);
            if (existing != null) {
                existing.unregister(commandMap);
                knownCommands.remove(cmd);
            }
        }
        registeredCommands.clear();
    }

    public ChatChannel getChannel(String name) {
        return channels.get(name.toLowerCase());
    }

    public Collection<ChatChannel> getAllChannels() {
        return Collections.unmodifiableCollection(channels.values());
    }

    public List<ChatChannel> getAccessibleChannels(Player player) {
        List<ChatChannel> accessible = new ArrayList<>();
        for (ChatChannel ch : channels.values()) {
            if (ch.getPermission().isEmpty() || player.hasPermission(ch.getPermission())) {
                accessible.add(ch);
            }
        }
        return accessible;
    }

    public boolean hasAccess(Player player, ChatChannel channel) {
        return channel.getPermission().isEmpty() || player.hasPermission(channel.getPermission());
    }

    public void setActiveChannel(Player player, String channelName) {
        if (channelName == null) {
            activeChannel.remove(player.getUniqueId());
        } else {
            activeChannel.put(player.getUniqueId(), channelName.toLowerCase());
        }
    }

    /**
     * Join a channel with configurable messages and alerts.
     * Returns true if the player was switched, false if they were already in this channel (toggled off).
     */
    public boolean joinChannel(Player player, ChatChannel channel) {
        String current = getActiveChannel(player);
        if (channel.getName().equalsIgnoreCase(current)) {
            // Already in this channel, so toggle out
            leaveChannel(player);
            return false;
        }

        // If they're in a different channel, leave that one first
        if (current != null) {
            ChatChannel oldChannel = getChannel(current);
            if (oldChannel != null) {
                sendLeaveAlert(player, oldChannel);
            }
        }

        setActiveChannel(player, channel.getName());

        // Send join message to the player
        String joinMsg = channel.getJoinMessage()
                .replace("{player}", player.getName())
                .replace("{channel}", channel.getDisplayName());
        player.sendMessage(plugin.getChatPrefix() + formatColors(joinMsg));

        // Send alert to channel members
        sendJoinAlert(player, channel);

        return true;
    }

    /**
     * Leave the current channel with configurable messages and alerts.
     */
    public void leaveChannel(Player player) {
        String current = getActiveChannel(player);
        if (current == null) return;

        ChatChannel channel = getChannel(current);
        setActiveChannel(player, null);

        if (channel != null) {
            String leaveMsg = channel.getLeaveMessage()
                    .replace("{player}", player.getName())
                    .replace("{channel}", channel.getDisplayName());
            player.sendMessage(plugin.getChatPrefix() + formatColors(leaveMsg));

            sendLeaveAlert(player, channel);
        }
    }

    private void sendJoinAlert(Player player, ChatChannel channel) {
        if (!channel.isAlertsEnabled()) return;

        String alert = channel.getAlertJoin()
                .replace("{player}", player.getName())
                .replace("{channel}", channel.getDisplayName());
        String formatted = formatColors(alert);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && hasAccess(target, channel)) {
                target.sendMessage(formatted);
            }
        }
    }

    private void sendLeaveAlert(Player player, ChatChannel channel) {
        if (!channel.isAlertsEnabled()) return;

        String alert = channel.getAlertLeave()
                .replace("{player}", player.getName())
                .replace("{channel}", channel.getDisplayName());
        String formatted = formatColors(alert);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && hasAccess(target, channel)) {
                target.sendMessage(formatted);
            }
        }
    }

    public String getActiveChannel(Player player) {
        return activeChannel.get(player.getUniqueId());
    }

    public boolean hasActiveChannel(Player player) {
        return activeChannel.containsKey(player.getUniqueId());
    }

    public void sendToChannel(Player sender, String channelName, String message) {
        ChatChannel channel = getChannel(channelName);
        if (channel == null) return;

        String formattedPrefix = formatColors(channel.getPrefix());
        String senderPrefix = "";
        if (plugin.isPluginEnabled("LuckPerms")) {
            senderPrefix = formatColors(dev.adf.awesomeChat.utils.LuckPermsUtil.getPlayerPrefix(sender));
        }

        String formatted = channel.getFormat()
                .replace("{channel}", channel.getDisplayName())
                .replace("{channel-prefix}", formattedPrefix)
                .replace("{prefix}", senderPrefix)
                .replace("{player}", sender.getName())
                .replace("{message}", message);

        formatted = formatColors(formatted);

        String chatPrefix = formatColors(channel.getPrefix());
        String fullMessage = chatPrefix + formatted;

        Sound sound = null;
        if (!channel.getSoundName().equalsIgnoreCase("none")) {
            try {
                sound = Sound.valueOf(channel.getSoundName().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid sound '" + channel.getSoundName() + "' for channel " + channelName);
            }
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (hasAccess(target, channel)) {
                target.sendMessage(fullMessage);
                if (sound != null) {
                    target.playSound(target.getLocation(), sound, channel.getVolume(), channel.getPitch());
                }
            }
        }

        Bukkit.getConsoleSender().sendMessage("[" + channel.getDisplayName() + "] " + sender.getName() + ": " + message);

        ChatLogManager chatLogManager = plugin.getChatLogManager();
        if (chatLogManager != null) {
            chatLogManager.logMessage(sender.getUniqueId(), sender.getName(), message, channelName, false);
        }
    }

    public void removePlayer(UUID playerId) {
        activeChannel.remove(playerId);
    }

    public static class ChatChannel {
        private final String name;
        private final String displayName;
        private final String prefix;
        private final String permission;
        private final String format;
        private final String soundName;
        private final float volume;
        private final float pitch;
        private final boolean autoJoin;
        private final String command;
        private final String joinMessage;
        private final String leaveMessage;
        private final boolean alertsEnabled;
        private final String alertJoin;
        private final String alertLeave;

        public ChatChannel(String name, String displayName, String prefix, String permission, String format, String soundName, float volume, float pitch, boolean autoJoin, String command, String joinMessage, String leaveMessage, boolean alertsEnabled, String alertJoin, String alertLeave) {
            this.name = name;
            this.displayName = displayName;
            this.prefix = prefix;
            this.permission = permission;
            this.format = format;
            this.soundName = soundName;
            this.volume = volume;
            this.pitch = pitch;
            this.autoJoin = autoJoin;
            this.command = command;
            this.joinMessage = joinMessage;
            this.leaveMessage = leaveMessage;
            this.alertsEnabled = alertsEnabled;
            this.alertJoin = alertJoin;
            this.alertLeave = alertLeave;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getPrefix() { return prefix; }
        public String getPermission() { return permission; }
        public String getFormat() { return format; }
        public String getSoundName() { return soundName; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
        public boolean isAutoJoin() { return autoJoin; }
        public String getCommand() { return command; }
        public String getJoinMessage() { return joinMessage; }
        public String getLeaveMessage() { return leaveMessage; }
        public boolean isAlertsEnabled() { return alertsEnabled; }
        public String getAlertJoin() { return alertJoin; }
        public String getAlertLeave() { return alertLeave; }
    }
}
