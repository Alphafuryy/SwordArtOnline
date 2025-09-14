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
    }

    public String getName() { return name; }
    public File getFolder() { return folder; }
    public List<String> getSpawnNames() { return spawnNames; }

    public void addSpawn(String spawnName) {
        if (!spawnNames.contains(spawnName)) spawnNames.add(spawnName);
    }
}
