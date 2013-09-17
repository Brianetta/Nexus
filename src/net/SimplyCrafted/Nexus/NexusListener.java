package net.SimplyCrafted.Nexus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Copyright © Brian Ronald
 * 14/09/13
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

public class NexusListener implements Listener {
    private final Nexus nexus;

    public NexusListener(Nexus instance) {
        this.nexus = instance;
    }

    private String hashLocation (Location location) {
        if (location == null) return "null";
        // Adding 0.5 to match the half-block position that's stored in the hash
        return String.format("%s %f %f %f", location.getWorld().getName(), location.getX() + 0.5F, location.getY() + 0.5F, location.getZ() + 0.5F);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEvent (PlayerInteractEvent event) {
        // Check whether the block still exists
        if (event.getClickedBlock() == null) return;
        // Check the player did something to a stone pressure plate
        if (!(event.getClickedBlock().getState().getType() == Material.STONE_PLATE)) return;
        // Check that that something involved, you know, feet
        if (!(event.getAction() == Action.PHYSICAL)) return;
        // Try to get the name of the town from the hash, using the location of the pressure plate
        String town = nexus.NexusMap.get(hashLocation(event.getClickedBlock().getLocation()));
        if ((town != null) && !(nexus.isLocked(event.getPlayer().getName()))) {
            nexus.lock(event.getPlayer().getName());
            NexusHandler pair = new NexusHandler(nexus,town,event.getPlayer());
            if (pair.isEstablished()) {
                pair.teleportFurthest();
            }
        }
    }
}
