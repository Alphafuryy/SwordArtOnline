package com.skills;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class DamageReductionListener implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity living) {
            UUID id = living.getUniqueId();
            if (LeapIntoTheFutureAbility.damageReductionPlayers.containsKey(id)) {
                double multiplier = LeapIntoTheFutureAbility.damageReductionAmount.getOrDefault(id, 1.0);
                event.setDamage(event.getDamage() * multiplier);
            }
        }
    }

}