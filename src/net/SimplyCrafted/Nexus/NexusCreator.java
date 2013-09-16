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

public class NexusCreator {
    // This plugin was designed for the Simply Crafted SMP server, where a hall
    // contains one end of each nexus pad pair, and a town contains the other
    // end. The hall is, literally, a nexus. So, the two locations are stored
    // as hallPad and townPad, respectively.

    private final Nexus nexus; // A Nexus class instance is required for config
    private Location hallPadLocation, townPadLocation;
    private boolean established = false; // set true when both Locations are defined
    private boolean paid = false; // flag to show whether this Nexus has been paid for
    private String town;
    private Player player;

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

    public String getName () {
        return town;
    }

    private String serializeLocation (Location location) {
        if (location == null) return "null";
        // Truncate all the floats (discarding 0.5) for simplicity
        return String.format("%s %f %f %f %f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw());
    }

    public String getSerializedTownLocation () {
        return serializeLocation(townPadLocation);
    }

    public String getSerializedHallLocation () {
        return serializeLocation(hallPadLocation);
    }

    private Location unserializeLocation (String string) {
        String[] args;
        args = string.split(" ");
        if (args.length > 4) {
            // Parse to doubles
            String world = args[0];
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            float yaw = Float.parseFloat(args[4]);
            return new Location(nexus.getServer().getWorld(world),x,y,z,yaw,0F);
        } else {
            return null;
        }
    }

    public NexusCreator(Nexus instance, String name, Player creator) {
        this.nexus = instance;
        town = name;
        player = creator;
        String tempLoc;
        boolean establishFlag = false;
        tempLoc = nexus.getConfig().getString("pairs." + name + ".hallLocation");
        if (tempLoc != null) {
            hallPadLocation = unserializeLocation(tempLoc);
            establishFlag = true;
        }
        tempLoc = nexus.getConfig().getString("pairs." + name + ".townLocation");
        if (tempLoc != null) {
            townPadLocation = unserializeLocation(tempLoc);
            if (establishFlag) established = true;
        }
    }

    private Location createLocationFromPlayer () {
        Location location = player.getLocation();
        // Convert yaw to one of eight cardinal points for simplicity,
        // adding 23 degrees to yaw first to move boundary conditions
        // by roughly 1/16th of a circle
        //
        // Centre the Location on the block by adding 0.5
        location.setX((float) (Math.floor(location.getX()) + 0.5));
        location.setZ((float) (Math.floor(location.getZ()) + 0.5));
        location.setY((float) (Math.floor(location.getY()) + 0.5));
        location.setYaw((float) (45F * Math.floor((location.getYaw() + 22F) / 45F)));
        return location;
    }

    private Location createPadAtPlayer () {
        // Place a block under the player's feet, and pop a stone
        // pressure plate upon it.
        Material padMaterial;
        Block padBlock;
        if ((player.getItemInHand().getType() != null) && player.getItemInHand().getType().isBlock() && !(player.getItemInHand().getType().isTransparent())) {
            // The player is holding a block, so let's make the pad with that type.
            // XXX Need to find a way to exclude blocks that won't take a pressure plate.
            padMaterial = player.getItemInHand().getType();
        }
        else {
            // The player is not holding an opaque block, so get the configured material
            padMaterial = Material.getMaterial(nexus.getConfig().getString("padmaterial"));
            if (padMaterial == null) padMaterial = Material.LAPIS_BLOCK;
        }
        Location locationFromPlayer = createLocationFromPlayer();
        padBlock = player.getWorld().getBlockAt(locationFromPlayer);
        // Place the block below the player
        padBlock.getRelative(BlockFace.DOWN).setType(padMaterial);
        // Place the pressure plate on the block
        padBlock.setType(Material.STONE_PLATE);
        return locationFromPlayer;
    }

    public void createPad (boolean hall) {
        // Remove it from the main hash, it's not going to be valid now.
        // Don't particularly care if it fails, that just means it was
        // never hashed anyway,.
        nexus.NexusMap.remove(getSerializedTownLocation());
        nexus.NexusMap.remove(getSerializedHallLocation());
        if (hall) {
            // Remove previous pad, if there is one
            if (hallPadLocation != null) {
                // Destroy the blocks
                hallPadLocation.getBlock().getRelative(BlockFace.DOWN).setType(Material.STONE);
                hallPadLocation.getBlock().setType(Material.AIR);
            }
            // Make the new pad where the player is
            hallPadLocation=createPadAtPlayer();
        } else {
            // Remove previous pad, if there is one
            if (townPadLocation != null) {
                // Destroy the blocks
                townPadLocation.getBlock().getRelative(BlockFace.DOWN).setType(Material.STONE);
                townPadLocation.getBlock().setType(Material.AIR);
            }
            // Make the pad where the player is
            townPadLocation=createPadAtPlayer();
        }
        // Mark this Nexus as established if the other location has also been created
        if (townPadLocation != null && hallPadLocation != null) {
            established = true;
            Nexus.msgPlayer(player, String.format("Nexus created for %s; distance is %.1fm", town, townPadLocation.distance(hallPadLocation)));
            // Hash the new locations
            nexus.NexusMap.put(getSerializedHallLocation(),town);
            nexus.NexusMap.put(getSerializedTownLocation(),town);
        }
    }

    public void close() {
        // Always call this method after you're done creating a Nexus.
        nexus.getConfig().set("pairs."+town+".hallLocation",serializeLocation(hallPadLocation));
        nexus.getConfig().set("pairs."+town+".townLocation",serializeLocation(townPadLocation));
    }
}
