package com.managers;

import com.SwordArtOnline;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessagesManager {

    private final SwordArtOnline plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessagesManager(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        File folder = new File(plugin.getDataFolder(), "Messages");
        if (!folder.exists()) folder.mkdirs();

        messagesFile = new File(folder, "spawn.yml");

        // If it doesn't exist in data folder, copy it from resources
        if (!messagesFile.exists()) {
            plugin.saveResource("Messages/spawn.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from resource as fallback (optional)
        try (InputStream in = plugin.getResource("Messages/spawn.yml")) {
            if (in != null) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
                messagesConfig.setDefaults(defaultConfig);
                messagesConfig.options().copyDefaults(true);
                saveMessages();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String key) {
        String message = messagesConfig.getString(key, key);
        plugin.getLogger().info("Loading message for key '" + key + "': " + message);
        return message;
    }
}