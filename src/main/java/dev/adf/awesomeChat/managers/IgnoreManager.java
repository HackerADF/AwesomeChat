package dev.adf.awesomeChat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class IgnoreManager {

    private final JavaPlugin plugin;
    private final Path dataFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Set<UUID>> ignoreMap = new HashMap<>();

    public IgnoreManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().toPath().resolve("data").resolve("ignores");

        try {
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create ignores directory: " + e.getMessage());
        }

        loadAll();
    }

    public boolean isIgnoring(Player player, Player target) {
        Set<UUID> ignored = ignoreMap.get(player.getUniqueId());
        return ignored != null && ignored.contains(target.getUniqueId());
    }

    public boolean isIgnoring(UUID playerId, UUID targetId) {
        Set<UUID> ignored = ignoreMap.get(playerId);
        return ignored != null && ignored.contains(targetId);
    }

    public boolean toggleIgnore(Player player, Player target) {
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        Set<UUID> ignored = ignoreMap.computeIfAbsent(playerId, k -> new HashSet<>());

        boolean nowIgnored;
        if (ignored.contains(targetId)) {
            ignored.remove(targetId);
            nowIgnored = false;
        } else {
            ignored.add(targetId);
            nowIgnored = true;
        }

        savePlayer(playerId);
        return nowIgnored;
    }

    public void setIgnore(Player player, Player target, boolean ignore) {
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        Set<UUID> ignored = ignoreMap.computeIfAbsent(playerId, k -> new HashSet<>());

        if (ignore) {
            ignored.add(targetId);
        } else {
            ignored.remove(targetId);
        }

        savePlayer(playerId);
    }

    public Set<UUID> getIgnoredPlayers(UUID playerId) {
        return Collections.unmodifiableSet(ignoreMap.getOrDefault(playerId, Collections.emptySet()));
    }

    private void loadAll() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataFolder, "*.json")) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();
                String uuidStr = filename.replace(".json", "");

                try {
                    UUID playerId = UUID.fromString(uuidStr);
                    Set<UUID> ignored = loadPlayer(file);
                    if (ignored != null && !ignored.isEmpty()) {
                        ignoreMap.put(playerId, ignored);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid ignore file: " + filename);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load ignore data: " + e.getMessage());
        }
    }

    private Set<UUID> loadPlayer(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> uuidStrings = gson.fromJson(reader, type);
            if (uuidStrings == null) return new HashSet<>();

            Set<UUID> result = new HashSet<>();
            for (String s : uuidStrings) {
                try {
                    result.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {}
            }
            return result;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load ignore file: " + file.getFileName());
            return new HashSet<>();
        }
    }

    private void savePlayer(UUID playerId) {
        Path file = dataFolder.resolve(playerId.toString() + ".json");
        Set<UUID> ignored = ignoreMap.get(playerId);

        if (ignored == null || ignored.isEmpty()) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored2) {}
            return;
        }

        List<String> uuidStrings = new ArrayList<>();
        for (UUID id : ignored) {
            uuidStrings.add(id.toString());
        }

        try (Writer writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(uuidStrings, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save ignore data for " + playerId + ": " + e.getMessage());
        }
    }
}
