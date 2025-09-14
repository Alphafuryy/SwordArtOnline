package com.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Floor {
    private String name;
    private File folder;
    private List<String> spawnNames = new ArrayList<>();

    public Floor(String name, File folder) {
        this.name = name;
        this.folder = folder;
        loadSpawnNames(); // ADD THIS LINE
    }

    public String getName() { return name; }
    public File getFolder() { return folder; }
    public List<String> getSpawnNames() { return spawnNames; }

    public void addSpawn(String spawnName) {
        if (!spawnNames.contains(spawnName)) spawnNames.add(spawnName);
    }

    // ADD THIS METHOD
    private void loadSpawnNames() {
        spawnNames.clear();
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    String spawnName = file.getName().replace(".yml", "");
                    spawnNames.add(spawnName);
                }
            }
        }
    }

    // OPTIONAL: Add a method to reload spawn names
    public void reloadSpawnNames() {
        loadSpawnNames();
    }
}