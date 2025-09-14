package com.managers;

import com.SwordArtOnline;
import com.utils.Floor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FloorManager {

    private final SwordArtOnline plugin;
    private final Map<String, Floor> floors = new HashMap<>();

    public FloorManager(SwordArtOnline plugin) {
        this.plugin = plugin;
        loadFloors();
    }

    public void loadFloors() {
        File folder = new File(plugin.getDataFolder(), "Spawns");
        if (!folder.exists()) folder.mkdirs();

        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                floors.put(f.getName(), new Floor(f.getName(), f));
            }
        }
    }

    public Floor getFloor(String name) {
        return floors.get(name);
    }

    public Floor createFloor(String name) {
        File folder = new File(plugin.getDataFolder() + "/Spawns/" + name);
        if (!folder.exists()) folder.mkdirs();

        Floor floor = new Floor(name, folder);
        floors.put(name, floor);
        return floor;
    }

    // <-- Add this getter
    public Map<String, Floor> getFloors() {
        return floors;
    }
}
