package net.SimplyCrafted.Nexus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

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

public class NexusPair {
    // This plugin was designed for the Simply Crafted SMP server, where a hall
    // contains one end of each nexus pad pair, and a town contains the other
    // end. The hall is, literally, a nexus. So, the two locations are stored
    // as hallPad and townPad, respectively.

    private final Nexus nexus; // A Nexus class instance is required for config
    private Location hallPadLocation, townPadLocation;
    private boolean established = false; // set true when both Locations are defined
    private boolean paid = false ; // flag to show whether this Nexus has been paid for

    public NexusPair(Nexus instance) {
        this.nexus = instance;
    }

    public Location getHallPadLocation() {
        return hallPadLocation;
    }

    public Location getTownPadLocation() {
        return townPadLocation;
    }

    public boolean isEstablished() {
        return established;
    }

    public boolean isPaid () {
        return paid;
    }

    private Location createLocationFromPlayer (Player player) {
        Location location = player.getLocation();
        // Convert yaw to one of eight cardinal points for simplicity,
        // adding 23 degrees to yaw first to move boundary conditions
        // by roughly 1/16th of a circle
        location.setYaw((float) (8F * Math.floor((location.getYaw() + 23F) / 8F)));
        return location;
    }

    private Location createPadAtPlayer (Player player) {
        // Place a block under the player's feet, and pop a stone
        // pressure plate upon it.
        Material padMaterial;
        Block here;
        if (player.getItemInHand().getType() != null || !(player.getItemInHand().getType().isTransparent())) {
            // The player is holding an opaque block, so let's make the pad with that type
            padMaterial = player.getItemInHand().getType();
        } else {
            // The player is not holding an opaque block, so get the configured material
            padMaterial = Material.getMaterial(nexus.getConfig().getString("padmaterial"));
        }
        here = player.getWorld().getBlockAt(createLocationFromPlayer(player));
        // Place the block below the player
        here.getRelative(BlockFace.DOWN).setType(padMaterial);
        // Place the pressure plate on the block
        here.setType(Material.STONE_PLATE);
        return here.getLocation();
    }

    public void createPad (Player player, boolean hall, String town) {
        if (hall) {
            // Make the pad where the player is
            hallPadLocation=createPadAtPlayer(player);
        } else {
            townPadLocation=createPadAtPlayer(player);
        }
        // Mark this NexusPair as established if the other location has also been created
        if (townPadLocation != null && hallPadLocation != null) {
            established = true;
            Nexus.msgPlayer(player, String.format("Nexus created for %s; distance is %s", town, townPadLocation.distance(hallPadLocation)));
        }
    }
}
