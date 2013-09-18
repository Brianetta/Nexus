package net.SimplyCrafted.Nexus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

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

public class NexusHandler {
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
        return String.format("%s %f %f %f %f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw());
    }

    public String getSerializedTownLocation () {
        return serializeLocation(townPadLocation);
    }

    public String getSerializedHallLocation () {
        return serializeLocation(hallPadLocation);
    }

    // These next three are like the three above, but have no Yaw info.

    private String hashLocation (Location location) {
        if (location == null) return "null";
        return String.format("%s %f %f %f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public String getHashTownLocation () {
        return hashLocation(townPadLocation);
    }

    public String getHashHallLocation () {
        return hashLocation(hallPadLocation);
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

    public NexusHandler(Nexus instance, String name, Player creator) {
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
            nexus.NexusMap.put(getHashHallLocation(),town);
            nexus.NexusMap.put(getHashTownLocation(),town);
        }
    }

    public void remove () {
        // Remove pads and delete config for this pad
        hallPadLocation.getBlock().getRelative(BlockFace.DOWN).setType(Material.STONE);
        hallPadLocation.getBlock().setType(Material.AIR);
        townPadLocation.getBlock().getRelative(BlockFace.DOWN).setType(Material.STONE);
        townPadLocation.getBlock().setType(Material.AIR);
        nexus.getConfig().set("pairs."+town,null);
        nexus.NexusMap.remove(getHashHallLocation());
        nexus.NexusMap.remove(getHashTownLocation());
    }

    // Schedulable runnable that activates the teleport
    private class Teleport implements Runnable {
        private final Nexus nexus;
        private final String town;
        private final Player player;
        private final Location destination;
        private final Location source;

        Teleport (Nexus nexus, String town, Player player, Location source, Location destination) {
            this.nexus = nexus;
            this.town = town;
            this.player = player;
            this.destination = destination;
            this.source = source;
        }

        public void run() {
            source.setY(source.getY() - 0.5); // Bring it to ground level
            if (destination.getBlock().getType() != Material.STONE_PLATE) {
                nexus.msgPlayer(player,"Traveling by "+town+" Nexus pad disabled; pressure plate is missing at other end");
                return;
            }
            if (player.getLocation().distance(source) < 0.5) {
                // Player's on the pad
                player.teleport(destination);
                nexus.msgPlayer(player,"Traveled using the "+town+" Nexus pad");
            } else {
                nexus.msgPlayer(player,"Nexus transport failed; "+player.getName()+" wasn't standing in the middle");
            }
        }
    }

    public void teleportFurthest() {
        // Move player to opposite pad

        // Copy location
        Location playerLocation = player.getLocation().clone();
        Location source, destination;
        if (playerLocation.distanceSquared(townPadLocation) > playerLocation.distanceSquared(hallPadLocation)) {
            // Hall pad is nearest
            source = hallPadLocation.clone();
            destination = townPadLocation.clone();
        } else {
            // Town pad is nearest, or as far
            source = townPadLocation.clone();
            destination = hallPadLocation.clone();
        }
        // Make it as transparent as possible
        destination.setPitch(playerLocation.getPitch());

        // Go!  Magic number 5 is about how many ticks it takes a walking player to get onto the pad. Trial and error.
        nexus.getServer().getScheduler().scheduleSyncDelayedTask(nexus, new Teleport(nexus, town, player, source, destination),5);
    }

    public void close() {
        // Always call this method after you're done creating a Nexus.
        nexus.getConfig().set("pairs."+town+".hallLocation",serializeLocation(hallPadLocation));
        nexus.getConfig().set("pairs."+town+".townLocation",serializeLocation(townPadLocation));
    }
}
