package com.guis;


import com.utils.GUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class SmithingMenuGUI {


    public static void openSmithingMenuGUI(Player player) {
        Inventory gui = GUI.createInventory("SmithingMenu.yml", 27, "&7Smithing");
        player.openInventory(gui);
    }
}