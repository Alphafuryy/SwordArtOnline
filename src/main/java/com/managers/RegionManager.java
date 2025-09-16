package com.managers;

import com.SwordArtOnline;
import com.utils.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {
    private final SwordArtOnline plugin;
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<String, Map<String, Object>> structureTypes = new HashMap<>();

    public RegionManager(SwordArtOnline plugin) {
        this.plugin = plugin;
        loadAllRegions();
        loadStructureTypes();
    }

    public void loadAllRegions() {
        regions.clear();
        File folder = new File(plugin.getDataFolder(), "Regions");
        if (!folder.exists()) folder.mkdirs();

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".yml") && !file.getName().equals("structure_types.yml")) {
                loadRegionFromFile(file);
            }
        }
    }

    public void loadStructureTypes() {
        structureTypes.clear();
        File file = new File(plugin.getDataFolder(), "Regions/structure_types.yml");
        if (!file.exists()) {
            createDefaultStructureTypes(file);
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String type : cfg.getKeys(false)) {
            Map<String, Object> settings = new HashMap<>();
            if (cfg.contains(type + ".settings")) {
                for (String key : cfg.getConfigurationSection(type + ".settings").getKeys(false)) {
                    settings.put(key, cfg.get(type + ".settings." + key));
                }
            }
            structureTypes.put(type, settings);
        }
    }

    private void createDefaultStructureTypes(File file) {
        FileConfiguration cfg = new YamlConfiguration();

        // Default structure types with their settings
        Map<String, Map<String, Object>> defaultTypes = new HashMap<>();

        Map<String, Object> dungeonSettings = new HashMap<>();
        dungeonSettings.put("pvp", true);
        dungeonSettings.put("mob-spawning", true);
        dungeonSettings.put("entry-message", "&cYou entered a dangerous dungeon!");
        defaultTypes.put("dungeon", dungeonSettings);

        Map<String, Object> bossSettings = new HashMap<>();
        bossSettings.put("pvp", false);
        bossSettings.put("entry-message", "&6Boss Arena - Prepare for battle!");
        defaultTypes.put("boss", bossSettings);

        Map<String, Object> safezoneSettings = new HashMap<>();
        safezoneSettings.put("pvp", false);
        safezoneSettings.put("mob-spawning", false);
        safezoneSettings.put("entry-message", "&aYou are now in a safe zone");
        defaultTypes.put("safezone", safezoneSettings);

        Map<String, Object> shopSettings = new HashMap<>();
        shopSettings.put("pvp", false);
        shopSettings.put("entry-message", "&eWelcome to the shop!");
        defaultTypes.put("shop", shopSettings);

        for (Map.Entry<String, Map<String, Object>> entry : defaultTypes.entrySet()) {
            for (Map.Entry<String, Object> setting : entry.getValue().entrySet()) {
                cfg.set(entry.getKey() + ".settings." + setting.getKey(), setting.getValue());
            }
        }

        try {
            cfg.save(file);
            structureTypes.putAll(defaultTypes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean createStructureType(String typeName, Map<String, Object> settings) {
        if (structureTypes.containsKey(typeName.toLowerCase())) {
            return false;
        }

        structureTypes.put(typeName.toLowerCase(), settings);
        saveStructureTypes();
        return true;
    }

    public boolean deleteStructureType(String typeName) {
        if (!structureTypes.containsKey(typeName.toLowerCase())) {
            return false;
        }

        structureTypes.remove(typeName.toLowerCase());
        saveStructureTypes();
        return true;
    }

    public boolean updateStructureType(String typeName, Map<String, Object> settings) {
        if (!structureTypes.containsKey(typeName.toLowerCase())) {
            return false;
        }

        structureTypes.put(typeName.toLowerCase(), settings);
        saveStructureTypes();
        return true;
    }

    private void saveStructureTypes() {
        File file = new File(plugin.getDataFolder(), "Regions/structure_types.yml");
        FileConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<String, Map<String, Object>> entry : structureTypes.entrySet()) {
            for (Map.Entry<String, Object> setting : entry.getValue().entrySet()) {
                cfg.set(entry.getKey() + ".settings." + setting.getKey(), setting.getValue());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Map<String, Object>> getStructureTypes() {
        return structureTypes;
    }

    public Map<String, Object> getStructureTypeSettings(String typeName) {
        return structureTypes.get(typeName.toLowerCase());
    }
    private void loadRegionFromFile(File file) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String regionName = cfg.getString("name");
        String regionType = cfg.getString("type", "floor");
        String structureType = cfg.getString("structureType", null);
        String world = cfg.getString("world");

        Location pos1 = Region.deserializeLoc(world,
                cfg.getDouble("pos1.x"), cfg.getDouble("pos1.y"), cfg.getDouble("pos1.z"));
        Location pos2 = Region.deserializeLoc(world,
                cfg.getDouble("pos2.x"), cfg.getDouble("pos2.y"), cfg.getDouble("pos2.z"));

        Region region = new Region(regionName, regionType, pos1, pos2);
        region.setStructureType(structureType);

        // Load settings
        if (cfg.contains("settings")) {
            for (String key : cfg.getConfigurationSection("settings").getKeys(false)) {
                region.setSetting(key, cfg.get("settings." + key));
            }
        }

        regions.put(regionName, region);
    }

    public void saveRegion(Region region) {
        File folder = new File(plugin.getDataFolder(), "Regions");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, region.getRegionName() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        // Basic region data
        cfg.set("name", region.getRegionName());
        cfg.set("type", region.getRegionType());
        cfg.set("structureType", region.getStructureType());
        cfg.set("world", region.getPos1().getWorld().getName());

        // Positions
        cfg.set("pos1.x", region.getPos1().getX());
        cfg.set("pos1.y", region.getPos1().getY());
        cfg.set("pos1.z", region.getPos1().getZ());
        cfg.set("pos2.x", region.getPos2().getX());
        cfg.set("pos2.y", region.getPos2().getY());
        cfg.set("pos2.z", region.getPos2().getZ());

        // Settings
        for (Map.Entry<String, Object> entry : region.getSettings().entrySet()) {
            cfg.set("settings." + entry.getKey(), entry.getValue());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        regions.put(region.getRegionName(), region);
    }

    public Region getRegion(String name) {
        return regions.get(name);
    }

    public Region getRegionForLocation(Location loc) {
        for (Region region : regions.values()) {
            if (region.isInside(loc)) return region;
        }
        return null;
    }

    public Map<String, Region> getRegions() {
        return regions;
    }

    public Map<String, Region> getRegionsByType(String type) {
        Map<String, Region> result = new HashMap<>();
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            if (entry.getValue().getRegionType().equalsIgnoreCase(type)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, Region> getStructureRegions() {
        return getRegionsByType("structure");
    }

    public boolean deleteRegion(String name) {
        Region region = regions.remove(name);
        if (region != null) {
            File file = new File(plugin.getDataFolder(), "Regions/" + name + ".yml");
            return file.delete();
        }
        return false;
    }

    public boolean updateRegionSettings(String regionName, Map<String, Object> newSettings) {
        Region region = regions.get(regionName);
        if (region != null) {
            for (Map.Entry<String, Object> entry : newSettings.entrySet()) {
                region.setSetting(entry.getKey(), entry.getValue());
            }
            saveRegion(region);
            return true;
        }
        return false;
    }
}