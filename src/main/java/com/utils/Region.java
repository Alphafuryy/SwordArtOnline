package com.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class Region {
    private final String regionName;
    private final Location pos1;
    private final Location pos2;
    private String regionType; // "floor", "structure", or custom types
    private String structureType; // specific structure type if this is a structure region
    private final Map<String, Object> settings = new HashMap<>();

    public Region(String regionName, String regionType, Location pos1, Location pos2) {
        this.regionName = regionName;
        this.regionType = regionType;
        this.pos1 = pos1;
        this.pos2 = pos2;
        initializeDefaultSettings();
    }

    // Add this constructor for backward compatibility
    public Region(String floorName, Location pos1, Location pos2) {
        this(floorName, "floor", pos1, pos2);
    }

    private void initializeDefaultSettings() {
        // Default settings for all region types
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

        // Additional settings specific to region types
        if ("floor".equals(regionType)) {
            settings.put("floor-level", 1);
            settings.put("floor-difficulty", "normal");
            settings.put("allow-teleport", true);
        } else if ("structure".equals(regionType)) {
            settings.put("structure-difficulty", "normal");
            settings.put("respawn-enabled", false);
            settings.put("respawn-time", 300); // 5 minutes in seconds
        }
    }

    public String getRegionName() {
        return regionName;
    }

    // Add this method for backward compatibility
    public String getFloorName() {
        return isFloor() ? regionName : null;
    }

    public String getRegionType() {
        return regionType;
    }

    public void setRegionType(String regionType) {
        this.regionType = regionType;
        // Reinitialize settings when type changes
        initializeDefaultSettings();
    }

    public String getStructureType() {
        return structureType;
    }

    public void setStructureType(String structureType) {
        this.structureType = structureType;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public Map<String, Object> getSettings() {
        return new HashMap<>(settings); // Return copy to prevent external modification
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public boolean hasSetting(String key) {
        return settings.containsKey(key);
    }

    public boolean getBooleanSetting(String key) {
        Object value = settings.get(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public String getStringSetting(String key) {
        Object value = settings.get(key);
        return value != null ? value.toString() : "";
    }

    public int getIntSetting(String key) {
        Object value = settings.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public boolean isInside(Location loc) {
        if (loc == null || pos1 == null || pos2 == null) {
            return false;
        }

        if (!loc.getWorld().equals(pos1.getWorld())) {
            return false;
        }

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

    public Location getCenter() {
        if (pos1 == null || pos2 == null) {
            return null;
        }

        double centerX = (pos1.getX() + pos2.getX()) / 2;
        double centerY = (pos1.getY() + pos2.getY()) / 2;
        double centerZ = (pos1.getZ() + pos2.getZ()) / 2;

        return new Location(pos1.getWorld(), centerX, centerY, centerZ);
    }

    public double getVolume() {
        if (pos1 == null || pos2 == null) {
            return 0;
        }

        double width = Math.abs(pos1.getX() - pos2.getX());
        double height = Math.abs(pos1.getY() - pos2.getY());
        double length = Math.abs(pos1.getZ() - pos2.getZ());

        return width * height * length;
    }

    public boolean isFloor() {
        return "floor".equals(regionType);
    }

    public boolean isStructure() {
        return "structure".equals(regionType);
    }

    public boolean overlaps(Region other) {
        if (other == null || pos1 == null || pos2 == null || other.pos1 == null || other.pos2 == null) {
            return false;
        }

        if (!pos1.getWorld().equals(other.pos1.getWorld())) {
            return false;
        }

        // Check if regions overlap in any dimension
        boolean xOverlap = (pos1.getX() <= other.pos2.getX() && pos2.getX() >= other.pos1.getX());
        boolean yOverlap = (pos1.getY() <= other.pos2.getY() && pos2.getY() >= other.pos1.getY());
        boolean zOverlap = (pos1.getZ() <= other.pos2.getZ() && pos2.getZ() >= other.pos1.getZ());

        return xOverlap && yOverlap && zOverlap;
    }

    public boolean contains(Region other) {
        if (other == null || pos1 == null || pos2 == null || other.pos1 == null || other.pos2 == null) {
            return false;
        }

        if (!pos1.getWorld().equals(other.pos1.getWorld())) {
            return false;
        }

        // Check if this region completely contains the other region
        return (pos1.getX() <= other.pos1.getX() && pos2.getX() >= other.pos2.getX() &&
                pos1.getY() <= other.pos1.getY() && pos2.getY() >= other.pos2.getY() &&
                pos1.getZ() <= other.pos1.getZ() && pos2.getZ() >= other.pos2.getZ());
    }

    public String serializeWorld() {
        return pos1 != null ? pos1.getWorld().getName() : "";
    }

    public static Location deserializeLoc(String world, double x, double y, double z) {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    public static Location deserializeLoc(String world, double x, double y, double z, float yaw, float pitch) {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return String.format("Region{name='%s', type='%s', structureType='%s', world='%s', bounds=[%s -> %s]}",
                regionName,
                regionType,
                structureType != null ? structureType : "N/A",
                pos1 != null ? pos1.getWorld().getName() : "null",
                formatLocation(pos1),
                formatLocation(pos2));
    }

    private String formatLocation(Location loc) {
        if (loc == null) {
            return "null";
        }
        return String.format("(%d, %d, %d)", (int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Region other = (Region) obj;
        return regionName.equals(other.regionName) &&
                regionType.equals(other.regionType) &&
                (structureType == null ? other.structureType == null : structureType.equals(other.structureType));
    }

    @Override
    public int hashCode() {
        int result = regionName.hashCode();
        result = 31 * result + regionType.hashCode();
        result = 31 * result + (structureType != null ? structureType.hashCode() : 0);
        return result;
    }

    // Utility methods for common region operations
    public boolean allowsPvP() {
        return getBooleanSetting("pvp");
    }

    public boolean allowsEntry() {
        return getBooleanSetting("entry-allowed");
    }

    public boolean allowsMobSpawning() {
        return getBooleanSetting("mob-spawning");
    }

    public boolean allowsBlockBreak() {
        return getBooleanSetting("block-break");
    }

    public boolean allowsBlockPlace() {
        return getBooleanSetting("block-place");
    }

    public String getEntryMessage() {
        return getStringSetting("entry-message");
    }

    public String getExitMessage() {
        return getStringSetting("exit-message");
    }

    public boolean shouldTeleportOnEntry() {
        return getBooleanSetting("teleport-on-entry");
    }

    public Location getTeleportLocation() {
        Object location = getSetting("teleport-location");
        return location instanceof Location ? (Location) location : getCenter();
    }

    // Method to apply settings from a structure type template
    public void applyStructureTypeSettings(Map<String, Object> structureSettings) {
        if (structureSettings != null) {
            for (Map.Entry<String, Object> entry : structureSettings.entrySet()) {
                setSetting(entry.getKey(), entry.getValue());
            }
        }
    }

    // Method to validate if the region has valid bounds
    public boolean isValid() {
        return pos1 != null && pos2 != null &&
                pos1.getWorld() != null && pos2.getWorld() != null &&
                pos1.getWorld().equals(pos2.getWorld()) &&
                regionName != null && !regionName.trim().isEmpty() &&
                regionType != null && !regionType.trim().isEmpty();
    }

    // Method to get the minimum point of the region
    public Location getMinPoint() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        return new Location(
                pos1.getWorld(),
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
    }

    // Method to get the maximum point of the region
    public Location getMaxPoint() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        return new Location(
                pos1.getWorld(),
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
    }
}