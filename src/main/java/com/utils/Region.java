// Region.java
package com.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class Region {
    private final String regionName;
    private final Location pos1;
    private final Location pos2;
    private String regionType; // "floor", "structure", "safezone", "arena", etc.
    private String floorName; // Only for structure regions
    private String structureType; // Only for structure regions
    private final Map<String, Object> settings = new HashMap<>();

    public Region(String regionName, String regionType, Location pos1, Location pos2) {
        this.regionName = regionName;
        this.regionType = regionType;
        this.pos1 = pos1;
        this.pos2 = pos2;
        initializeDefaultSettings();
    }

    public Region(String regionName, String regionType, String floorName, Location pos1, Location pos2) {
        this.regionName = regionName;
        this.regionType = regionType;
        this.floorName = floorName;
        this.pos1 = pos1;
        this.pos2 = pos2;
        initializeDefaultSettings();
    }

    private void initializeDefaultSettings() {
        settings.put("pvp", true);
        settings.put("entry-allowed", true);
        settings.put("mob-spawning", true);
        settings.put("block-break", true);
        settings.put("block-place", true);
        settings.put("damage-players", true);
        settings.put("damage-mobs", true);
        settings.put("hunger-drain", true);
        settings.put("teleport-on-entry", false);
        settings.put("teleport-location", null);
        settings.put("entry-message", "");
        settings.put("exit-message", "");
    }

    public String getRegionName() {
        return regionName;
    }

    public String getRegionType() {
        return regionType;
    }

    public void setRegionType(String regionType) {
        this.regionType = regionType;
    }


    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public String getStructureType() {
        return structureType;
    }

    public void setStructureType(String structureType) {
        this.structureType = structureType;
    }

    public String getFloorName() {
        return floorName;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }



    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        double xMin = Math.min(pos1.getX(), pos2.getX());
        double xMax = Math.max(pos1.getX(), pos2.getX());
        double yMin = Math.min(pos1.getY(), pos2.getY());
        double yMax = Math.max(pos1.getY(), pos2.getY());
        double zMin = Math.min(pos1.getZ(), pos2.getZ());
        double zMax = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= xMin && loc.getX() <= xMax &&
                loc.getY() >= yMin && loc.getY() <= yMax &&
                loc.getZ() >= zMin && loc.getZ() <= zMax;
    }

    public String serializeWorld() {
        return pos1.getWorld().getName();
    }

    public static Location deserializeLoc(String world, double x, double y, double z) {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
}