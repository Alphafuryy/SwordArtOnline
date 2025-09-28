package com.managers;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SoundManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private File configFile;

    private final Map<String, Map<String, SoundData>> sounds = new HashMap<>();

    public SoundManager(Plugin plugin) {
        this.plugin = plugin;
        setupConfig();
        loadSounds();
    }

    private void setupConfig() {
        // Create plugins/YourPlugin/sounds.yml if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "sounds.yml");

        if (!configFile.exists()) {
            try (InputStream in = plugin.getResource("sounds.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("Created default sounds.yml");
                } else {
                    // Create a basic config if resource doesn't exist
                    configFile.createNewFile();
                    plugin.getLogger().warning("sounds.yml not found in resources, creating empty file");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create sounds.yml", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadSounds() {
        sounds.clear();

        if (config.getKeys(false).isEmpty()) {
            plugin.getLogger().warning("sounds.yml is empty or contains no valid sound configurations!");
            return;
        }

        for (String category : config.getKeys(false)) {
            if (!config.isConfigurationSection(category)) {
                plugin.getLogger().warning("Invalid configuration section for category: " + category);
                continue;
            }

            Map<String, SoundData> actionMap = new HashMap<>();
            for (String action : config.getConfigurationSection(category).getKeys(false)) {
                String path = category + "." + action;

                if (!config.isConfigurationSection(path)) {
                    plugin.getLogger().warning("Invalid configuration section for: " + path);
                    continue;
                }

                String soundName = config.getString(path + ".sound", "BLOCK_NOTE_BLOCK_BASS");
                float volume = (float) config.getDouble(path + ".volume", 1.0);
                float pitch = (float) config.getDouble(path + ".pitch", 1.0);

                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    actionMap.put(action.toLowerCase(), new SoundData(sound, volume, pitch));
                    plugin.getLogger().info("Loaded sound: " + category + "." + action + " -> " + soundName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound name: " + soundName + " for " + path);
                    // Fallback to a default sound
                    try {
                        Sound fallback = Sound.BLOCK_NOTE_BLOCK_BASS;
                        actionMap.put(action.toLowerCase(), new SoundData(fallback, volume, pitch));
                    } catch (IllegalArgumentException e2) {
                        plugin.getLogger().severe("Even fallback sound failed! This should not happen.");
                    }
                }
            }
            sounds.put(category.toLowerCase(), actionMap);
        }

        if (sounds.isEmpty()) {
            plugin.getLogger().warning("No sounds were loaded from sounds.yml!");
        } else {
            plugin.getLogger().info("Loaded " + sounds.size() + " sound categories");
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadSounds();
    }

    public void play(Player player, String category, String action) {
        if (player == null || !player.isOnline()) return;

        category = category.toLowerCase();
        action = action.toLowerCase();

        if (!sounds.containsKey(category)) {
            plugin.getLogger().warning("Sound category not found: " + category);
            return;
        }

        SoundData data = sounds.get(category).get(action);
        if (data == null) {
            plugin.getLogger().warning("Sound action not found: " + category + "." + action);
            return;
        }

        player.playSound(player.getLocation(), data.sound, data.volume, data.pitch);
    }

    private static class SoundData {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundData(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}