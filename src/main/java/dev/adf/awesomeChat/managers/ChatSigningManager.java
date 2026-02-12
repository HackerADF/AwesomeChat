package dev.adf.awesomeChat.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class ChatSigningManager {

    private final JavaPlugin plugin;

    public ChatSigningManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setEnforceSecureProfile(boolean enabled) {
        Path path = Path.of("server.properties");

        if (!Files.exists(path)) {
            plugin.getLogger().warning("server.properties not found!");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            List<String> updated = new ArrayList<>();

            boolean found = false;

            for (String line : lines) {
                if (line.startsWith("enforce-secure-profile=")) {
                    updated.add("enforce-secure-profile=" + enabled);
                    found = true;
                } else {
                    updated.add(line);
                }
            }

            if (!found) {
                updated.add("enforce-secure-profile=" + enabled);
            }

            Files.write(path, updated, StandardOpenOption.TRUNCATE_EXISTING);

            plugin.getLogger().info("Updated enforce-secure-profile to " + enabled + ".");
            plugin.getLogger().warning("Server restart required for changes to take effect.");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to modify server.properties!");
            e.printStackTrace();
        }
    }

    // probably will not use
    public boolean restartServer() {
        try {
            Bukkit.spigot().restart();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Restart failed. Manual restart required.");
            return false;
        }
    }
}