package com.commands;

import com.SwordArtOnline;
import com.listeners.TeleportListener;
import com.utils.Floor;
import com.utils.Messages;
import com.utils.SpawnPoint;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final SwordArtOnline plugin;
    private final TeleportListener teleportListener;

    public SpawnCommand(SwordArtOnline plugin, TeleportListener teleportListener) {
        this.plugin = plugin;
        this.teleportListener = teleportListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("only-player"));
            return true;
        }

        if (args.length == 0) {
            // Just /spawn → teleport to nearest spawn
            teleportListener.startTeleport(player, 100L); // 5 seconds = 100 ticks
            return true;
        }

        // Optional: admin commands like set/delete/edit
        String action = args[0].toLowerCase();
        switch (action) {
            case "set" -> handleSet(player, args);
            case "delete" -> handleDelete(player, args);
            case "edit" -> handleEdit(player, args);
            default -> player.sendMessage(Messages.get("unknown-subcommand", "%subcommand%", action));
        }

        return true;
    }


    private void handleSet(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.get("usage"));
            return;
        }

        String spawnName = args[1];
        String floorName = args[2];

        Floor floor = plugin.getFloorManager().getFloor(floorName);
        if (floor == null) floor = plugin.getFloorManager().createFloor(floorName);

        plugin.getSpawnManager().setSpawn(floor, spawnName, player.getLocation());
        player.sendMessage(Messages.get("spawn-set", "%spawn%", spawnName, "%floor%", floorName));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.get("usage"));
            return;
        }

        String spawnName = args[1];
        String floorName = args[2];

        Floor floor = plugin.getFloorManager().getFloor(floorName);
        if (floor == null) {
            player.sendMessage(Messages.get("floor-not-found", "%floor%", floorName));
            return;
        }

        boolean success = plugin.getSpawnManager().deleteSpawn(floor, spawnName);
        if (success) player.sendMessage(Messages.get("spawn-deleted", "%spawn%", spawnName, "%floor%", floorName));
        else player.sendMessage(Messages.get("spawn-not-found", "%spawn%", spawnName));
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.get("usage"));
            return;
        }

        String spawnName = args[1];
        String floorName = args[2];

        Floor floor = plugin.getFloorManager().getFloor(floorName);
        if (floor == null) {
            player.sendMessage(Messages.get("floor-not-found", "%floor%", floorName));
            return;
        }

        SpawnPoint spawn = plugin.getSpawnManager().getSpawn(floor, spawnName);
        if (spawn == null) {
            player.sendMessage(Messages.get("spawn-not-found", "%spawn%", spawnName));
            return;
        }

        plugin.getSpawnManager().setSpawn(floor, spawnName, player.getLocation());
        player.sendMessage(Messages.get("spawn-edited", "%spawn%", spawnName, "%floor%", floorName));
    }
}
