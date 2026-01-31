package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

import static dev.adf.awesomeChat.listeners.ChatListener.formatColors;

public class ChannelManager {

    private final AwesomeChat plugin;
    private final Map<String, ChatChannel> channels = new LinkedHashMap<>();
    private final Map<UUID, String> activeChannel = new HashMap<>();

    public ChannelManager(AwesomeChat plugin) {
        this.plugin = plugin;
        loadChannels();
    }

    public void loadChannels() {
        channels.clear();

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

            ChatChannel channel = new ChatChannel(key, displayName, prefix, permission, format, soundName, volume, pitch, autoJoin);
            channels.put(key.toLowerCase(), channel);
        }
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

        public ChatChannel(String name, String displayName, String prefix, String permission, String format, String soundName, float volume, float pitch, boolean autoJoin) {
            this.name = name;
            this.displayName = displayName;
            this.prefix = prefix;
            this.permission = permission;
            this.format = format;
            this.soundName = soundName;
            this.volume = volume;
            this.pitch = pitch;
            this.autoJoin = autoJoin;
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
    }
}
