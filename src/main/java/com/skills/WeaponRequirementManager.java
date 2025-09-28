package com.skills;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import com.managers.ItemManager;
import com.SwordArtOnline;
import org.bukkit.NamespacedKey;

import java.util.*;

public class WeaponRequirementManager {

    private static WeaponRequirementManager instance;
    private final Map<SkillCategory, List<String>> requiredWeaponCategories;
    private final Map<SkillCategory, List<Material>> fallbackMaterials;

    public WeaponRequirementManager() {
        this.requiredWeaponCategories = new HashMap<>();
        this.fallbackMaterials = new HashMap<>();
        initializeWeaponMappings();
    }

    public static WeaponRequirementManager getInstance() {
        if (instance == null) {
            instance = new WeaponRequirementManager();
        }
        return instance;
    }

    private void initializeWeaponMappings() {
        // 1H Sword Skills - require Swords category or sword materials
        requiredWeaponCategories.put(SkillCategory.ONE_HANDED_SWORD, Arrays.asList("Swords"));
        fallbackMaterials.put(SkillCategory.ONE_HANDED_SWORD, Arrays.asList(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
        ));

        // 1H Dagger Skills - require Daggers category
        requiredWeaponCategories.put(SkillCategory.ONE_HANDED_DAGGER, Arrays.asList("Daggers"));
        fallbackMaterials.put(SkillCategory.ONE_HANDED_DAGGER, Arrays.asList(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD // Fallback to swords
        ));

        // 1H Rapier Skills - require Rapiers category
        requiredWeaponCategories.put(SkillCategory.ONE_HANDED_RAPIER, Arrays.asList("Rapiers"));
        fallbackMaterials.put(SkillCategory.ONE_HANDED_RAPIER, Arrays.asList(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD // Fallback to swords
        ));

        // 1H Mace Skills - require Maces category
        requiredWeaponCategories.put(SkillCategory.ONE_HANDED_MACE, Arrays.asList("Maces"));
        fallbackMaterials.put(SkillCategory.ONE_HANDED_MACE, Arrays.asList(
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE // Fallback to axes
        ));

        // 2H Axe Skills - require Axes category or axe materials
        requiredWeaponCategories.put(SkillCategory.TWO_HANDED_AXE, Arrays.asList("Axes"));
        fallbackMaterials.put(SkillCategory.TWO_HANDED_AXE, Arrays.asList(
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
        ));

        // 2H Spear Skills - require Spears category
        requiredWeaponCategories.put(SkillCategory.TWO_HANDED_SPEAR, Arrays.asList("Spears"));
        fallbackMaterials.put(SkillCategory.TWO_HANDED_SPEAR, Arrays.asList(
                Material.TRIDENT // Fallback to trident
        ));

        // Bow Skills - require Bows category or bow materials
        requiredWeaponCategories.put(SkillCategory.BOW, Arrays.asList("Bows"));
        fallbackMaterials.put(SkillCategory.BOW, Arrays.asList(
                Material.BOW, Material.CROSSBOW
        ));

        // Shield Skills - require Shields category or shield materials
        requiredWeaponCategories.put(SkillCategory.SHIELD, Arrays.asList("Shields"));
        fallbackMaterials.put(SkillCategory.SHIELD, Arrays.asList(
                Material.SHIELD
        ));

        // Universal Skills - no weapon requirements
        requiredWeaponCategories.put(SkillCategory.UNIVERSAL, new ArrayList<>());
        fallbackMaterials.put(SkillCategory.UNIVERSAL, new ArrayList<>());
    }

    public boolean canUseSkillWithWeapon(SkillCategory skillCategory, ItemStack weapon) {
        // Universal skills can be used with any weapon
        if (skillCategory == SkillCategory.UNIVERSAL) {
            return true;
        }

        // Check if player is holding anything
        if (weapon == null || weapon.getType() == Material.AIR) {
            return false;
        }

        // First, check if it's a custom item from the required category
        if (isCustomItemFromCategory(weapon, skillCategory)) {
            return true;
        }

        // Fallback to vanilla material check
        return isVanillaMaterialAllowed(weapon.getType(), skillCategory);
    }

    private boolean isCustomItemFromCategory(ItemStack item, SkillCategory requiredCategory) {
        try {
            // Check if item has custom ID from ItemManager
            NamespacedKey key = new NamespacedKey(SwordArtOnline.getInstance(), "id");
            if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String itemId = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

                // Get ItemManager instance (you might need to adjust this based on your setup)
                ItemManager itemManager = SwordArtOnline.getInstance().getItemManager();
                if (itemManager != null) {
                    ItemManager.CustomItem customItem = itemManager.getItem(itemId);
                    if (customItem != null) {
                        // Check if the custom item's category matches required weapon categories
                        List<String> requiredCategories = requiredWeaponCategories.get(requiredCategory);
                        if (requiredCategories != null) {
                            return requiredCategories.contains(customItem.getCategory());
                        }
                    }
                }
            }
        } catch (Exception e) {
            SwordArtOnline.getInstance().getLogger().warning("Error checking custom item category: " + e.getMessage());
        }
        return false;
    }

    private boolean isVanillaMaterialAllowed(Material material, SkillCategory requiredCategory) {
        List<Material> allowedMaterials = fallbackMaterials.get(requiredCategory);
        return allowedMaterials != null && allowedMaterials.contains(material);
    }

    public String getRequiredWeaponType(SkillCategory skillCategory) {
        List<String> categories = requiredWeaponCategories.get(skillCategory);
        if (categories == null || categories.isEmpty()) {
            return "Any Weapon";
        }

        // Convert category names to more readable format
        List<String> readableNames = new ArrayList<>();
        for (String category : categories) {
            switch (category) {
                case "Swords": readableNames.add("Sword"); break;
                case "Daggers": readableNames.add("Dagger"); break;
                case "Rapiers": readableNames.add("Rapier"); break;
                case "Maces": readableNames.add("Mace"); break;
                case "Axes": readableNames.add("Axe"); break;
                case "Spears": readableNames.add("Spear"); break;
                case "Bows": readableNames.add("Bow"); break;
                case "Shields": readableNames.add("Shield"); break;
                default: readableNames.add(category);
            }
        }

        return String.join(" or ", readableNames);
    }

    public void reload() {
        initializeWeaponMappings();
    }
}