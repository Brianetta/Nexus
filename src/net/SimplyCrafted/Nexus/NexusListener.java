package net.SimplyCrafted.Nexus;

import org.bukkit.Material;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEvent (PlayerInteractEvent event) {
        // Check whether the block still exists
        if (event.getClickedBlock() == null) return;
        // Check the player did something to a stone pressure plate
        if (!(event.getClickedBlock().getState().getType() == Material.STONE_PLATE)) return;
        // Check that that something involved, you know, feet
        if (!(event.getAction() == Action.PHYSICAL)) return;
        Nexus.msgPlayer(event.getPlayer(), "Pressure plate activated. I saw it.");
    }

}
