package com.tabcompleters;

import com.managers.ItemManager;
import com.managers.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemTabCompleter implements TabCompleter {

    private final ItemManager itemManager;
    private final RecipeManager recipeManager;

    public ItemTabCompleter(ItemManager itemManager, RecipeManager recipeManager) {
        this.itemManager = itemManager;
        this.recipeManager = recipeManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("give");
            completions.add("recipe");
            return filter(completions, args[0]);
        }

        // ---- /item give ----
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                completions.addAll(itemManager.getAllItems().keySet()); // suggest item IDs
                return filter(completions, args[1]);
            }
            if (args.length == 3) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .collect(Collectors.toList()));
                return filter(completions, args[2]);
            }
            if (args.length == 4) {
                completions.add("1");
                completions.add("16");
                completions.add("32");
                completions.add("64");
                return filter(completions, args[3]);
            }
        }

        // ---- /item recipe ----
        if (args[0].equalsIgnoreCase("recipe")) {
            if (args.length == 2) {
                completions.addAll(recipeManager.getAllRecipes().keySet()); // recipe IDs
                return filter(completions, args[1]);
            }
            if (args.length == 3) {
                completions.add("give");
                return filter(completions, args[2]);
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("give")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .collect(Collectors.toList()));
                return filter(completions, args[3]);
            }
            if (args.length == 5 && args[2].equalsIgnoreCase("give")) {
                completions.add("1");
                completions.add("16");
                completions.add("32");
                completions.add("64");
                return filter(completions, args[4]);
            }
        }

        return completions;
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
