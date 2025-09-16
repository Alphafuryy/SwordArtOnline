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
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("wand");
            completions.add("create");
            completions.add("edit");
            completions.add("list");
            completions.add("delete");
            completions.add("info");
            completions.add("settings");
            completions.add("structure");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "create":
                    completions.addAll(List.of("floor", "structure", "safezone", "arena", "restricted"));
                    break;
                case "edit":
                case "delete":
                case "info":
                    completions.addAll(plugin.getRegionManager().getRegions().keySet());
                    break;
                case "list":
                    completions.addAll(List.of("floor", "structure", "safezone", "arena", "restricted"));
                    break;
                case "structure":
                    completions.addAll(List.of("create", "delete", "list", "info"));
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "create":
                    if ("structure".equalsIgnoreCase(args[1])) {
                        completions.addAll(plugin.getRegionManager().getStructureTypes().keySet());
                    }
                    break;
                case "structure":
                    if ("delete".equalsIgnoreCase(args[1]) || "info".equalsIgnoreCase(args[1])) {
                        completions.addAll(plugin.getRegionManager().getStructureTypes().keySet());
                    }
                    break;
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}