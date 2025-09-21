package com.commands;

import com.managers.SkillManager;
import com.utils.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillCommand implements CommandExecutor {
    private final SkillManager skillManager;

    public SkillCommand(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command without arguments.");
                return true;
            }

            Player player = (Player) sender;
            showSkills(player, player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "view":
                if (args.length > 1 && sender.hasPermission("skill.view.others")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        showSkills(sender, target);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                    }
                } else if (sender instanceof Player) {
                    showSkills(sender, (Player) sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player: /skill view <player>");
                }
                break;

            case "addxp":
                if (!sender.hasPermission("skill.modify")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /skill addxp <player> <skill> <amount>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                try {
                    SkillType skill = SkillType.valueOf(args[2].toUpperCase());
                    double amount = Double.parseDouble(args[3]);

                    skillManager.addSkillExperience(target.getUniqueId(), skill, amount);
                    sender.sendMessage(ChatColor.GREEN + "Added " + amount + " XP to " + target.getName() + "'s " + skill.getDisplayName() + " skill.");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid skill or amount.");
                }
                break;

            case "setlevel":
                if (!sender.hasPermission("skill.modify")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /skill setlevel <player> <skill> <level>");
                    return true;
                }

                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                try {
                    SkillType skill = SkillType.valueOf(args[2].toUpperCase());
                    int level = Integer.parseInt(args[3]);

                    skillManager.setSkillLevel(target.getUniqueId(), skill, level);
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + skill.getDisplayName() + " skill to level " + level + ".");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid skill or level.");
                }
                break;

            case "list":
                sender.sendMessage(ChatColor.GOLD + "Available skills:");
                for (SkillType skill : SkillType.values()) {
                    sender.sendMessage(ChatColor.YELLOW + " - " + skill.name() + ChatColor.GRAY + ": " + skill.getDescription());
                }
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Available: view, addxp, setlevel, list");
                break;
        }

        return true;
    }

    private void showSkills(CommandSender sender, Player target) {
        sender.sendMessage(ChatColor.GOLD + "=== " + target.getName() + "'s Skills ===");

        for (SkillType skill : SkillType.values()) {
            int level = skillManager.getSkillLevel(target.getUniqueId(), skill);
            double exp = skillManager.getSkillExperience(target.getUniqueId(), skill);
            double expNeeded = 100 * Math.pow(1.5, level); // Same formula as in PlayerSkillData

            sender.sendMessage(ChatColor.YELLOW + skill.getDisplayName() +
                    ChatColor.GREEN + " Lv." + level +
                    ChatColor.AQUA + " XP: " + String.format("%.1f", exp) + "/" + String.format("%.1f", expNeeded));
        }
    }
}