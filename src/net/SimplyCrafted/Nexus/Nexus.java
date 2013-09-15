package net.SimplyCrafted.Nexus;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
    @Override
    public void onEnable() {
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        saveConfig();
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
            sender.sendMessage("OK.");
        }
        return true;
    }
}
