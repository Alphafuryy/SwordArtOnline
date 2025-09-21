package com.tabcompleters;

import com.utils.SkillType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SkillTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("view");
            completions.add("list");

            if (sender.hasPermission("skill.modify")) {
                completions.add("addxp");
                completions.add("setlevel");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("view") ||
                    (sender.hasPermission("skill.modify") &&
                            (args[0].equalsIgnoreCase("addxp") || args[0].equalsIgnoreCase("setlevel")))) {

                // Add online players
                completions.addAll(org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            if (sender.hasPermission("skill.modify") &&
                    (args[0].equalsIgnoreCase("addxp") || args[0].equalsIgnoreCase("setlevel"))) {

                // Add skill names
                for (SkillType skill : SkillType.values()) {
                    completions.add(skill.name());
                }
            }
        }

        // Filter based on what the user has typed
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}