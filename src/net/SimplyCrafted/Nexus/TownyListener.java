package net.SimplyCrafted.Nexus;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Copyright © Brian Ronald
 * 28/11/13
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
public class TownyListener  implements Listener {
    private final Towny towny;

    public TownyListener(Towny instance) {
        this.towny = instance;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void on (RenameTownEvent event) {
        // This event handler renames a Nexus pair if:
        // 1. It has the same name as the town
        // 2. It has a Town pad within the town

        // Find out whether the town has a Nexus pair of the same name with
        // a configured Town pad
        NexusHandler pair = new NexusHandler((Nexus) towny.getServer().getPluginManager().getPlugin("Nexus"),event.getOldName(),null);
        Location padLocation = pair.getTownPadLocation();
        if (padLocation == null) return; // There *is* a Town pad
        // Now check whether that pad is within the town
        String testPad = null;
        try {
            testPad = towny.getTownyUniverse().getTownName(padLocation);
        }
        catch (Exception testPadOK) {
            // No town there? No need to rename, we can bomb out.
            return;
        }
        if (event.getTown().getName().equals(testPad)) {
            // The new name of the town matches the name of the town
            // in which the pad is located. Rename the Nexus.
            pair.rename(testPad);
            pair.close(); // Save changes (-:
        }
    }
}