package com.skills;

import com.SwordArtOnline;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SwordSkillCommand implements CommandExecutor, TabCompleter {

    private final SwordSkillManager skillManager;

    public SwordSkillCommand() {
        this.skillManager = SwordSkillManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /skill set <slot(1-3)> <skillname>");
                    return true;
                }
                handleSetSkill(player, args);
                break;

            case "activate":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /skill activate <slot(1-3)>");
                    return true;
                }
                handleActivateSkill(player, args);
                break;

            case "list":
                handleListSkills(player, args);
                break;

            case "categories":
                handleListCategories(player);
                break;

            case "slots":
                handleShowSlots(player);
                break;

            case "clear":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /skill clear <slot(1-3)|all>");
                    return true;
                }
                handleClearSlot(player, args);
                break;

            default:
                // Direct activation: /skill <slot>
                handleDirectActivation(player, args);
                break;
        }

        return true;
    }

    private void handleSetSkill(Player player, String[] args) {
        try {
            int slot = Integer.parseInt(args[1]) - 1;
            String skillName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

            if (skillManager.setPlayerSkill(player, slot, skillName)) {
                player.sendMessage(ChatColor.GREEN + "Skill assigned successfully!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Slot must be a number between 1 and 3!");
        }
    }

    private void handleActivateSkill(Player player, String[] args) {
        try {
            int slot = Integer.parseInt(args[1]) - 1;
            skillManager.activateSkill(player, slot);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Slot must be a number between 1 and 3!");
        }
    }

    private void handleDirectActivation(Player player, String[] args) {
        try {
            int slot = Integer.parseInt(args[0]) - 1;
            skillManager.activateSkill(player, slot);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Usage: /skill <slot(1-3)> or use subcommands: set, list, categories, slots, clear");
        }
    }

    private void handleListSkills(Player player, String[] args) {
        if (args.length > 1) {
            // List skills by category
            String categoryName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            try {
                SkillCategory category = SkillCategory.valueOf(categoryName.toUpperCase().replace(" ", "_"));
                listSkillsByCategory(player, category);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid category! Use /skill categories to see available categories.");
            }
        } else {
            // List all available skills grouped by category
            listAllSkills(player);
        }
    }

    private void listAllSkills(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== Available Skills (By Category) ===");

        Map<SkillCategory, List<Skill>> skillsByCategory = skillManager.getSkillsByCategory();
        boolean hasAnySkills = false;

        for (Map.Entry<SkillCategory, List<Skill>> entry : skillsByCategory.entrySet()) {
            SkillCategory category = entry.getKey();
            List<Skill> availableSkills = entry.getValue().stream()
                    .filter(skill -> player.hasPermission(skill.getPermission()))
                    .collect(Collectors.toList());

            if (!availableSkills.isEmpty()) {
                hasAnySkills = true;
                player.sendMessage(category.getColor() + "§l" + category.getDisplayName() + ":");

                for (Skill skill : availableSkills) {
                    player.sendMessage("  " + ChatColor.GREEN + skill.getName() +
                            ChatColor.GRAY + " - " + skill.getDescription() +
                            ChatColor.DARK_GRAY + " (CD: " + skill.getCooldown() + "s)");
                }
            }
        }

        if (!hasAnySkills) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use any skills!");
            player.sendMessage(ChatColor.GRAY + "Required permissions: sao.skill.<skillname>");
        }
    }

    private void listSkillsByCategory(Player player, SkillCategory category) {
        List<Skill> skills = skillManager.getSkillsByCategory(category);
        List<Skill> availableSkills = skills.stream()
                .filter(skill -> player.hasPermission(skill.getPermission()))
                .collect(Collectors.toList());

        player.sendMessage(category.getColor() + "=== " + category.getDisplayName() + " ===");

        if (availableSkills.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No skills available in this category or no permission!");
            player.sendMessage(ChatColor.GRAY + "Required permission: " + category.getPermission());
        } else {
            for (Skill skill : availableSkills) {
                player.sendMessage(ChatColor.GREEN + skill.getName() +
                        ChatColor.GRAY + " - " + skill.getDescription() +
                        ChatColor.DARK_GRAY + " (CD: " + skill.getCooldown() + "s)");
            }
        }
    }

    private void handleListCategories(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== Skill Categories ===");
        for (SkillCategory category : SkillCategory.values()) {
            int availableCount = (int) skillManager.getSkillsByCategory(category).stream()
                    .filter(skill -> player.hasPermission(skill.getPermission()))
                    .count();
            int totalCount = skillManager.getSkillsByCategory(category).size();

            String status = availableCount > 0 ?
                    ChatColor.GREEN + "(" + availableCount + "/" + totalCount + " available)" :
                    ChatColor.RED + "(No permission)";

            player.sendMessage(category.getColor() + "• " + category.getDisplayName() +
                    ChatColor.GRAY + " " + status);
        }
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GOLD + "/skill list <category>" +
                ChatColor.GRAY + " to view skills in a category");
    }

    private void handleShowSlots(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== Your Skill Slots ===");
        SkillSlot[] slots = skillManager.getPlayerSlots(player);

        String[] slotNames = {"Shift + Right Click", "Shift + Left Click", "Shift + F (Offhand)"};

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                Skill skill = slots[i].getSkill();
                long cooldown = slots[i].getRemainingCooldown(player);
                String cooldownText = cooldown > 0 ? ChatColor.RED + " (CD: " + cooldown + "s)" : ChatColor.GREEN + " (Ready)";

                player.sendMessage(ChatColor.GOLD + "Slot " + (i + 1) + " (" + slotNames[i] + "): " +
                        skill.getCategory().getColor() + skill.getName() +
                        ChatColor.GRAY + " [" + skill.getCategory().getDisplayName() + "]" + cooldownText);
            } else {
                player.sendMessage(ChatColor.GOLD + "Slot " + (i + 1) + " (" + slotNames[i] + "): " +
                        ChatColor.RED + "Empty");
            }
        }
    }

    private void handleClearSlot(Player player, String[] args) {
        if (args[1].equalsIgnoreCase("all")) {
            skillManager.clearPlayerSlots(player);
            player.sendMessage(ChatColor.GREEN + "All skill slots cleared!");
        } else {
            try {
                int slot = Integer.parseInt(args[1]) - 1;
                SkillSlot[] slots = skillManager.getPlayerSlots(player);
                if (slots[slot] != null) {
                    String skillName = slots[slot].getSkill().getName();
                    slots[slot] = null;
                    player.sendMessage(ChatColor.GREEN + "Slot " + (slot + 1) + " cleared! (" + skillName + ")");
                } else {
                    player.sendMessage(ChatColor.RED + "Slot " + (slot + 1) + " is already empty!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Slot must be a number between 1 and 3 or 'all'!");
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== Skill System Help ===");
        player.sendMessage(ChatColor.GOLD + "/skill set <1-3> <name>" + ChatColor.GRAY + " - Assign skill to slot");
        player.sendMessage(ChatColor.GOLD + "/skill <1-3>" + ChatColor.GRAY + " - Activate skill in slot");
        player.sendMessage(ChatColor.GOLD + "/skill activate <1-3>" + ChatColor.GRAY + " - Activate skill in slot");
        player.sendMessage(ChatColor.GOLD + "/skill list [category]" + ChatColor.GRAY + " - List available skills");
        player.sendMessage(ChatColor.GOLD + "/skill categories" + ChatColor.GRAY + " - List skill categories");
        player.sendMessage(ChatColor.GOLD + "/skill slots" + ChatColor.GRAY + " - Show your current slots");
        player.sendMessage(ChatColor.GOLD + "/skill clear <1-3|all>" + ChatColor.GRAY + " - Clear slot(s)");
        player.sendMessage(ChatColor.GRAY + "Activation Methods:");
        player.sendMessage(ChatColor.GRAY + "  Slot 1: " + ChatColor.GREEN + "Shift + Right Click");
        player.sendMessage(ChatColor.GRAY + "  Slot 2: " + ChatColor.GREEN + "Shift + Left Click");
        player.sendMessage(ChatColor.GRAY + "  Slot 3: " + ChatColor.GREEN + "Shift + F (Offhand Swap)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "activate", "list", "categories", "slots", "clear", "1", "2", "3"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("activate") ||
                    args[0].equalsIgnoreCase("clear")) {
                completions.addAll(Arrays.asList("1", "2", "3"));
            } else if (args[0].equalsIgnoreCase("list")) {
                // Category names for list command
                completions.addAll(Arrays.stream(SkillCategory.values())
                        .map(cat -> cat.name().toLowerCase().replace("_", " "))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            // Skill names completion with permission check
            if (sender instanceof Player) {
                Player player = (Player) sender;
                completions.addAll(skillManager.getAvailableSkills(player).stream()
                        .map(Skill::getName)
                        .collect(Collectors.toList()));
            } else {
                completions.addAll(skillManager.getRegisteredSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toList()));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}