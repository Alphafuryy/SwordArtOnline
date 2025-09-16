package com.commands;

import com.listeners.RegionListener;
import com.managers.RegionManager;
import com.utils.LocationSelection;
import com.utils.Messages;
import com.utils.Region;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RegionCommand implements CommandExecutor {

    private final RegionManager regionManager;
    private final RegionListener regionListener;

    public RegionCommand(RegionManager manager, RegionListener listener) {
        this.regionManager = manager;
        this.regionListener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("only-player"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> handleWand(player);
            case "create" -> handleCreate(player, args);
            case "edit" -> handleEdit(player, args);
            case "list" -> handleList(player, args);
            case "delete" -> handleDelete(player, args);
            case "info" -> handleInfo(player, args);
            case "settings" -> handleSettings(player, args);
            case "structure" -> handleStructure(player, args);
            case "floor" -> handleFloor(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Messages.get("region-usage"));
        player.sendMessage(Messages.get("region-wand-usage"));
        player.sendMessage(Messages.get("region-create-usage"));
        player.sendMessage(Messages.get("region-edit-usage"));
        player.sendMessage(Messages.get("region-list-usage"));
        player.sendMessage(Messages.get("region-delete-usage"));
        player.sendMessage(Messages.get("region-info-usage"));
        player.sendMessage(Messages.get("region-settings-usage"));
        player.sendMessage(Messages.get("region-structure-create-usage"));
        player.sendMessage(Messages.get("region-structure-delete-usage"));
        player.sendMessage(Messages.get("region-structure-list-usage"));
        player.sendMessage(Messages.get("region-structure-info-usage"));
        player.sendMessage(Messages.get("region-floor-create-usage"));
        player.sendMessage(Messages.get("region-floor-delete-usage"));
    }

    private void handleStructure(Player player, String[] args) {
        if (!player.hasPermission("sao.region.structure.manage")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Messages.get("structure-usage"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handleStructureCreate(player, args);
            case "delete" -> handleStructureDelete(player, args);
            case "list" -> handleStructureList(player);
            case "info" -> handleStructureInfo(player, args);
            default -> player.sendMessage(Messages.get("structure-usage"));
        }
    }

    private void handleFloor(Player player, String[] args) {
        if (!player.hasPermission("sao.region.floor.manage")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Messages.get("floor-usage"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handleFloorCreate(player, args);
            case "delete" -> handleFloorDelete(player, args);
            default -> player.sendMessage(Messages.get("floor-usage"));
        }
    }

    private void handleFloorCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.get("floor-create-usage"));
            return;
        }

        String floorName = args[2];
        LocationSelection sel = regionListener.getSelection(player);

        if (sel == null || !sel.isComplete()) {
            player.sendMessage(Messages.get("region-selection-incomplete"));
            return;
        }

        // Create a floor region (type = "floor")
        Region floorRegion = new Region(floorName, "floor", sel.getPos1(), sel.getPos2());

        if (regionManager.saveRegion(floorRegion)) {
            player.sendMessage(Messages.get("floor-created", "%name%", floorName));
        } else {
            player.sendMessage(Messages.get("floor-exists", "%name%", floorName));
        }
    }

    private void handleFloorDelete(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.get("floor-delete-usage"));
            return;
        }

        String floorName = args[2];
        if (regionManager.deleteRegion(floorName)) {
            player.sendMessage(Messages.get("floor-deleted", "%name%", floorName));
        } else {
            player.sendMessage(Messages.get("floor-not-found", "%name%", floorName));
        }
    }

    private void handleStructureCreate(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(Messages.get("structure-create-usage"));
            return;
        }

        String structureName = args[2];
        String structureType = args[3];
        String floorName = args[4];

        // Check if the floor exists
        Region floorRegion = regionManager.getRegion(floorName);
        if (floorRegion == null || !"floor".equals(floorRegion.getRegionType())) {
            player.sendMessage(Messages.get("floor-not-found", "%name%", floorName));
            return;
        }

        LocationSelection sel = regionListener.getSelection(player);
        if (sel == null || !sel.isComplete()) {
            player.sendMessage(Messages.get("region-selection-incomplete"));
            return;
        }

        // Check if structure already exists
        if (regionManager.getRegion(structureName) != null) {
            player.sendMessage(Messages.get("region-exists", "%name%", structureName));
            return;
        }

        // Check if structure type exists
        Map<String, Object> structureTypeSettings = regionManager.getStructureTypeSettings(structureType);
        if (structureTypeSettings == null) {
            player.sendMessage(Messages.get("structure-type-not-found", "%name%", structureType));
            return;
        }

        // Create structure region
        Region structureRegion = new Region(structureName, "structure", sel.getPos1(), sel.getPos2());
        structureRegion.setStructureType(structureType);

        // Apply structure type settings
        for (Map.Entry<String, Object> entry : structureTypeSettings.entrySet()) {
            structureRegion.setSetting(entry.getKey(), entry.getValue());
        }

        if (regionManager.saveRegion(structureRegion)) {
            player.sendMessage(Messages.get("structure-created", "%name%", structureName, "%type%", structureType, "%floor%", floorName));
        } else {
            player.sendMessage(Messages.get("structure-save-failed", "%name%", structureName));
        }
    }

    private void handleStructureDelete(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.get("structure-delete-usage"));
            return;
        }

        String structureName = args[2];
        if (regionManager.deleteRegion(structureName)) {
            player.sendMessage(Messages.get("structure-deleted", "%name%", structureName));
        } else {
            player.sendMessage(Messages.get("structure-not-found", "%name%", structureName));
        }
    }

    private void handleStructureList(Player player) {
        Map<String, Region> structureRegions = regionManager.getRegionsByType("structure");
        player.sendMessage(Messages.get("structure-list-header"));

        if (structureRegions.isEmpty()) {
            player.sendMessage(Messages.get("no-structures-found"));
            return;
        }

        for (Map.Entry<String, Region> entry : structureRegions.entrySet()) {
            Region region = entry.getValue();
            String info = Messages.get("structure-list-item",
                    "%name%", entry.getKey(),
                    "%type%", region.getStructureType());
            player.sendMessage(info);
        }
    }

    private void handleStructureInfo(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.get("structure-info-usage"));
            return;
        }

        String structureName = args[2];
        Region structure = regionManager.getRegion(structureName);

        if (structure == null || !"structure".equals(structure.getRegionType())) {
            player.sendMessage(Messages.get("structure-not-found", "%name%", structureName));
            return;
        }

        player.sendMessage(Messages.get("structure-info-header", "%name%", structureName));
        player.sendMessage(Messages.get("structure-info-type", "%type%", structure.getStructureType()));
        player.sendMessage(Messages.get("structure-info-world", "%world%", structure.getPos1().getWorld().getName()));
        player.sendMessage(Messages.get("structure-info-bounds",
                "%pos1%", formatLocation(structure.getPos1()),
                "%pos2%", formatLocation(structure.getPos2())));

        player.sendMessage(Messages.get("structure-info-settings-header"));
        for (Map.Entry<String, Object> entry : structure.getSettings().entrySet()) {
            player.sendMessage(Messages.get("setting-display", "%key%", entry.getKey(), "%value%", entry.getValue().toString()));
        }
    }

    private void handleWand(Player player) {
        if (!player.hasPermission("sao.region.wand")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        player.getInventory().addItem(RegionListener.getWand());
        player.sendMessage(Messages.get("region-wand-received"));
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("sao.region.create")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Messages.get("create-usage"));
            return;
        }

        String regionName = args[1];
        String regionType = args[2].toLowerCase();

        if (regionManager.getRegion(regionName) != null) {
            player.sendMessage(Messages.get("region-exists", "%name%", regionName));
            return;
        }

        LocationSelection sel = regionListener.getSelection(player);
        if (sel == null || !sel.isComplete()) {
            player.sendMessage(Messages.get("region-selection-incomplete"));
            return;
        }

        Region region = new Region(regionName, regionType, sel.getPos1(), sel.getPos2());

        if (regionManager.saveRegion(region)) {
            player.sendMessage(Messages.get("region-created", "%name%", regionName, "%type%", regionType));
        } else {
            player.sendMessage(Messages.get("region-save-failed", "%name%", regionName));
        }
    }

    private void handleEdit(Player player, String[] args) {
        if (!player.hasPermission("sao.region.edit")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        if (args.length < 4) {
            player.sendMessage(Messages.get("edit-usage"));
            return;
        }

        String regionName = args[1];
        String setting = args[2].toLowerCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            player.sendMessage(Messages.get("not-found", "%name%", regionName));
            return;
        }

        Object parsedValue = parseSettingValue(setting, value);
        if (parsedValue == null) {
            player.sendMessage(Messages.get("invalid-setting-value", "%setting%", setting, "%value%", value));
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put(setting, parsedValue);

        if (regionManager.updateRegionSettings(regionName, update)) {
            player.sendMessage(Messages.get("setting-updated", "%setting%", setting, "%value%", value.toString()));
        } else {
            player.sendMessage(Messages.get("update-failed"));
        }
    }

    private Object parseSettingValue(String setting, String value) {
        switch (setting) {
            case "pvp":
            case "entry-allowed":
            case "mob-spawning":
            case "block-break":
            case "block-place":
            case "damage-players":
            case "damage-mobs":
            case "hunger-drain":
            case "teleport-on-entry":
                return Boolean.parseBoolean(value);
            case "entry-message":
            case "exit-message":
                return value;
            default:
                return null;
        }
    }

    private void handleList(Player player, String[] args) {
        if (!player.hasPermission("sao.region.list")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        String filterType = args.length > 1 ? args[1].toLowerCase() : null;
        Map<String, Region> regionsToShow = filterType != null ?
                regionManager.getRegionsByType(filterType) : regionManager.getRegions();

        if ("floor".equals(filterType)) {
            player.sendMessage(Messages.get("floor-list-header"));
        } else if ("structure".equals(filterType)) {
            player.sendMessage(Messages.get("structure-list-header"));
        } else {
            player.sendMessage(Messages.get("region-list-header"));
        }

        if (regionsToShow.isEmpty()) {
            player.sendMessage(Messages.get("no-regions-found"));
            return;
        }

        for (Map.Entry<String, Region> entry : regionsToShow.entrySet()) {
            Region region = entry.getValue();
            if ("floor".equals(region.getRegionType())) {
                player.sendMessage(Messages.get("floor-list-item", "%name%", entry.getKey()));
            } else if ("structure".equals(region.getRegionType())) {
                String info = Messages.get("structure-list-item", "%name%", entry.getKey(), "%type%", region.getStructureType());
                player.sendMessage(info);
            } else {
                String info = Messages.get("region-list-item", "%name%", entry.getKey(), "%type%", region.getRegionType());
                player.sendMessage(info);
            }
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("sao.region.delete")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Messages.get("delete-usage"));
            return;
        }

        String regionName = args[1];
        if (regionManager.deleteRegion(regionName)) {
            player.sendMessage(Messages.get("region-deleted", "%name%", regionName));
        } else {
            player.sendMessage(Messages.get("not-found", "%name%", regionName));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (!player.hasPermission("sao.region.info")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Messages.get("info-usage"));
            return;
        }

        String regionName = args[1];
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            player.sendMessage(Messages.get("not-found", "%name%", regionName));
            return;
        }

        if ("floor".equals(region.getRegionType())) {
            player.sendMessage(Messages.get("floor-info-header", "%name%", regionName));
            player.sendMessage(Messages.get("floor-info-world", "%world%", region.getPos1().getWorld().getName()));
            player.sendMessage(Messages.get("floor-info-bounds",
                    "%pos1%", formatLocation(region.getPos1()),
                    "%pos2%", formatLocation(region.getPos2())));
        } else if ("structure".equals(region.getRegionType())) {
            player.sendMessage(Messages.get("structure-info-header", "%name%", regionName));
            player.sendMessage(Messages.get("structure-info-type", "%type%", region.getStructureType()));
            player.sendMessage(Messages.get("structure-info-world", "%world%", region.getPos1().getWorld().getName()));
            player.sendMessage(Messages.get("structure-info-bounds",
                    "%pos1%", formatLocation(region.getPos1()),
                    "%pos2%", formatLocation(region.getPos2())));
        } else {
            player.sendMessage(Messages.get("region-info-header", "%name%", regionName));
            player.sendMessage(Messages.get("region-info-type", "%type%", region.getRegionType()));
        }

        player.sendMessage(Messages.get("region-info-settings-header"));
        for (Map.Entry<String, Object> entry : region.getSettings().entrySet()) {
            player.sendMessage(Messages.get("setting-display", "%key%", entry.getKey(), "%value%", entry.getValue().toString()));
        }
    }

    private void handleSettings(Player player, String[] args) {
        if (!player.hasPermission("sao.region.settings")) {
            player.sendMessage(Messages.get("no-permission"));
            return;
        }

        player.sendMessage(Messages.get("available-settings-header"));
        player.sendMessage(Messages.get("boolean-settings-header"));
        player.sendMessage(Messages.get("setting-pvp-description"));
        player.sendMessage(Messages.get("setting-entry-allowed-description"));
        player.sendMessage(Messages.get("setting-mob-spawning-description"));
        player.sendMessage(Messages.get("setting-block-break-description"));
        player.sendMessage(Messages.get("setting-block-place-description"));
        player.sendMessage(Messages.get("setting-damage-players-description"));
        player.sendMessage(Messages.get("setting-damage-mobs-description"));
        player.sendMessage(Messages.get("setting-hunger-drain-description"));
        player.sendMessage(Messages.get("setting-teleport-on-entry-description"));

        player.sendMessage(Messages.get("string-settings-header"));
        player.sendMessage(Messages.get("setting-entry-message-description"));
        player.sendMessage(Messages.get("setting-exit-message-description"));
    }

    private String formatLocation(Location loc) {
        return Messages.get("location-format",
                "%x%", String.valueOf((int) loc.getX()),
                "%y%", String.valueOf((int) loc.getY()),
                "%z%", String.valueOf((int) loc.getZ()));
    }
}