package com.managers;

import com.SwordArtOnline;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager {

    private final SwordArtOnline plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    public MessagesManager(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    /**
     * Load a message file (creates it if it doesn't exist)
     * @param name The file name without extension (e.g., "spawn" or "region")
     */
    public void loadMessages(String name) {
        File folder = new File(plugin.getDataFolder(), "Messages");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, name + ".yml");
        if (!file.exists()) {
            plugin.saveResource("Messages/" + name + ".yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Load defaults from resource as fallback
        try (InputStream in = plugin.getResource("Messages/" + name + ".yml")) {
            if (in != null) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
                cfg.setDefaults(defaultConfig);
                cfg.options().copyDefaults(true);
                cfg.save(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        configs.put(name, cfg);
        files.put(name, file);
    }

    public String getMessage(String key) {
        return getMessage("spawn", key); // default to spawn.yml
    }

    public String getMessage(String fileName, String key) {
        FileConfiguration cfg = configs.get(fileName);
        if (cfg == null) return key; // fallback to key
        return cfg.getString(key, key);
    }
}
