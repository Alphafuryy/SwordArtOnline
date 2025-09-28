package com.skills;

public enum SkillCategory {
    ONE_HANDED_SWORD("1H Sword Skill Records", "sao.category.1h_sword", "§6"),
    ONE_HANDED_DAGGER("1H Dagger Skill Records", "sao.category.1h_dagger", "§a"),
    ONE_HANDED_RAPIER("1H Rapier Skill Records", "sao.category.1h_rapier", "§b"),
    ONE_HANDED_MACE("1H Mace Skill Records", "sao.category.1h_mace", "§e"),
    TWO_HANDED_AXE("2H Axe Skill Records", "sao.category.2h_axe", "§c"),
    TWO_HANDED_SPEAR("2H Spear Skill Records", "sao.category.2h_spear", "§d"),
    BOW("Bow Skill Records", "sao.category.bow", "§2"),
    SHIELD("Shield Skill Records", "sao.category.shield", "§9"),
    UNIVERSAL("Universal Skills", "sao.category.universal", "§f");

    private final String displayName;
    private final String permission;
    private final String color;

    SkillCategory(String displayName, String permission, String color) {
        this.displayName = displayName;
        this.permission = permission;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermission() {
        return permission;
    }

    public String getColor() {
        return color;
    }

    public static SkillCategory fromString(String name) {
        for (SkillCategory category : values()) {
            if (category.name().equalsIgnoreCase(name) ||
                    category.getDisplayName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return UNIVERSAL;
    }
}