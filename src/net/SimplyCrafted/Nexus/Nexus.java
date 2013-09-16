package net.SimplyCrafted.Nexus;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

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

    public Nexus() {
        NexusMap = new HashMap<String, String>();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        NexusCreator nexusCreator;
        for (String configKeys : getConfig().getKeys(true)) {
            // Magic numbers:
            // 5 is the position of the '.' in "pairs."
            // 6 is the position of the next character.
            if (configKeys.startsWith("pairs.") && configKeys.lastIndexOf(".") == 5) {
                // configKeys.substring(6) is a town name
                nexusCreator = new NexusCreator(this,configKeys.substring(6),null);
                // Hash the serialized string version of a location with its actual paired destination
                NexusMap.put(nexusCreator.getSerializedHallLocation(), nexusCreator.getName());
                NexusMap.put(nexusCreator.getSerializedTownLocation(), nexusCreator.getName());
                getLogger().info("Loaded and hashed Nexus for: " + nexusCreator.getName());
            }
        }
        // Register the handler that detects the player treading on a pad
        getServer().getPluginManager().registerEvents(new NexusListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Saving Nexus info");
        saveConfig();
        getLogger().info("Clearing Nexus hash");
        NexusMap.clear();
        PlayerInteractEvent.getHandlerList().unregister((Listener) this);
    }

    public static void msgPlayer(Player player,String msg) {
        if (!msg.isEmpty()) player.sendMessage(ChatColor.GOLD + "[Nexus] " + ChatColor.GRAY + msg);
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
                                NexusCreator pair = new NexusCreator(this,args[1],player);
                                pair.createPad(false);
                                pair.close();
                            } else if (args[2].equalsIgnoreCase("hall")) {
                                msgPlayer(player, "Building hall pad for " + args[1]);
                                NexusCreator pair = new NexusCreator(this,args[1],player);
                                pair.createPad(true);
                                pair.close();
                            }
                        }
                    } else {
                        msgPlayer(player, "You don't have permission to build a Nexus pad");
                    }
                }
            } else return false;
        }
        return true;
    }
}
