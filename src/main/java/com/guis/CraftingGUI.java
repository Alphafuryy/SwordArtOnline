package com.guis;

import com.utils.GUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CraftingGUI {

    public static void openCraftingGUI(Player player) {
        Inventory gui = GUI.createInventory("Crafting.yml", 36, "&7Crafting");
        player.openInventory(gui);
    }
}
