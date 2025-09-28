package com.managers;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StatsManager {

    public final Map<String, Attribute> attributeMap = new HashMap<>();
    private final Map<String, Boolean> upgradeableMap = new HashMap<>();
    private final File configFile;
    private final JavaPlugin plugin; // add this line

    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin; // assign it here

        this.configFile = new File(plugin.getDataFolder(), "stats.yml");

        if (!configFile.exists()) {
            plugin.saveResource("stats.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        loadStats(config);
    }

    private void loadStats(FileConfiguration config) {
        attributeMap.clear();
        upgradeableMap.clear();

        if (!config.isConfigurationSection("Stats")) {
            plugin.getLogger().warning("No 'Stats' section found in stats.yml");
            return;
        }

        for (String key : config.getConfigurationSection("Stats").getKeys(false)) {
            String attrName = config.getString("Stats." + key + ".attribute");
            boolean upgradeable = config.getBoolean("Stats." + key + ".upgradeable", true);

            if (attrName == null) {
                plugin.getLogger().warning("Missing attribute for stat: " + key);
                continue;
            }

            try {
                Attribute attribute = Attribute.valueOf(attrName.toUpperCase());
                attributeMap.put(key.toLowerCase(), attribute);
                upgradeableMap.put(key.toLowerCase(), upgradeable);
                plugin.getLogger().info("Loaded stat: " + key + " -> " + attribute);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid attribute in stats.yml for " + key + ": " + attrName);
            }
        }
    }

    public Attribute getAttribute(String key) {
        return attributeMap.get(key.toLowerCase());
    }

    public boolean isUpgradeable(String key) {
        return upgradeableMap.getOrDefault(key.toLowerCase(), false);
    }

    public boolean isStatSupported(String key) {
        return attributeMap.containsKey(key.toLowerCase());
    }
}