// RegionManager.java
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
    private final Map<String, Region> structureRegions = new HashMap<>();

    public RegionManager(SwordArtOnline plugin) {
        this.plugin = plugin;
        loadRegions();
        loadStructureRegions();
    }

    public void loadRegions() {
        regions.clear();
        File folder = new File(plugin.getDataFolder(), "Regions");
        if (!folder.exists()) folder.mkdirs();

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".yml") && !file.getName().startsWith("structure_")) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                String floor = cfg.getString("floor");
                String world = cfg.getString("world");
                Location pos1 = Region.deserializeLoc(world,
                        cfg.getDouble("pos1.x"), cfg.getDouble("pos1.y"), cfg.getDouble("pos1.z"));
                Location pos2 = Region.deserializeLoc(world,
                        cfg.getDouble("pos2.x"), cfg.getDouble("pos2.y"), cfg.getDouble("pos2.z"));

                Region region = new Region(floor, pos1, pos2);
                regions.put(floor, region);
            }
        }
    }

    public void loadStructureRegions() {
        structureRegions.clear();
        File folder = new File(plugin.getDataFolder(), "Regions");
        if (!folder.exists()) folder.mkdirs();

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile() && file.getName().startsWith("structure_") && file.getName().endsWith(".yml")) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                String structureName = cfg.getString("structureName");
                String floor = cfg.getString("floor");
                String structureType = cfg.getString("structureType");
                String world = cfg.getString("world");
                Location pos1 = Region.deserializeLoc(world,
                        cfg.getDouble("pos1.x"), cfg.getDouble("pos1.y"), cfg.getDouble("pos1.z"));
                Location pos2 = Region.deserializeLoc(world,
                        cfg.getDouble("pos2.x"), cfg.getDouble("pos2.y"), cfg.getDouble("pos2.z"));

                Region region = new Region(floor, pos1, pos2);
                region.setStructureType(structureType);
                structureRegions.put(structureName, region);
            }
        }
    }

    public void saveRegion(Region region) {
        File folder = new File(plugin.getDataFolder(), "Regions");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, region.getFloorName() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("floor", region.getFloorName());
        cfg.set("world", region.getPos1().getWorld().getName());

        cfg.set("pos1.x", region.getPos1().getX());
        cfg.set("pos1.y", region.getPos1().getY());
        cfg.set("pos1.z", region.getPos1().getZ());

        cfg.set("pos2.x", region.getPos2().getX());
        cfg.set("pos2.y", region.getPos2().getY());
        cfg.set("pos2.z", region.getPos2().getZ());

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        regions.put(region.getFloorName(), region);
    }

    public void saveStructureRegion(Region region) {
        File folder = new File(plugin.getDataFolder(), "Regions");
        if (!folder.exists()) folder.mkdirs();

        String structureName = region.getStructureType() + "_" + region.getFloorName();
        File file = new File(folder, "structure_" + structureName + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("structureName", structureName);
        cfg.set("structureType", region.getStructureType());
        cfg.set("floor", region.getFloorName());
        cfg.set("world", region.getPos1().getWorld().getName());

        cfg.set("pos1.x", region.getPos1().getX());
        cfg.set("pos1.y", region.getPos1().getY());
        cfg.set("pos1.z", region.getPos1().getZ());

        cfg.set("pos2.x", region.getPos2().getX());
        cfg.set("pos2.y", region.getPos2().getY());
        cfg.set("pos2.z", region.getPos2().getZ());

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        structureRegions.put(structureName, region);
    }

    public Region getRegion(String floor) {
        return regions.get(floor);
    }

    public Region getStructureRegion(String structureName) {
        return structureRegions.get(structureName);
    }

    public Region getRegionForLocation(Location loc) {
        for (Region region : regions.values()) {
            if (region.isInside(loc)) return region;
        }
        return null;
    }

    public Region getStructureRegionForLocation(Location loc) {
        for (Region region : structureRegions.values()) {
            if (region.isInside(loc)) return region;
        }
        return null;
    }

    public Map<String, Region> getRegions() {
        return regions;
    }

    public Map<String, Region> getStructureRegions() {
        return structureRegions;
    }

    public boolean deleteRegion(String name) {
        Region region = regions.remove(name);
        if (region != null) {
            File file = new File(plugin.getDataFolder(), "Regions/" + name + ".yml");
            return file.delete();
        }
        return false;
    }

    public boolean deleteStructureRegion(String name) {
        Region region = structureRegions.remove(name);
        if (region != null) {
            File file = new File(plugin.getDataFolder(), "Regions/structure_" + name + ".yml");
            return file.delete();
        }
        return false;
    }
}