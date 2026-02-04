package dev.adf.awesomeChat.api;

import dev.adf.awesomeChat.managers.ChatFilterManager;
import dev.adf.awesomeChat.managers.PrivateMessageManager;
import dev.adf.awesomeChat.managers.SocialSpyManager;
import dev.adf.awesomeChat.storage.ViolationStorage;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface AwesomeChatAPI {

    String getAPIVersion();

    boolean isModerator(Player player);

    boolean hasPermission(Player player, String permission);

    boolean canModerate(Player player);

    String getPlayerGroup(Player player);

    String getPlayerPrefix(Player player);

    String getPlayerSuffix(Player player);

    String getPlayerMetadata(Player player, String key);

    List<ViolationStorage.ViolationRecord> getViolations(UUID playerId);

    int getViolationCount(UUID playerId);

    List<ViolationStorage.ViolationRecord> getViolationsByRule(UUID playerId, String ruleName);

    void addViolation(UUID playerId, String reason);

    void clearViolations(UUID playerId);

    boolean isMessageBlocked(Player player, String message);

    String getFormattedMessage(Player player, String message, String primaryGroup);

    String getMatchedRule(String message);

    ChatFilterManager getChatFilterManager();

    PrivateMessageManager getPrivateMessageManager();

    SocialSpyManager getSocialSpyManager();
}
