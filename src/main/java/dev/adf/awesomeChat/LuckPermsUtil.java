package dev.adf.awesomeChat;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

public class LuckPermsUtil {

    private static LuckPerms luckPerms;

    static {
        if (LuckPermsProvider.get() != null) {
            luckPerms = LuckPermsProvider.get();
        }
    }

    // This method returns the players LuckPerms group (parent)
    public static String getPlayerGroup(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getPrimaryGroup();
        }
        return "default";  // Default group if not found
    }

    // This method returns the players LuckPerms prefix
    public static String getPlayerPrefix(Player player) {
        if (luckPerms == null) {
            return "";
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getPrefix();
        }
        return "";
    }

    // This method returns the players LuckPerms suffix
    public static String getPlayerSuffix(Player player) {
        if (luckPerms == null) {
            return "";
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getSuffix();
        }
        return "";
    }
}
