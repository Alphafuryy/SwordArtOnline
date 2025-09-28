// Updated LeapIntoTheFuture skill with category
package com.skills;

import org.bukkit.entity.Player;

public class LeapIntoTheFutureSkill extends AbstractSkill {

    public LeapIntoTheFutureSkill() {
        super("LeapIntoTheFuture", "Dash forward and slash with time manipulation", 12, SkillCategory.ONE_HANDED_DAGGER);
    }

    @Override
    public void activate(Player player) {
        LeapIntoTheFutureAbility.activateAbility(player);
    }
}
