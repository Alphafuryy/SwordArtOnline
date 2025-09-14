package com.managers;

import com.SwordArtOnline;
import com.utils.Floor;
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
        SpawnPoint nearest = null;
        double minDistance = Double.MAX_VALUE;

        plugin.getLogger().info("Searching for nearest spawn to: " + loc);
        plugin.getLogger().info("Available floors: " + plugin.getFloorManager().getFloors().keySet());

        for (Floor floor : plugin.getFloorManager().getFloors().values()) {
            plugin.getLogger().info("Floor " + floor.getName() + " has spawns: " + floor.getSpawnNames());

            for (String spawnName : floor.getSpawnNames()) {
                SpawnPoint spawn = getSpawn(floor, spawnName);
                if (spawn == null) {
                    plugin.getLogger().warning("Failed to load spawn: " + spawnName + " from floor: " + floor.getName());
                    continue;
                }

                double distance = spawn.getLocation().distanceSquared(loc);
                plugin.getLogger().info("Spawn " + spawnName + " distance: " + distance);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = spawn;
                    plugin.getLogger().info("New nearest spawn: " + spawnName + " at distance: " + distance);
                }
            }
        }

        plugin.getLogger().info("Final nearest spawn: " + (nearest != null ? nearest.getName() : "NONE"));
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