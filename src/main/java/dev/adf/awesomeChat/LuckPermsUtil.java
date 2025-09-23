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

    /**
     * Get the player's primary LuckPerms group.
     */
    public static String getPlayerGroup(Player player) {
        if (luckPerms == null) return "default";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getPrimaryGroup();
        }
        return "default";
    }

    /**
     * Get the player's LuckPerms prefix synchronously.
     */
    public static String getPlayerPrefix(Player player) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            QueryOptions options = luckPerms.getContextManager().getStaticQueryOptions();
            String prefix = user.getCachedData().getMetaData(options).getPrefix();
            return prefix != null ? prefix : "";
        }
        return "";
    }

    /**
     * Get the player's LuckPerms suffix synchronously.
     */
    public static String getPlayerSuffix(Player player) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            QueryOptions options = luckPerms.getContextManager().getStaticQueryOptions();
            String suffix = user.getCachedData().getMetaData(options).getSuffix();
            return suffix != null ? suffix : "";
        }
        return "";
    }
}
