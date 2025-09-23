package dev.adf.awesomeChat.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PrivateMessageManager {

    private final Map<UUID, UUID> lastMessaged = new HashMap<>();
    private final Set<UUID> disabledMessages = new HashSet<>();
    private final Set<UUID> socialSpy = new HashSet<>();

    public void setLastMessaged(Player sender, Player receiver) {
        lastMessaged.put(sender.getUniqueId(), receiver.getUniqueId());
        lastMessaged.put(receiver.getUniqueId(), sender.getUniqueId()); // mutual
    }

    public Player getLastMessaged(Player player) {
        UUID targetId = lastMessaged.get(player.getUniqueId());
        return targetId != null ? Bukkit.getPlayer(targetId) : null;
    }

    public void setMessagesDisabled(Player player, boolean disabled) {
        UUID id = player.getUniqueId();
        if (disabled) {
            disabledMessages.add(id);
        } else {
            disabledMessages.remove(id);
        }
    }

    public boolean toggleMessages(Player player) {
        UUID id = player.getUniqueId();
        if (disabledMessages.contains(id)) {
            disabledMessages.remove(id);
            return false; // messages enabled
        } else {
            disabledMessages.add(id);
            return true; // messages disabled
        }
    }

    public boolean hasMessagesDisabled(Player player) {
        return disabledMessages.contains(player.getUniqueId());
    }

    public boolean toggleSocialSpy(Player player) {
        UUID id = player.getUniqueId();
        if (socialSpy.contains(id)) {
            socialSpy.remove(id);
            return false; // now disabled
        } else {
            socialSpy.add(id);
            return true; // now enabled
        }
    }

    public boolean hasSocialSpy(Player player) {
        return socialSpy.contains(player.getUniqueId());
    }

    public Set<UUID> getSocialSpyPlayers() {
        return Collections.unmodifiableSet(socialSpy);
    }
}
