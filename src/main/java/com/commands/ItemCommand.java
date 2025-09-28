package com.commands;

import com.managers.ItemManager;
import com.managers.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemCommand implements CommandExecutor {

    private final ItemManager itemManager;
    private final RecipeManager recipeManager;

    public ItemCommand(ItemManager itemManager, RecipeManager recipeManager) {
        this.itemManager = itemManager;
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give":
                return handleGive(sender, args);
            case "recipe":
                return handleRecipe(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /item give <id> [player] [amount]");
            return true;
        }

        String itemId = args[1].toLowerCase();
        Player target;

        // If a player is specified
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must specify a player when using this from console.");
                return true;
            }
            target = (Player) sender;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                return true;
            }
        }

        ItemStack item = itemManager.getItemStack(itemId);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Unknown item ID: " + itemId);
            return true;
        }

        ItemStack toGive = item.clone();
        toGive.setAmount(amount);
        target.getInventory().addItem(toGive);

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + itemId + " to " + target.getName());
        if (!sender.equals(target)) {
            target.sendMessage(ChatColor.YELLOW + "You received " + amount + "x " + itemId + "!");
        }
        return true;
    }

    private boolean handleRecipe(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /item recipe <id> [give] [player]");
            return true;
        }

        String recipeId = args[1].toLowerCase();
        RecipeManager.CustomRecipe recipe = recipeManager.getRecipe(recipeId);

        if (recipe == null) {
            sender.sendMessage(ChatColor.RED + "Unknown recipe ID: " + recipeId);
            return true;
        }

        // Show recipe details
        if (args.length == 2) {
            sender.sendMessage(ChatColor.GOLD + "Recipe for " + recipe.getResult() + ":");
            recipe.getIngredients().forEach((ingredient, amount) -> {
                RecipeManager.CustomIngredient ing = recipeManager.getIngredient(ingredient);
                if (ing != null) {
                    sender.sendMessage(ChatColor.GRAY + " - " + amount + "x " +
                            ChatColor.translateAlternateColorCodes('&', ing.getDisplayName()));
                } else {
                    sender.sendMessage(ChatColor.GRAY + " - " + amount + "x " + ingredient);
                }
            });
            return true;
        }

        // Give ingredients: /item recipe <id> give [player]
        if (args[2].equalsIgnoreCase("give")) {
            Player target;

            if (args.length >= 4) {
                target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[3]);
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You must specify a player when using this from console.");
                    return true;
                }
                target = (Player) sender;
            }

            // Loop through recipe ingredients and give custom items
            recipe.getIngredients().forEach((ingredient, amount) -> {
                ItemStack base = recipeManager.getIngredientItem(ingredient);
                if (base == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid ingredient in recipe: " + ingredient);
                    return;
                }

                ItemStack stack = base.clone();
                stack.setAmount(amount);
                target.getInventory().addItem(stack);
            });

            sender.sendMessage(ChatColor.GREEN + "Gave materials for recipe " + recipeId + " to " + target.getName());
            if (!sender.equals(target)) {
                target.sendMessage(ChatColor.YELLOW + "You received the materials for recipe " + recipeId + "!");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /item recipe <id> [give] [player]");
        return true;
    }


    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.GRAY + " /item give <id> [player] [amount]");
        sender.sendMessage(ChatColor.GRAY + " /item recipe <id>");
    }
}
