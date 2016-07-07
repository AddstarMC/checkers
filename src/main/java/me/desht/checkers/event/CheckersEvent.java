package me.desht.checkers.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

abstract class CheckersEvent extends Event {

	@Override
	public abstract HandlerList getHandlers();

}
