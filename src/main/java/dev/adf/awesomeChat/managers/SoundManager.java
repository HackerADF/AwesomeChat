package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.utils.LuckPermsUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SoundManager {

    private final AwesomeChat plugin;

    public SoundManager(AwesomeChat plugin) {
        this.plugin = plugin;
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
            target.playSound(target.getLocation(), soundConfig.getSound(), soundConfig.getVolume(), soundConfig.getPitch());
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
