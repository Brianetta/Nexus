package net.SimplyCrafted.Nexus;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
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
        NexusMap = new HashMap<>();
        Cooldown = new HashMap<>();
    }

    public Towny towny;
    public Boolean noTowny = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getConfig().getConfigurationSection("pairs") == null) {
            getConfig().createSection("pairs");
        }
        populateNexusMap();
        String chatcolor;
        Boolean colormatch = false;

        // Register the handler that detects the player treading on a pad
        getServer().getPluginManager().registerEvents(new NexusListener(this), this);

        towny = (Towny) this.getServer().getPluginManager().getPlugin("Towny");
        noTowny = (towny == null);
        if (noTowny) {
            getLogger().info("Towny not found - won't be using Mayor functions");
        } else {
            getLogger().info("Towny found - Mayor functions available");
            getServer().getPluginManager().registerEvents(new TownyListener(towny), this);
        }

        // Error checking: Make sure that the config contains a valid color
        chatcolor = getConfig().getString("chatcolor");
        for (ChatColor testcolor : ChatColor.values()) {
            if (testcolor.toString().equals(chatcolor)) colormatch = true;
        }
        if (!colormatch) getConfig().set("chatcolor", "GOLD"); // Default value, in case the config contains junk
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
        if (!noTowny) RenameTownEvent.getHandlerList().unregister(this);
    }

    public void msgPlayer(Player player,String msg) {
        if (player == null) return; // It can happen, in a Towny event handler

        // Can't override a default config value with an empty one, so
        // unfortunately we need a magic value. The word "empty".
        if (!(msg.isEmpty() || msg.equalsIgnoreCase("empty"))) player.sendMessage(ChatColor.valueOf(getConfig().getString("chatcolor")) + "[" + this.getName() + "] " + ChatColor.GRAY + msg.replace('_',' '));
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
        SortedSet<String> sortedKeys=new TreeSet<>(getConfig().getConfigurationSection("pairs").getKeys(false));
        int counter=0; // I'm so sorry, please forgive me. )-:
        for (String configKeys : sortedKeys) {
            if (counter >= (page-1)*linesPerPage && counter < page*linesPerPage) {
                pair = new NexusHandler(this,configKeys,player);
                pair.shortInfo();
            }
            counter++; // I know, this hurts
        }
        msgPlayer(player,String.format("Page %d of %d",page,(counter/linesPerPage)+1));
    }

    public void listNexus (Player player) {
        listNexus(player,1);
    }

    String townBelongingTo(Player player) {
        if (noTowny) return null;
        try {
            Resident resident;
            resident = TownyUniverse.getDataSource().getResident(player.getName());
            String townName = resident.getTown().getName();
            if ((resident.isMayor() &&
                  (townName.equals(TownyUniverse.getTownName(player.getLocation()))))) {
                return townName;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    Boolean getPaymentFromTown(Player player, NexusHandler pair) {
        Integer feeInt;
        double fee;
        try {
            Town town = TownyUniverse.getDataSource().getTown(pair.getName());
            String testPad = null;
            try {
                testPad = TownyUniverse.getTownName(pair.getTownPadLocation());
            }
            catch (Exception testPadOK) {
                // *Shrug* Don't care.
            }
            if (!(town.getName().equals(testPad)) && pair.getTownPadLocation() != null) {
                // The town pad exists, but isn't within the town. The mayor might be
                // attempting to subvert a non-town Nexus with their town name.
                // Just abort.
                msgPlayer(player, getConfig().getString("messages.nameproblem"));
                return false;
            }
            if (pair.isEstablished()) {
                feeInt = getConfig().getInt("townyfeemove");
                msgPlayer(player, String.format(getConfig().getString("messages.takefeemove"),feeInt));
            } else {
                feeInt = getConfig().getInt("townyfeenew");
                msgPlayer(player, String.format(getConfig().getString("messages.takefeenew"),feeInt));
            }
            fee = feeInt.doubleValue();
            if (town.canPayFromHoldings(fee)) {
                msgPlayer(player, String.format(getConfig().getString("messages.paidfromtownbank"),feeInt));
                town.pay(fee, "Nexus");
                if (!pair.isEstablished()) {
                    PluginCommand modreq = this.getServer().getPluginCommand("modreq");
                    if (modreq != null) {
                        String[] modreqContent = {String.format(getConfig().getString("messages.modreq"),town.getName())};
                        modreq.execute(player, "modreq", modreqContent);
                    }
                }
                return true;
            } else {
                msgPlayer(player, String.format(getConfig().getString("messages.towncannotafford"),feeInt));
            }
        } catch (Exception e) {
            return false;
        }
        return false;
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
                    String townName = townBelongingTo(player);
                    if (!(townName == null) && args.length == 1) {
                        // If the town name already exists in the config, unify its case.
                        for (String configuredName : getConfig().getConfigurationSection("pairs").getKeys(false)) {
                            if (configuredName.equalsIgnoreCase(townName)) {
                                townName = configuredName; // Force the name to have original case
                            }
                        }
                        // Player is a town mayor in his town, and only issued /nexus build
                        if (player.getGameMode().equals(GameMode.CREATIVE)) {
                            msgPlayer(player, getConfig().getString("messages.nocreativebuild"));
                            return true;
                        }
                        msgPlayer(player, String.format(getConfig().getString("messages.buildingtown"), townName));
                        NexusHandler pair = new NexusHandler(this,townName,player);
                        if (getPaymentFromTown(player,pair)) {
                            if (pair.createPad(false)) {
                                pair.close();
                            } else {
                                msgPlayer(player, getConfig().getString("messages.alreadythere"));
                            }
                        }
                    } else if (player.hasPermission("Nexus.create")) {
                        if (args.length == 3) {
                            if (args[2].equalsIgnoreCase("town")) {
                                // If the town name already exists in the config, unify its case.
                                townName = args[1];
                                for (String configuredName : getConfig().getConfigurationSection("pairs").getKeys(false)) {
                                    if (configuredName.equalsIgnoreCase(townName)) {
                                        townName = configuredName; // Force the name to have original case
                                    }
                                }
                                msgPlayer(player, String.format(getConfig().getString("messages.buildingtown"), townName));
                                NexusHandler pair = new NexusHandler(this,townName,player);
                                if (pair.createPad(false)) {
                                    pair.close();
                                } else {
                                    msgPlayer(player, getConfig().getString("messages.alreadythere"));
                                }
                            } else if (args[2].equalsIgnoreCase("hall")) {
                                // If the town name already exists in the config, unify its case.
                                townName = args[1];
                                for (String configuredName : getConfig().getConfigurationSection("pairs").getKeys(false)) {
                                    if (configuredName.equalsIgnoreCase(townName)) {
                                        townName = configuredName; // Force the name to have original case
                                    }
                                }
                                msgPlayer(player, String.format(getConfig().getString("messages.buildinghall"), townName));
                                NexusHandler pair = new NexusHandler(this, townName, player);
                                if (pair.createPad(true)) {
                                    pair.close();
                                } else {
                                    msgPlayer(player, getConfig().getString("messages.alreadythere"));
                                }
                            } else {
                                msgPlayer(player, getConfig().getString("messages.buildhelp"));
                            }
                        } else {
                            msgPlayer(player, getConfig().getString("messages.buildhelp"));
                        }
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtobuild"));
                    }
                } else if (args[0].equalsIgnoreCase("destroy")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 2) {
                            String townName = args[1];
                            for (String configuredName : getConfig().getConfigurationSection("pairs").getKeys(false)) {
                                if (configuredName.equalsIgnoreCase(townName)) {
                                    townName = configuredName; // Force the name to have original case
                                }
                            }
                            msgPlayer(player, String.format(getConfig().getString("messages.destroying"), args[1]));
                            NexusHandler pair = new NexusHandler(this,townName,player);
                            pair.remove();
                        } else {
                            msgPlayer(player, getConfig().getString("messages.specifyname"));
                        }
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtodestroy"));
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
                            msgPlayer(player, String.format(getConfig().getString("messages.forcing"),town));
                            // Lock the player against an immediate return
                            lock(player.getName());
                            pair.teleportFurthest(true);
                        } else {
                            msgPlayer(player, getConfig().getString("messages.nopadhere"));
                        }
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtooverride"));
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 1)
                            listNexus(player);
                        else {
                            int page;
                            try {
                                page = Integer.parseInt(args[1]);
                            }
                            catch (NumberFormatException e){
                                page = 1;
                            }
                            listNexus(player,page);
                        }
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtolist"));
                    }
                } else if (args[0].equalsIgnoreCase("info")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 2) {
                            String townName = args[1];
                            for (String configuredName : getConfig().getConfigurationSection("pairs").getKeys(false)) {
                                if (configuredName.equalsIgnoreCase(townName)) {
                                    townName = configuredName; // Force the name to have original case
                                }
                            }
                            NexusHandler pair = new NexusHandler(this, townName, player);
                            pair.longInfo();
                        } else {
                            msgPlayer(player, getConfig().getString("messages.specifyname"));
                        }
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtoview"));
                    }
                } else if (args[0].equalsIgnoreCase("warp")) {
                    if (player.hasPermission("Nexus.warp")) {
                        if (args.length == 2) {
                            String townName = args[1];
                            for (String configuredName : getConfig().getConfigurationSection("pairs").getKeys(false)) {
                                if (configuredName.equalsIgnoreCase(townName)) {
                                    townName = configuredName; // Force the name to have original case
                                }
                            }
                            NexusHandler pair = new NexusHandler(this, townName, player);
                            pair.warpTo();
                        } else {
                            msgPlayer(player, getConfig().getString("messages.specifyname"));
                        }
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtowarp"));
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (player.hasPermission("Nexus.admin")) {
                        msgPlayer(player, getConfig().getString("messages.reloading"));
                        reloadConfig();
                        NexusMap.clear();
                        populateNexusMap();
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtoreload"));
                    }
                } else if (args[0].equalsIgnoreCase("rename")) {
                    if (player.hasPermission("Nexus.create")) {
                        if (args.length == 3) {
                            String townName = args[1];
                            for (String configuredName : getConfig().getConfigurationSection("pairs").getKeys(false)) {
                                if (configuredName.equalsIgnoreCase(townName)) {
                                    townName = configuredName; // Force the name to have original case
                                }
                            }
                            NexusHandler pair = new NexusHandler(this,townName,player);
                            pair.rename(args[2]);
                            pair.close();
                        }
                    } else {
                        msgPlayer(player, getConfig().getString("messages.nopermtorename"));
                    }
                }
            } else return false;
        }
        return true;
    }
}
