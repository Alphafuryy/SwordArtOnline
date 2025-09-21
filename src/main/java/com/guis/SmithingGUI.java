package com.guis;

import com.utils.GUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SmithingGUI {

    public static void openSmithingGUI(Player player) {
        Inventory gui = GUI.createInventory("Smithing.yml", 36, "&7Smithing");
        player.openInventory(gui);
    }
}
