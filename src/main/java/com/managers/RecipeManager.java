package com.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class RecipeManager {
    private Plugin plugin;
    private File recipesFolder;
    private File ingredientsFile;
    private Map<String, CustomRecipe> recipes = new HashMap<>();
    private Map<String, CustomIngredient> ingredients = new HashMap<>();
    private ItemManager itemManager;
    private String[] categories = {
            "Swords", "Rapiers", "Maces", "Daggers", "Axes", "Spears", "Bows", "Shields", "Armor"
    };

    public RecipeManager(Plugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.recipesFolder = new File(plugin.getDataFolder(), "Recipes");
        this.ingredientsFile = new File(plugin.getDataFolder(), "Items/recipes.yml");

        // Create directories if they don't exist
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            // Extract default recipe files from JAR
            extractDefaultRecipeFiles();
        }
        if (!ingredientsFile.getParentFile().exists()) {
            ingredientsFile.getParentFile().mkdirs();
        }

        loadAllIngredients();
        loadAllRecipes();
    }
    private void extractDefaultRecipeFiles() {
        for (String category : categories) {
            String fileName = category.toLowerCase() + ".yml";
            File categoryFile = new File(recipesFolder, fileName);

            // Only extract if file doesn't exist
            if (!categoryFile.exists()) {
                try {
                    // Try to get the file from JAR
                    InputStream inputStream = plugin.getResource("Recipes/" + fileName);
                    if (inputStream != null) {
                        Files.copy(inputStream, categoryFile.toPath());
                        plugin.getLogger().info("Extracted default recipe file: " + fileName);
                    } else {
                        // Create empty file if not found in JAR
                        categoryFile.createNewFile();
                        plugin.getLogger().info("Created empty recipe file: " + fileName);
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not create recipe file: " + fileName, e);
                }
            }
        }

        // Extract ingredients file if it doesn't exist
        if (!ingredientsFile.exists()) {
            try {
                InputStream inputStream = plugin.getResource("Items/recipes.yml");
                if (inputStream != null) {
                    ingredientsFile.getParentFile().mkdirs();
                    Files.copy(inputStream, ingredientsFile.toPath());
                    plugin.getLogger().info("Extracted default ingredients file");
                } else {
                    ingredientsFile.createNewFile();
                    plugin.getLogger().info("Created empty ingredients file");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not create ingredients file", e);
            }
        }
    }

    public void loadAllIngredients() {
        ingredients.clear();

        if (!ingredientsFile.exists()) {
            plugin.getLogger().warning("Ingredients file not found: " + ingredientsFile.getPath());
            // Try to load from JAR as fallback
            loadIngredientsFromJar();
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(ingredientsFile);

            for (String ingredientId : config.getKeys(false)) {
                try {
                    Material material = Material.matchMaterial(config.getString(ingredientId + ".material", "STONE"));
                    String displayName = config.getString(ingredientId + ".displayname", ingredientId);
                    List<String> lore = config.getStringList(ingredientId + ".lore");

                    // Validate material
                    if (material == null) {
                        plugin.getLogger().warning("Invalid material for ingredient " + ingredientId + ": " + config.getString(ingredientId + ".material"));
                        material = Material.STONE;
                    }

                    CustomIngredient ingredient = new CustomIngredient(ingredientId, material, displayName, lore);
                    ingredients.put(ingredientId, ingredient);

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error loading ingredient " + ingredientId, e);
                }
            }
            plugin.getLogger().info("Loaded " + ingredients.size() + " custom ingredients from file");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading ingredients file", e);
        }
    }
    private void loadIngredientsFromJar() {
        try {
            InputStream inputStream = plugin.getResource("Items/recipes.yml");
            if (inputStream != null) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));

                for (String ingredientId : config.getKeys(false)) {
                    try {
                        Material material = Material.matchMaterial(config.getString(ingredientId + ".material", "STONE"));
                        String displayName = config.getString(ingredientId + ".displayname", ingredientId);
                        List<String> lore = config.getStringList(ingredientId + ".lore");

                        if (material == null) {
                            material = Material.STONE;
                        }

                        CustomIngredient ingredient = new CustomIngredient(ingredientId, material, displayName, lore);
                        ingredients.put(ingredientId, ingredient);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error loading ingredient from JAR: " + ingredientId, e);
                    }
                }
                plugin.getLogger().info("Loaded " + ingredients.size() + " custom ingredients from JAR");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading ingredients from JAR", e);
        }
    }

    public void loadAllRecipes() {
        recipes.clear();
        int totalLoaded = 0;

        for (String category : categories) {
            File categoryFile = new File(recipesFolder, category.toLowerCase() + ".yml");
            if (!categoryFile.exists()) {
                plugin.getLogger().warning("Recipe category file not found: " + categoryFile.getPath());
                // Try to load from JAR as fallback
                loadRecipesFromJar(category);
                continue;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
                int categoryLoaded = 0;

                for (String recipeId : config.getKeys(false)) {
                    try {
                        String result = config.getString(recipeId + ".result");
                        if (result == null) {
                            plugin.getLogger().warning("Missing result for recipe: " + recipeId);
                            continue;
                        }

                        Map<String, Integer> ingredientsMap = new HashMap<>();

                        // Load ingredients
                        if (config.contains(recipeId + ".ingredients")) {
                            for (String ingredientKey : config.getConfigurationSection(recipeId + ".ingredients").getKeys(false)) {
                                int amount = config.getInt(recipeId + ".ingredients." + ingredientKey);
                                ingredientsMap.put(ingredientKey.toLowerCase(), amount);
                            }
                        }

                        CustomRecipe recipe = new CustomRecipe(recipeId, result, ingredientsMap, category);
                        recipes.put(recipeId, recipe);
                        categoryLoaded++;

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error loading recipe " + recipeId + " from " + categoryFile.getName(), e);
                    }
                }
                plugin.getLogger().info("Loaded " + categoryLoaded + " recipes from " + category);
                totalLoaded += categoryLoaded;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading recipe file: " + categoryFile.getName(), e);
            }
        }
        plugin.getLogger().info("Total loaded " + totalLoaded + " custom recipes from " + categories.length + " categories");
    }
    public boolean createRecipe(String id, String result, String category, Map<String, Integer> ingredients) {
        if (recipes.containsKey(id)) {
            plugin.getLogger().warning("Recipe with ID " + id + " already exists!");
            return false;
        }

        // Validate category
        if (!Arrays.asList(categories).contains(category)) {
            plugin.getLogger().warning("Invalid category: " + category);
            return false;
        }

        // Check if result item exists
        if (itemManager.getItem(result) == null) {
            plugin.getLogger().warning("Result item " + result + " does not exist!");
            return false;
        }

        CustomRecipe recipe = new CustomRecipe(id, result, ingredients, category);
        recipes.put(id, recipe);

        return saveRecipe(recipe);
    }

    public boolean deleteRecipe(String id) {
        CustomRecipe recipe = recipes.get(id);
        if (recipe == null) return false;

        recipes.remove(id);
        return deleteRecipeFromFile(recipe);
    }

    public boolean editRecipeResult(String id, String newResult) {
        CustomRecipe recipe = recipes.get(id);
        if (recipe == null) return false;

        // Check if new result item exists
        if (itemManager.getItem(newResult) == null) {
            plugin.getLogger().warning("Result item " + newResult + " does not exist!");
            return false;
        }

        recipe.setResult(newResult);
        return saveRecipe(recipe);
    }

    public boolean editRecipeIngredient(String id, String ingredient, int amount) {
        CustomRecipe recipe = recipes.get(id);
        if (recipe == null) return false;

        if (amount <= 0) {
            recipe.getIngredients().remove(ingredient.toLowerCase());
        } else {
            recipe.getIngredients().put(ingredient.toLowerCase(), amount);
        }

        return saveRecipe(recipe);
    }

    public boolean editRecipeCategory(String id, String newCategory) {
        CustomRecipe recipe = recipes.get(id);
        if (recipe == null) return false;

        if (!Arrays.asList(categories).contains(newCategory)) {
            plugin.getLogger().warning("Invalid category: " + newCategory);
            return false;
        }

        // Move recipe to different category file
        String oldCategory = recipe.getCategory();
        recipe.setCategory(newCategory);
        if (!oldCategory.equals(newCategory)) {
            deleteRecipeFromFileInCategory(recipe, oldCategory);
        }

        return saveRecipe(recipe);
    }
    private void loadRecipesFromJar(String category) {
        try {
            String fileName = category.toLowerCase() + ".yml";
            InputStream inputStream = plugin.getResource("Recipes/" + fileName);
            if (inputStream != null) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
                int categoryLoaded = 0;

                for (String recipeId : config.getKeys(false)) {
                    try {
                        String result = config.getString(recipeId + ".result");
                        if (result == null) continue;

                        Map<String, Integer> ingredientsMap = new HashMap<>();

                        if (config.contains(recipeId + ".ingredients")) {
                            for (String ingredientKey : config.getConfigurationSection(recipeId + ".ingredients").getKeys(false)) {
                                int amount = config.getInt(recipeId + ".ingredients." + ingredientKey);
                                ingredientsMap.put(ingredientKey.toLowerCase(), amount);
                            }
                        }

                        CustomRecipe recipe = new CustomRecipe(recipeId, result, ingredientsMap, category);
                        recipes.put(recipeId, recipe);
                        categoryLoaded++;

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error loading recipe from JAR: " + recipeId, e);
                    }
                }
                plugin.getLogger().info("Loaded " + categoryLoaded + " recipes from JAR for category: " + category);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading recipes from JAR for category: " + category, e);
        }
    }
    private boolean saveRecipe(CustomRecipe recipe) {
        File categoryFile = new File(recipesFolder, recipe.getCategory().toLowerCase() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);

        config.set(recipe.getId() + ".result", recipe.getResult());

        for (Map.Entry<String, Integer> entry : recipe.getIngredients().entrySet()) {
            config.set(recipe.getId() + ".ingredients." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(categoryFile);
            plugin.getLogger().info("Saved recipe: " + recipe.getId() + " to " + categoryFile.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save recipe " + recipe.getId(), e);
            return false;
        }
    }

    private boolean deleteRecipeFromFile(CustomRecipe recipe) {
        return deleteRecipeFromFileInCategory(recipe, recipe.getCategory());
    }

    private boolean deleteRecipeFromFileInCategory(CustomRecipe recipe, String category) {
        File categoryFile = new File(recipesFolder, category.toLowerCase() + ".yml");
        if (!categoryFile.exists()) return true;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
        config.set(recipe.getId(), null);

        try {
            config.save(categoryFile);
            plugin.getLogger().info("Deleted recipe: " + recipe.getId() + " from " + categoryFile.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not delete recipe " + recipe.getId(), e);
            return false;
        }
    }

    // Ingredient management methods
    public boolean createIngredient(String id, Material material, String displayName) {
        if (ingredients.containsKey(id)) {
            plugin.getLogger().warning("Ingredient with ID " + id + " already exists!");
            return false;
        }

        CustomIngredient ingredient = new CustomIngredient(id, material, displayName, new ArrayList<>());
        ingredients.put(id, ingredient);

        return saveIngredient(ingredient);
    }

    public boolean editIngredientMaterial(String id, Material newMaterial) {
        CustomIngredient ingredient = ingredients.get(id);
        if (ingredient == null) return false;

        ingredient.setMaterial(newMaterial);
        return saveIngredient(ingredient);
    }

    public boolean editIngredientDisplayName(String id, String newDisplayName) {
        CustomIngredient ingredient = ingredients.get(id);
        if (ingredient == null) return false;

        ingredient.setDisplayName(newDisplayName);
        return saveIngredient(ingredient);
    }

    public boolean editIngredientLore(String id, List<String> lore) {
        CustomIngredient ingredient = ingredients.get(id);
        if (ingredient == null) return false;

        ingredient.setLore(lore);
        return saveIngredient(ingredient);
    }

    private boolean saveIngredient(CustomIngredient ingredient) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(ingredientsFile);

        config.set(ingredient.getId() + ".material", ingredient.getMaterial().name());
        config.set(ingredient.getId() + ".displayname", ingredient.getDisplayName());
        config.set(ingredient.getId() + ".lore", ingredient.getLore());

        try {
            config.save(ingredientsFile);
            plugin.getLogger().info("Saved ingredient: " + ingredient.getId());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save ingredient " + ingredient.getId(), e);
            return false;
        }
    }

    public ItemStack getIngredientItem(String ingredientId) {
        CustomIngredient ingredient = ingredients.get(ingredientId);
        if (ingredient == null) {
            // Try to use as vanilla material
            Material material = Material.matchMaterial(ingredientId.toUpperCase());
            if (material != null) {
                return new ItemStack(material, 1);
            }
            return null;
        }

        ItemStack item = new ItemStack(ingredient.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ingredient.getDisplayName()));

            if (!ingredient.getLore().isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : ingredient.getLore()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isIngredient(ItemStack item, String ingredientId) {
        if (item == null || item.getType() == Material.AIR) return false;

        CustomIngredient ingredient = ingredients.get(ingredientId);
        if (ingredient == null) {
            // Check if it's a vanilla material
            return item.getType().name().equalsIgnoreCase(ingredientId);
        }

        if (item.getType() != ingredient.getMaterial()) return false;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                return meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', ingredient.getDisplayName()));
            }
        }

        return false;
    }

    public String getIngredientId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        // Check custom ingredients first
        for (CustomIngredient ingredient : ingredients.values()) {
            if (item.getType() == ingredient.getMaterial()) {
                if (item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta.hasDisplayName() &&
                            meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', ingredient.getDisplayName()))) {
                        return ingredient.getId();
                    }
                }
            }
        }

        // Return vanilla material name
        return item.getType().name().toLowerCase();
    }

    public boolean canCraftRecipe(String recipeId, Map<String, Integer> availableIngredients) {
        CustomRecipe recipe = recipes.get(recipeId);
        if (recipe == null) return false;

        for (Map.Entry<String, Integer> required : recipe.getIngredients().entrySet()) {
            int available = availableIngredients.getOrDefault(required.getKey().toLowerCase(), 0);
            if (available < required.getValue()) {
                return false;
            }
        }

        return true;
    }

    public Map<String, Integer> getRequiredIngredients(String recipeId) {
        CustomRecipe recipe = recipes.get(recipeId);
        if (recipe == null) return new HashMap<>();

        return new HashMap<>(recipe.getIngredients());
    }

    public ItemStack craftRecipe(String recipeId, Map<String, Integer> availableIngredients) {
        if (!canCraftRecipe(recipeId, availableIngredients)) {
            return null;
        }

        CustomRecipe recipe = recipes.get(recipeId);
        return itemManager.getItemStack(recipe.getResult());
    }

    public CustomRecipe getRecipe(String id) {
        return recipes.get(id);
    }

    public Map<String, CustomRecipe> getAllRecipes() {
        return new HashMap<>(recipes);
    }

    public CustomIngredient getIngredient(String id) {
        return ingredients.get(id);
    }

    public Map<String, CustomIngredient> getAllIngredients() {
        return new HashMap<>(ingredients);
    }

    public List<CustomRecipe> getRecipesForItem(String itemId) {
        List<CustomRecipe> result = new ArrayList<>();
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.getResult().equalsIgnoreCase(itemId)) {
                result.add(recipe);
            }
        }
        return result;
    }

    public List<CustomRecipe> getRecipesByCategory(String category) {
        List<CustomRecipe> categoryRecipes = new ArrayList<>();
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.getCategory().equalsIgnoreCase(category)) {
                categoryRecipes.add(recipe);
            }
        }
        return categoryRecipes;
    }

    public static class CustomRecipe {
        private String id;
        private String result;
        private Map<String, Integer> ingredients;
        private String category;

        public CustomRecipe(String id, String result, Map<String, Integer> ingredients, String category) {
            this.id = id;
            this.result = result;
            this.ingredients = ingredients != null ? ingredients : new HashMap<>();
            this.category = category;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public Map<String, Integer> getIngredients() { return ingredients; }
        public void setIngredients(Map<String, Integer> ingredients) { this.ingredients = ingredients; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class CustomIngredient {
        private String id;
        private Material material;
        private String displayName;
        private List<String> lore;

        public CustomIngredient(String id, Material material, String displayName, List<String> lore) {
            this.id = id;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore != null ? lore : new ArrayList<>();
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public Material getMaterial() { return material; }
        public void setMaterial(Material material) { this.material = material; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public List<String> getLore() { return lore; }
        public void setLore(List<String> lore) { this.lore = lore; }
    }
}