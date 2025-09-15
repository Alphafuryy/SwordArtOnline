// RegionTabCompleter.java
package com.tabcompleters;

import com.SwordArtOnline;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RegionTabCompleter implements TabCompleter {

    private final SwordArtOnline plugin;

    public RegionTabCompleter(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("wand");
            list.add("set");
            list.add("structure");
            list.add("list");
            list.add("delete");
            return list.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                list.addAll(plugin.getFloorManager().getFloors().keySet());
            } else if (args[0].equalsIgnoreCase("structure")) {
                list.add("dungeon");
                list.add("boss");
                list.add("safezone");
                list.add("shop");
                list.add("teleporter");
            } else if (args[0].equalsIgnoreCase("delete")) {
                list.addAll(plugin.getRegionManager().getRegions().keySet());
                list.addAll(plugin.getRegionManager().getStructureRegions().keySet());
            }
            return list.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("structure")) {
            list.addAll(plugin.getFloorManager().getFloors().keySet());
            return list.stream().filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }

        return list;
    }
}