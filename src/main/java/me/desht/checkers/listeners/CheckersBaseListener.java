package me.desht.checkers.listeners;

import org.bukkit.event.Listener;

import me.desht.checkers.CheckersPlugin;

abstract class CheckersBaseListener implements Listener {
	final CheckersPlugin plugin;

	CheckersBaseListener(CheckersPlugin plugin) {
		this.plugin = plugin;
	}
}
