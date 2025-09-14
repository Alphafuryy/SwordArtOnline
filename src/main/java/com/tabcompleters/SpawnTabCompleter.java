package com.tabcompleters;

import com.SwordArtOnline;
import com.utils.Floor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnTabCompleter implements TabCompleter {

    private final SwordArtOnline plugin;

    public SpawnTabCompleter(SwordArtOnline plugin) { this.plugin = plugin; }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) return completions;

        if (args.length == 1) {
            completions.add("set");
            completions.add("delete");
            completions.add("edit");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("edit")) {
                // Suggest spawn names from all floors
                for (Floor floor : plugin.getFloorManager().getFloors().values()) {
                    completions.addAll(floor.getSpawnNames());
                }
            }
        } else if (args.length == 3) {
            // Suggest floor names
            completions.addAll(plugin.getFloorManager().getFloors().keySet());
        }

        return completions;
    }
}
