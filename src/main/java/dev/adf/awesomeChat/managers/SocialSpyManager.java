package dev.adf.awesomeChat.managers;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SocialSpyManager {

    private final Set<UUID> spies = new HashSet<>();

    public boolean toggleSpy(Player player) {
        UUID uuid = player.getUniqueId();
        if (spies.contains(uuid)) {
            spies.remove(uuid);
            return false; // now disabled
        } else {
            spies.add(uuid);
            return true; // now enabled
        }
    }

    public boolean hasSpy(Player player) {
        return spies.contains(player.getUniqueId());
    }

    public void setSpy(Player player, boolean enabled) {
        if (enabled) {
            spies.add(player.getUniqueId());
        } else {
            spies.remove(player.getUniqueId());
        }
    }

    public Set<UUID> getSpies() {
        return spies;
    }
}
