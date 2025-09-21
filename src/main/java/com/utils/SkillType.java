package com.utils;

import java.io.Serializable;

public enum SkillType implements Serializable {
    // Combat Skills
    ONE_HANDED_WEAPON_CREATION("One-Handed Weapon Creation", "Crafting one-handed weapons", 1.8),
    SLASH_WEAPON_FORGING("Slash Weapon Forging", "Crafting slashing weapons", 1.7),
    THRUST_WEAPON_FORGING("Thrust Weapon Forging", "Crafting thrusting weapons", 1.7),
    BLUNT_WEAPON_FORGING("Blunt Weapon Forging", "Forging maces", 1.6),
    POLEARM_WEAPON_CREATION("Polearm Weapon Creation", "Crafting polearm weapons", 1.9),

    // Armor Skills
    HEAVY_METAL_ARMOR_FORGING("Heavy Metal Armor Forging", "Crafting heavy armor", 1.7),
    LIGHT_METAL_ARMOR_FORGING("Light Metal Armor Forging", "Crafting light armor", 1.6),
    METAL_ARMOR_REPAIRING("Metal Armor Repairing", "Repairing armor", 1.5),

    // Utility Skills
    ACROBATICS("Acrobatics", "Taking fall damage or jumping", 1.4),
    SNEAKING("Sneaking", "Moving undetected", 1.5),
    HIDING("Hiding", "Sneaking past monsters undetected", 1.4),
    SWIMMING("Swimming", "Moving efficiently in water", 1.3),
    SEARCHING("Searching", "Exploring new areas", 1.4),
    EXTENDED_WEIGHT_LIMIT("Extended Weight Limit", "Carrying more items with less speed penalty", 1.6),

    // Crafting Skills
    CARPENTRY("Carpentry", "Crafting wood stuff", 1.5),
    SEWING("Sewing", "Crafting with wool", 1.4),
    LUMBER("Lumber", "Breaking trees", 1.3),
    MIXING("Mixing", "Combining foods with potions", 1.5),

    // Social Skills
    CHANT("Chant", "Singing songs in chat", 1.8),
    MUSICAL_INSTRUMENT("Musical Instrument", "Playing instruments and unlocking songs", 1.7),
    PURCHASE_NEGOTIATION("Purchase Negotiation", "Getting better prices when buying", 1.6),

    // Specialized Skills
    EQUIPMENT_APPRAISAL("Equipment Appraisal", "Appraising equipment when picked up", 1.5),
    TOOLS_APPRAISAL("Tools Appraisal", "Appraising tools when picked up", 1.4),
    PICKING("Picking", "Pickpocketing and gathering", 1.6),
    ROBBING("Robbing", "Stealing from other players", 2.0), // Very hard to master
    TRAP_DISMANTLING("Trap Dismantling", "Disarming traps", 1.7),
    FAMILIAR_COMMUNICATION("Familiar Communication", "Interacting with pets via shift + right click", 1.5),
    FAMILIAR_RECOVERY("Familiar Recovery", "Improving pet regeneration", 1.4);

    private final String displayName;
    private final String description;
    private final double difficultyMultiplier;

    SkillType(String displayName, String description, double difficultyMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.difficultyMultiplier = difficultyMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getDifficultyMultiplier() {
        return difficultyMultiplier;
    }
}