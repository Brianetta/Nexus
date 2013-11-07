package net.SimplyCrafted.Nexus;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.List;

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
    private String town;
    private Player player;
    Material plateMaterial;

    public boolean isEstablished() {
        return established;
    }

    public String getName () {
        return town;
    }

    public void rename (String name) {
        Boolean doIActuallyExist = false;
        if (townPadLocation != null) {
            nexus.NexusMap.put(getHashTownLocation(), name);
            doIActuallyExist = true;
        }
        if (hallPadLocation != null) {
            nexus.NexusMap.put(getHashHallLocation(), name);
            doIActuallyExist = true;
        }
        if (doIActuallyExist) {
            nexus.msgPlayer(player, String.format(nexus.getConfig().getString("messages.renaming"), town, name));
        } else {
            nexus.msgPlayer(player, nexus.getConfig().getString("messages.nosuchnexus"));
        }
        nexus.getConfig().set("pairs."+name,nexus.getConfig().get("pairs."+town));
        nexus.getConfig().set("pairs."+town,null);
        town = name;
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
        if (tempLoc != null && !tempLoc.equalsIgnoreCase("null")) {
            hallPadLocation = unserializeLocation(tempLoc);
            establishFlag = true;
        }
        tempLoc = nexus.getConfig().getString("pairs." + name + ".townLocation");
        if (tempLoc != null && !tempLoc.equalsIgnoreCase("null")) {
            townPadLocation = unserializeLocation(tempLoc);
            if (establishFlag) established = true;
        }
        plateMaterial = Material.getMaterial(nexus.getConfig().getString("platematerial"));
        if (plateMaterial == null) plateMaterial = Material.STONE_PLATE;
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
        List <String> allowedBlocks = nexus.getConfig().getStringList("allowedblocks");
        if ((player.getItemInHand().getType() != null) && allowedBlocks.contains(player.getItemInHand().getType().name())) {
            // The player is holding a valid block, so let's make the pad with that type.
            padMaterial = player.getItemInHand().getType();
            // If the player is not in creative mode, take the block off them.
            if (!(player.getGameMode().equals(GameMode.CREATIVE))) {
                int stackHeight = player.getItemInHand().getAmount();
                // If there's more than one item in the player's hand,
                if (stackHeight > 1 ) {
                    // take one away
                    player.getItemInHand().setAmount(stackHeight-1);
                } else {
                    // otherwise take the only one away.
                    player.getInventory().remove(player.getItemInHand());
                }
            }
        }
        else {
            // The player is not holding an opaque block, so get the configured material
            padMaterial = Material.getMaterial(nexus.getConfig().getString("padmaterial"));
            if (padMaterial == null) padMaterial = Material.LAPIS_BLOCK;
        }
        Location locationFromPlayer = createLocationFromPlayer();
        padBlock = player.getWorld().getBlockAt(locationFromPlayer);
        // Break & place the block below the player
        Block underBlock = padBlock.getRelative(BlockFace.DOWN);
        underBlock.breakNaturally();
        underBlock.setType(padMaterial);
        // Place the pressure plate on the block
        padBlock.setType(plateMaterial);
        return locationFromPlayer;
    }

    private void breakPad (Location location) {
        if (location.getBlock().getState().getType() == plateMaterial)
        {
            location.getBlock().breakNaturally();
        }
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
                breakPad(hallPadLocation);
            }
            // Make the new pad where the player is
            hallPadLocation=createPadAtPlayer();
        } else {
            // Remove previous pad, if there is one
            if (townPadLocation != null) {
                // Destroy the blocks
                breakPad(townPadLocation);
            }
            // Make the pad where the player is
            townPadLocation=createPadAtPlayer();
        }
        // Mark this Nexus as established if the other location has also been created
        if (townPadLocation != null && hallPadLocation != null) {
            established = true;
            if (hallPadLocation.getWorld().equals(townPadLocation.getWorld()))
                nexus.msgPlayer(player, String.format(nexus.getConfig().getString("messages.createdinworld"), town, townPadLocation.distance(hallPadLocation)));
            else
                nexus.msgPlayer(player, String.format(nexus.getConfig().getString("messages.createdexworld"), town));
            // Hash the new locations
            nexus.NexusMap.put(getHashHallLocation(),town);
            nexus.NexusMap.put(getHashTownLocation(),town);
        }
    }

    public void remove () {
        // Remove pads and delete config for this pad
        if (hallPadLocation != null) {
            breakPad(hallPadLocation);
        }
        if (townPadLocation != null) {
            breakPad(townPadLocation);
        }
        nexus.getConfig().set("pairs."+town,null);
        nexus.NexusMap.remove(getHashHallLocation());
        nexus.NexusMap.remove(getHashTownLocation());
    }

    // Schedulable runnable that activates the teleport
    private class Teleport implements Runnable {
        private final String town;
        private final Player player;
        private final Location destination;
        private final Location source;
        private final boolean override;

        Teleport (String town, Player player, Location source, Location destination, boolean override) {
            this.town = town;
            this.player = player;
            this.destination = destination;
            this.source = source;
            this.override = override;
        }

        public void run() {
            source.setY(source.getY() - 0.5); // Bring it to ground level
            if (!(destination.getBlock().getType() == plateMaterial || override)) {
                nexus.msgPlayer(player,String.format(nexus.getConfig().getString("messages.disabled"),town));
                return;
            }
            if (player.getLocation().distance(source) < 0.5) {
                // Player's on the pad
                player.teleport(destination);
                nexus.msgPlayer(player,String.format(nexus.getConfig().getString("messages.traveling"),town));
            } else {
                nexus.msgPlayer(player, nexus.getConfig().getString("messages.aborted"));
            }
        }
    }

    public void shortInfo() {
        nexus.msgPlayer(player, String.format("%s (%s)", ChatColor.WHITE + getName() + ChatColor.GRAY, isEstablished() ? "Established" : "Incomplete"));
    }

    public void longInfo() {
        nexus.msgPlayer(player, ChatColor.GREEN + "Name: " + ChatColor.WHITE + town);
        if (hallPadLocation != null)
            nexus.msgPlayer(player, ChatColor.GREEN + "Hall: " + ChatColor.GRAY + String.format("%1.0f, %1.0f (height %1.0f) in \"%s\" world", hallPadLocation.getX(), hallPadLocation.getZ(), hallPadLocation.getY(), hallPadLocation.getWorld().getName()));
        else
            nexus.msgPlayer(player, ChatColor.GREEN + "Hall: " + ChatColor.GRAY + "Not yet defined");
        if (townPadLocation != null)
            nexus.msgPlayer(player, ChatColor.GREEN + "Town: " + ChatColor.GRAY + String.format("%1.0f, %1.0f (height %1.0f) in \"%s\" world", townPadLocation.getX(), townPadLocation.getZ(), townPadLocation.getY(), townPadLocation.getWorld().getName()));
        else
            nexus.msgPlayer(player, ChatColor.GREEN + "Town: " + ChatColor.GRAY + "Not yet defined");
    }

    public void teleportFurthest(boolean override) {
        // Move player to opposite pad

        // Copy location
        Location playerLocation = player.getLocation().clone();
        Location source, destination;
        if (hallPadLocation.getWorld().equals(playerLocation.getWorld()) && townPadLocation.getWorld().equals(playerLocation.getWorld())) {
            if (playerLocation.distanceSquared(townPadLocation) > playerLocation.distanceSquared(hallPadLocation)) {
                // Hall pad is nearest
                source = hallPadLocation.clone();
                destination = townPadLocation.clone();
            } else {
                // Town pad is nearest, or as far
                source = townPadLocation.clone();
                destination = hallPadLocation.clone();
            }
        } else if (hallPadLocation.getWorld().equals(playerLocation.getWorld())) {
            // Town pad location is in another world, so must be further away
            source = hallPadLocation;
            destination = townPadLocation;
        } else {
            // Hall pad location is in another world.
            source = townPadLocation;
            destination = hallPadLocation;
        }
        // Make it as transparent as possible
        destination.setPitch(playerLocation.getPitch());

        // Go!  Magic number 5 is about how many ticks it takes a walking player to get onto the pad. Trial and error.
        nexus.getServer().getScheduler().scheduleSyncDelayedTask(nexus, new Teleport(town, player, source, destination, override),5);
    }

    public void warpTo() {
        // Move the player directly to the town end, regardless of location.
        nexus.lock(player.getName());
        if (townPadLocation == null)
            nexus.msgPlayer(player, nexus.getConfig().getString("messages.nosuchnexus"));
        else
            player.teleport(townPadLocation);
    }

    public void close() {
        // Always call this method after you're done creating or renaming a Nexus.
        nexus.getConfig().set("pairs."+town+".hallLocation",serializeLocation(hallPadLocation));
        nexus.getConfig().set("pairs."+town+".townLocation",serializeLocation(townPadLocation));
    }
}
