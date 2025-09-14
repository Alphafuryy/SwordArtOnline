package com.commands;

import com.Abilitys.DoubleCleaveAbility;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SkillCommand implements CommandExecutor {


    public SkillCommand() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /skill <metadata>");
            return true;
        }

        String metadata = args[0];

        // Give metadata to the item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be holding an item!");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) meta = item.getItemMeta();

        List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
        if (!lore.contains(metadata)) {
            lore.add(metadata);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);

        player.sendMessage(ChatColor.GREEN + "Metadata '" + metadata + "' added to your item!");

        return true;
    }

    // Helper method to check if a player’s item has the metadata
    public static boolean hasMetadata(Player player, String metadata) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        return meta.getLore().contains(metadata);
    }
}
