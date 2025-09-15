// RegionCommand.java
package com.commands;

import com.listeners.RegionListener;
import com.managers.RegionManager;
import com.utils.LocationSelection;
import com.utils.Messages;
import com.utils.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            sender.sendMessage(Messages.getRegion("only-player"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Messages.getRegion("region-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> {
                if (!player.hasPermission("sao.region.wand")) {
                    player.sendMessage(Messages.getRegion("no-permission"));
                    return true;
                }

                player.getInventory().addItem(RegionListener.getWand());
                player.sendMessage(Messages.getRegion("region-wand-received"));
            }
            case "set" -> {
                if (!player.hasPermission("sao.region.set")) {
                    player.sendMessage(Messages.getRegion("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(Messages.getRegion("region-usage"));
                    return true;
                }

                String floor = args[1];
                LocationSelection sel = regionListener.getSelection(player);

                if (sel == null || !sel.isComplete()) {
                    player.sendMessage(Messages.getRegion("region-selection-incomplete"));
                    return true;
                }

                Region region = new Region(floor, sel.getPos1(), sel.getPos2());
                regionManager.saveRegion(region);

                player.sendMessage(Messages.getRegion("region-saved", "%floor%", floor));
            }
            case "structure" -> {
                if (!player.hasPermission("sao.region.structure")) {
                    player.sendMessage(Messages.getRegion("no-permission"));
                    return true;
                }

                if (args.length < 3) {
                    player.sendMessage(Messages.getRegion("structure-usage"));
                    return true;
                }

                String structureType = args[1];
                String floor = args[2];
                LocationSelection sel = regionListener.getSelection(player);

                if (sel == null || !sel.isComplete()) {
                    player.sendMessage(Messages.getRegion("region-selection-incomplete"));
                    return true;
                }

                Region structureRegion = new Region(floor, sel.getPos1(), sel.getPos2());
                structureRegion.setStructureType(structureType);
                regionManager.saveStructureRegion(structureRegion);

                player.sendMessage(Messages.getRegion("structure-saved",
                        "%type%", structureType, "%floor%", floor));
            }
            case "list" -> {
                if (!player.hasPermission("sao.region.list")) {
                    player.sendMessage(Messages.getRegion("no-permission"));
                    return true;
                }

                player.sendMessage(Messages.getRegion("region-list-header"));
                for (String floor : regionManager.getRegions().keySet()) {
                    player.sendMessage(Messages.getRegion("region-list-item", "%floor%", floor));
                }

                player.sendMessage(Messages.getRegion("structure-list-header"));
                for (String structure : regionManager.getStructureRegions().keySet()) {
                    player.sendMessage(Messages.getRegion("structure-list-item", "%structure%", structure));
                }
            }
            case "delete" -> {
                if (!player.hasPermission("sao.region.delete")) {
                    player.sendMessage(Messages.getRegion("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(Messages.getRegion("delete-usage"));
                    return true;
                }

                String name = args[1];
                if (regionManager.deleteRegion(name)) {
                    player.sendMessage(Messages.getRegion("region-deleted", "%name%", name));
                } else if (regionManager.deleteStructureRegion(name)) {
                    player.sendMessage(Messages.getRegion("structure-deleted", "%name%", name));
                } else {
                    player.sendMessage(Messages.getRegion("not-found", "%name%", name));
                }
            }
            default -> player.sendMessage(Messages.getRegion("region-usage"));
        }

        return true;
    }
}