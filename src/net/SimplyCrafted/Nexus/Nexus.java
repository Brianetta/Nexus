package net.SimplyCrafted.Nexus;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

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

public class Nexus extends JavaPlugin {

    // Map of serialized coordinates to town names
    public HashMap <String, String> NexusMap;

    // Cooldown map; players hashed here have just teleported, and must wait before
    // using another Nexus pad
    public HashMap <String, Boolean> Cooldown;

    public Nexus() {
        NexusMap = new HashMap<String, String>();
        Cooldown = new HashMap<String, Boolean>();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        populateNexusMap();
        // Register the handler that detects the player treading on a pad
        getServer().getPluginManager().registerEvents(new NexusListener(this), this);
    }

    private void populateNexusMap() {
        NexusHandler nexusHandler;
        for (String configKeys : getConfig().getKeys(true)) {
            // Magic numbers:
            // 5 is the position of the '.' in "pairs."
            // 6 is the position of the next character.
            if (configKeys.startsWith("pairs.") && configKeys.lastIndexOf(".") == 5) {
                // configKeys.substring(6) is a town name
                nexusHandler = new NexusHandler(this,configKeys.substring(6),null);
                // Hash the serialized string version of a location with its actual paired destination
                NexusMap.put(nexusHandler.getHashHallLocation(), nexusHandler.getName());
                NexusMap.put(nexusHandler.getHashTownLocation(), nexusHandler.getName());
                getLogger().info("Loaded and hashed Nexus for: " + nexusHandler.getName());
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Saving Nexus info");
        saveConfig();
        getLogger().info("Clearing Nexus hash");
        NexusMap.clear();
        Cooldown.clear();
        PlayerInteractEvent.getHandlerList().unregister(this);
    }

    public static void msgPlayer(Player player,String msg) {
        if (!msg.isEmpty()) player.sendMessage(ChatColor.GOLD + "[Nexus] " + ChatColor.GRAY + msg.replace('_',' '));
    }

    // Check whether player has just been teleported
    public boolean isLocked(String player) {
        return Cooldown.containsKey(player);
    }

    // Schedulable runnable that removes the lock
    private class Unlock implements Runnable {
        private final String name;
        private final Nexus nexus;

        Unlock (Nexus nexus, String name) {
            this.name = name;
            this.nexus = nexus;
        }

        public void run() {
            nexus.Cooldown.remove(name);
        }
    }

    // Place a lock on a player (a flag in a hash) then schedule a task to remove the lock
    public void lock(String player) {
        Cooldown.put(player, true);
        // Schedule unlock by "delay" seconds
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Unlock(this,player),10 * getConfig().getLong("cooldown"));
    }

    // List all of the Nexus pairs that have been defined
    public void listNexus (Player player, int page) {
        final int linesPerPage=9; // Pagination constant
        NexusHandler pair;
        SortedSet<String> sortedKeys=new TreeSet<String>(getConfig().getKeys(true));
        int counter=0; // I'm so sorry, please forgive me. )-:
        for (String configKeys : sortedKeys) {
            // Magic numbers:
            // 5 is the position of the '.' in "pairs."
            // 6 is the position of the next character.
            if (configKeys.startsWith("pairs.") && configKeys.lastIndexOf(".") == 5) {
                if (counter >= (page-1)*linesPerPage && counter < page*linesPerPage) {
                    // configKeys.substring(6) is a town name
                    pair = new NexusHandler(this,configKeys.substring(6),player);
                    pair.shortInfo();
                }
                counter++; // I know, this hurts
            }
        }
        msgPlayer(player,String.format("Page %d of %d",page,(counter/linesPerPage)+1));
    }

    public void listNexus (Player player) {
        listNexus(player,1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("Nexus")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command is not intended for console use");
                return true;
            }
            Player player = (Player) sender;
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("build")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 3) {
                            if (args[2].equalsIgnoreCase("town")) {
                                msgPlayer(player, "Building town pad for " + args[1]);
                                NexusHandler pair = new NexusHandler(this,args[1],player);
                                pair.createPad(false);
                                pair.close();
                            } else if (args[2].equalsIgnoreCase("hall")) {
                                msgPlayer(player, "Building hall pad for " + args[1]);
                                NexusHandler pair = new NexusHandler(this, args[1], player);
                                pair.createPad(true);
                                pair.close();
                            } else {
                                msgPlayer(player, "You must specify a name, followed by either the word \"hall\" or the word \"town\"");
                            }
                        }
                    } else {
                        msgPlayer(player, "You don't have permission to build a Nexus pad");
                    }
                } else if (args[0].equalsIgnoreCase("destroy")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 2) {
                            msgPlayer(player, "Destroying both pads for " + args[1]);
                            NexusHandler pair = new NexusHandler(this,args[1],player);
                            pair.remove();
                        } else {
                            msgPlayer(player, "You must specify a name");
                        }
                    } else {
                        msgPlayer(player, "You do not have permission to destroy a Nexus");
                    }
                } else if (args[0].equalsIgnoreCase("override")) {
                    if (player.hasPermission("Nexus.create")) {
                        // Force teleport to other pad, even if they have been physically damaged,
                        // but only for players who can build them
                        Location here = player.getLocation().getBlock().getLocation(); // The block is aligned, the player is not
                        String town = NexusMap.get(String.format("%s %f %f %f",
                                here.getWorld().getName(),
                                here.getX() + 0.5F,
                                here.getY() + 0.5F,
                                here.getZ() + 0.5F));
                        if (town != null) {
                            // We're on a pad location!
                            NexusHandler pair = new NexusHandler(this, town, player);
                            msgPlayer(player, "Forcing activation of "+town+" Nexus");
                            // Lock the player against an immediate return
                            lock(player.getName());
                            pair.teleportFurthest(true);
                        } else {
                            msgPlayer(player, "This is not a Nexus pad location");
                        }
                    } else {
                        msgPlayer(player, "You do not have permission to override a Nexus");
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 1)
                            listNexus(player);
                        else
                            listNexus(player, Integer.parseInt(args[1]));
                    } else {
                        msgPlayer(player, "You do not have permission to list all Nexus");
                    }
                } else if (args[0].equalsIgnoreCase("info")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 2) {
                            NexusHandler pair = new NexusHandler(this, args[1], player);
                            pair.longInfo();
                        } else {
                            msgPlayer(player, "you must specify a name");
                        }
                    } else {
                        msgPlayer(player, "You do not have permission to view Nexus details");
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (player.hasPermission("Nexus.admin")) {
                        msgPlayer(player,"Reloading config from disk!");
                        reloadConfig();
                        NexusMap.clear();
                        populateNexusMap();
                    } else {
                        msgPlayer(player,"You do not have permission to reload the Nexus mod");
                    }
                } else if (args[0].equalsIgnoreCase("rename")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 3) {
                            NexusHandler pair = new NexusHandler(this,args[1],player);
                            pair.rename(args[2]);
                            pair.close();
                        }
                    }
                }
            } else return false;
        }
        return true;
    }
}
