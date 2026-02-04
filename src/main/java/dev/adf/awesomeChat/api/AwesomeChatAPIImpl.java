package dev.adf.awesomeChat.api;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.listeners.ChatListener;
import dev.adf.awesomeChat.managers.ChatFilterManager;
import dev.adf.awesomeChat.managers.PrivateMessageManager;
import dev.adf.awesomeChat.managers.SocialSpyManager;
import dev.adf.awesomeChat.storage.ViolationStorage;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AwesomeChatAPIImpl implements AwesomeChatAPI {

    private static final String API_VERSION = "1.0";
    private final AwesomeChat plugin;

    public AwesomeChatAPIImpl(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAPIVersion() {
        return API_VERSION;
    }

    @Override
    public boolean isModerator(Player player) {
        return player.hasPermission("awesomechat.moderator");
    }

    @Override
    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean canModerate(Player player) {
        return player.hasPermission("awesomechat.moderator")
                || player.hasPermission("awesomechat.moderator.punish")
                || player.hasPermission("awesomechat.moderator.view");
    }

    @Override
    public String getPlayerGroup(Player player) {
        return LuckPermsUtil.getPlayerGroup(player);
    }

    @Override
    public String getPlayerPrefix(Player player) {
        return LuckPermsUtil.getPlayerPrefix(player);
    }

    @Override
    public String getPlayerSuffix(Player player) {
        return LuckPermsUtil.getPlayerSuffix(player);
    }

    @Override
    public String getPlayerMetadata(Player player, String key) {
        return LuckPermsUtil.getChatMeta(player, key);
    }

    @Override
    public List<ViolationStorage.ViolationRecord> getViolations(UUID playerId) {
        return ViolationStorage.getViolations(playerId);
    }

    @Override
    public int getViolationCount(UUID playerId) {
        return ViolationStorage.getViolations(playerId).size();
    }

    @Override
    public List<ViolationStorage.ViolationRecord> getViolationsByRule(UUID playerId, String ruleName) {
        return ViolationStorage.getViolations(playerId).stream()
                .filter(v -> v.getRuleName().equals(ruleName))
                .collect(Collectors.toList());
    }

    @Override
    public void addViolation(UUID playerId, String reason) {
        ViolationStorage.addViolation(playerId, reason);
    }

    @Override
    public void clearViolations(UUID playerId) {
        ViolationStorage.clearViolations(playerId);
    }

    @Override
    public boolean isMessageBlocked(Player player, String message) {
        ChatFilterManager filter = plugin.getChatFilterManager();
        if (filter == null) {
            return false;
        }
        ChatFilterManager.FilterResult result = filter.checkAndCensor(player, message, "check");
        return result.blocked;
    }

    @Deprecated
    public String getFormattedMessage(Player player, String message, String primaryGroup) {
        ChatListener formatter = plugin.getChatListener();

        return "In Progress";
    }

    @Override
    public String getMatchedRule(String message) {
        return null;
    }

    @Override
    public ChatFilterManager getChatFilterManager() {
        return plugin.getChatFilterManager();
    }

    @Override
    public PrivateMessageManager getPrivateMessageManager() {
        return plugin.getPrivateMessageManager();
    }

    @Override
    public SocialSpyManager getSocialSpyManager() {
        return plugin.getSocialSpyManager();
    }
}
