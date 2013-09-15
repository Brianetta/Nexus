package net.SimplyCrafted.Nexus;

import org.bukkit.Location;

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
    private Location hallPadLocation, townPadLocation;
    private boolean established = false; // set true when both Locations are defined
    private boolean paid = false ; // flag to show whether this Nexus has been paid for

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
}
