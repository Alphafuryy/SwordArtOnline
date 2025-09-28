package com.tasks;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SmithingTask {

    private final Plugin plugin;
    private final File configFile;
    private YamlConfiguration config;

    private static final Map<String, Integer> starLimits = new HashMap<>();
    private static double priceMultiplierPerStar = 0.25; // default 25%
    private static double buffRatePerStar = 0.10;        // default 10%

    public SmithingTask(Plugin plugin) {
        this.plugin = plugin;

        // Ensure /Tasks folder exists
        File taskFolder = new File(plugin.getDataFolder(), "Tasks");
        if (!taskFolder.exists()) {
            taskFolder.mkdirs();
        }

        // smithing.yml inside /Tasks
        this.configFile = new File(taskFolder, "smithing.yml");

        if (!configFile.exists()) {
            createDefaultConfig();
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadConfigValues();
    }

    private void createDefaultConfig() {
        try {
            configFile.createNewFile();
            YamlConfiguration defaultConfig = new YamlConfiguration();

            defaultConfig.set("star_limits.normal", 3);
            defaultConfig.set("star_limits.rare", 5);
            defaultConfig.set("star_limits.unique", 7);
            defaultConfig.set("star_limits.legendary", 10);

            defaultConfig.set("price_multiplier_per_star", 0.25);
            defaultConfig.set("buff_rate_per_star", 0.10);

            defaultConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create smithing.yml!");
            e.printStackTrace();
        }
    }

    private void loadConfigValues() {
        // Load star limits
        starLimits.clear();
        if (config.contains("star_limits")) {
            for (String key : config.getConfigurationSection("star_limits").getKeys(false)) {
                int limit = config.getInt("star_limits." + key, 3);
                starLimits.put(key.toLowerCase(), limit);
            }
        }

        // Load multipliers
        priceMultiplierPerStar = config.getDouble("price_multiplier_per_star", 0.25);
        buffRatePerStar = config.getDouble("buff_rate_per_star", 0.10);

        plugin.getLogger().info("Smithing config loaded: starLimits=" + starLimits +
                ", priceMultiplier=" + priceMultiplierPerStar +
                ", buffRate=" + buffRatePerStar);
    }

    public static int getMaxStarsForRarity(String rarity) {
        return starLimits.getOrDefault(rarity.toLowerCase(), 3);
    }

    public static double getPriceMultiplierPerStar() {
        return priceMultiplierPerStar;
    }

    public static double getBuffRatePerStar() {
        return buffRatePerStar;
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadConfigValues();
    }
}
