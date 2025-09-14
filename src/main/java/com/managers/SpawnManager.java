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

        // Use plugin.getFloorManager() instead of undefined floorManager
        for (Floor floor : plugin.getFloorManager().getFloors().values()) {
            for (String spawnName : floor.getSpawnNames()) {
                SpawnPoint spawn = getSpawn(floor, spawnName);
                if (spawn == null) continue;

                double distance = spawn.getLocation().distanceSquared(loc);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = spawn;
                }
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
        plugin.getFloorManager().loadFloors(); // refresh
    }

    public SpawnPoint getSpawn(Floor floor, String spawnName) {
        File spawnFile = new File(floor.getFolder(), spawnName + ".yml");
        if (!spawnFile.exists()) return null;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(spawnFile);
        Location loc = new Location(
                Bukkit.getWorld(cfg.getString("world")),
                cfg.getDouble("x"),
                cfg.getDouble("y"),
                cfg.getDouble("z"),
                (float) cfg.getDouble("yaw"),
                (float) cfg.getDouble("pitch")
        );

        // Pass the floor to SpawnPoint constructor
        return new SpawnPoint(spawnName, loc, floor);
    }
}
