package com.utils;

import org.bukkit.Location;

public class SpawnPoint {
    private String name;
    private Location location;
    private Floor floor; // Add floor reference

    public SpawnPoint(String name, Location location, Floor floor) {
        this.name = name;
        this.location = location;
        this.floor = floor;
    }

    public String getName() { return name; }
    public Location getLocation() { return location; }
    public Floor getFloor() { return floor; } // NEW
}
