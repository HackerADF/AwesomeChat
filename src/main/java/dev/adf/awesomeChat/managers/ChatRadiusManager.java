package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ChatRadiusManager {

    private final AwesomeChat plugin;

    public ChatRadiusManager(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getPluginConfig().getBoolean("chat-radius.enabled", false);
    }

    public boolean isShout(String message) {
        String prefix = getShoutPrefix();
        return prefix != null && !prefix.isEmpty() && message.startsWith(prefix);
    }

    public String stripShout(String message) {
        String prefix = getShoutPrefix();
        if (prefix != null && !prefix.isEmpty() && message.startsWith(prefix)) {
            return message.substring(prefix.length()).trim();
        }
        return message;
    }

    public String getShoutPrefix() {
        return plugin.getPluginConfig().getString("chat-radius.shout-prefix", "!");
    }

    public String getShoutFormat() {
        return plugin.getPluginConfig().getString("chat-radius.shout-format", "&c[SHOUT] &r");
    }

    public boolean isInRange(Player sender, Player viewer) {
        if (sender.equals(viewer)) return true;

        FileConfiguration config = plugin.getPluginConfig();
        boolean crossWorld = config.getBoolean("chat-radius.cross-world", false);
        if (!crossWorld && !sender.getWorld().equals(viewer.getWorld())) {
            return false;
        }

        double radius = getRadius(sender);
        if (radius < 0) return true; // -1 = unlimited

        if (!sender.getWorld().equals(viewer.getWorld())) {
            return false; // different worlds and cross-world off, already handled above but safe check
        }

        Location senderLoc = sender.getLocation();
        Location viewerLoc = viewer.getLocation();
        double distanceSq = senderLoc.distanceSquared(viewerLoc);
        return distanceSq <= (radius * radius);
    }

    public double getRadius(Player player) {
        FileConfiguration config = plugin.getPluginConfig();

        if (config.getBoolean("chat-radius.per-group.enabled", false)) {
            String group = LuckPermsUtil.getPlayerGroup(player);
            if (group != null && config.contains("chat-radius.per-group.groups." + group)) {
                return config.getDouble("chat-radius.per-group.groups." + group);
            }
        }

        return config.getDouble("chat-radius.default-radius", 100);
    }
}
