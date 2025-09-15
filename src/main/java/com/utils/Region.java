// Region.java
package com.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Region {
    private final String floorName;
    private final Location pos1;
    private final Location pos2;
    private String structureType;

    public Region(String floorName, Location pos1, Location pos2) {
        this.floorName = floorName;
        this.pos1 = pos1;
        this.pos2 = pos2;
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

    public String getStructureType() {
        return structureType;
    }

    public void setStructureType(String structureType) {
        this.structureType = structureType;
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