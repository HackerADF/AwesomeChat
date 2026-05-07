package dev.adf.awesomeChat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SoundManager {

    private final AwesomeChat plugin;
    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();
    private final Path dataFile;
    private final Gson gson = new GsonBuilder().create();

    public SoundManager(AwesomeChat plugin) {
        this.plugin = plugin;
        this.dataFile = plugin.getDataFolder().toPath().resolve("data").resolve("chat-sounds-disabled.json");
        loadDisabled();
    }

    public void playChatSound(Player sender) {
        FileConfiguration config = plugin.getPluginConfig();

        if (!config.getBoolean("chat-format.sound.enabled", true)) {
            return;
        }

        String senderGroup = LuckPermsUtil.getPlayerGroup(sender);
        SoundConfig soundConfig = getSoundConfig(senderGroup);

        if (soundConfig == null) {
            return;
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (disabledPlayers.contains(target.getUniqueId())) continue;
            target.playSound(target.getLocation(), soundConfig.getSound(), soundConfig.getVolume(), soundConfig.getPitch());
        }
    }

    /** Toggles sounds off/on for the player. Returns true if sounds are now disabled. */
    public boolean toggleDisabled(UUID playerId) {
        boolean nowDisabled;
        if (disabledPlayers.contains(playerId)) {
            disabledPlayers.remove(playerId);
            nowDisabled = false;
        } else {
            disabledPlayers.add(playerId);
            nowDisabled = true;
        }
        saveDisabled();
        return nowDisabled;
    }

    public boolean isDisabled(UUID playerId) {
        return disabledPlayers.contains(playerId);
    }

    private void loadDisabled() {
        if (!Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> list = gson.fromJson(reader, type);
            if (list == null) return;
            for (String s : list) {
                try { disabledPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load chat-sounds-disabled.json: " + e.getMessage());
        }
    }

    private void saveDisabled() {
        try {
            Files.createDirectories(dataFile.getParent());
            List<String> list = new ArrayList<>();
            for (UUID id : disabledPlayers) list.add(id.toString());
            try (Writer writer = Files.newBufferedWriter(dataFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                gson.toJson(list, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chat-sounds-disabled.json: " + e.getMessage());
        }
    }

    private SoundConfig getSoundConfig(String group) {
        FileConfiguration config = plugin.getPluginConfig();

        if (config.getBoolean("chat-format.sound.per-group.enabled", false)) {
            String path = "chat-format.sound.per-group.groups." + group;
            if (config.isConfigurationSection(path)) {
                return loadSoundFromPath(path);
            }
        }

        String globalPath = "chat-format.sound.global";
        if (config.isConfigurationSection(globalPath)) {
            return loadSoundFromPath(globalPath);
        }

        String legacyPath = "chat-format.sound";
        if (config.isConfigurationSection(legacyPath) && config.contains(legacyPath + ".name")) {
            return loadSoundFromPath(legacyPath);
        }

        return null;
    }

    private SoundConfig loadSoundFromPath(String path) {
        FileConfiguration config = plugin.getPluginConfig();

        String soundName = config.getString(path + ".name", "ENTITY_CHICKEN_EGG");
        float volume = (float) config.getDouble(path + ".volume", 100.0);
        float pitch = (float) config.getDouble(path + ".pitch", 2.0);

        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name '" + soundName + "' at path '" + path + "'. Using default.");
            sound = Sound.ENTITY_CHICKEN_EGG;
        }

        return new SoundConfig(sound, volume, pitch);
    }

    public static class SoundConfig {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundConfig(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public Sound getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }
}
