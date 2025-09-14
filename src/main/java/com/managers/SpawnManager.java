package com.managers;

import com.SwordArtOnline;
import com.utils.Floor;
import com.utils.Region;
import com.utils.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SpawnManager {

    private final SwordArtOnline plugin;

    public SpawnManager(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    public boolean deleteSpawn(Floor floor, String spawnName) {
        File spawnFile = new File(floor.getFolder(), spawnName + ".yml");
        if (!spawnFile.exists()) return false;

        boolean deleted = spawnFile.delete();
        if (deleted) floor.getSpawnNames().remove(spawnName);
        return deleted;
    }

    public SpawnPoint getNearestSpawn(Location loc) {
        Region region = plugin.getRegionManager().getRegionForLocation(loc);
        if (region == null) {
            plugin.getLogger().info("No region found for player location: " + loc);
            return null;
        }

        String floorName = region.getFloorName();
        Floor floor = plugin.getFloorManager().getFloor(floorName);
        if (floor == null) {
            plugin.getLogger().info("No floor found for region: " + floorName);
            return null;
        }

        SpawnPoint nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (String spawnName : floor.getSpawnNames()) {
            SpawnPoint spawn = getSpawn(floor, spawnName);
            if (spawn == null) continue;

            if (!region.isInside(spawn.getLocation())) continue;

            double distance = spawn.getLocation().distanceSquared(loc);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = spawn;
            }
        }
        return nearest;
    }

    public void setSpawn(Floor floor, String spawnName, Location location) {
        File spawnFile = new File(floor.getFolder(), spawnName + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(spawnFile);

        cfg.set("world", location.getWorld().getName());
        cfg.set("x", location.getX());
        cfg.set("y", location.getY());
        cfg.set("z", location.getZ());
        cfg.set("yaw", location.getYaw());
        cfg.set("pitch", location.getPitch());

        try {
            cfg.save(spawnFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        floor.addSpawn(spawnName);
        // Remove this: plugin.getFloorManager().loadFloors(); // refresh
    }

    public SpawnPoint getSpawn(Floor floor, String spawnName) {
        File spawnFile = new File(floor.getFolder(), spawnName + ".yml");

        // Add debug logging
        plugin.getLogger().info("Looking for spawn file: " + spawnFile.getAbsolutePath());
        plugin.getLogger().info("File exists: " + spawnFile.exists());

        if (!spawnFile.exists()) {
            plugin.getLogger().warning("Spawn file not found: " + spawnName + ".yml in floor " + floor.getName());
            return null;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(spawnFile);

        // Check if world exists
        String worldName = cfg.getString("world");
        if (worldName == null) {
            plugin.getLogger().warning("World not specified in spawn file: " + spawnName);
            return null;
        }

        if (Bukkit.getWorld(worldName) == null) {
            plugin.getLogger().warning("World not found: " + worldName + " for spawn: " + spawnName);
            return null;
        }

        Location loc = new Location(
                Bukkit.getWorld(worldName),
                cfg.getDouble("x"),
                cfg.getDouble("y"),
                cfg.getDouble("z"),
                (float) cfg.getDouble("yaw"),
                (float) cfg.getDouble("pitch")
        );

        return new SpawnPoint(spawnName, loc, floor);
    }
}