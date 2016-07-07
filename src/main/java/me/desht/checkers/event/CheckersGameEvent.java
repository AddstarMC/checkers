package me.desht.checkers.event;

import me.desht.checkers.game.CheckersGame;

public abstract class CheckersGameEvent extends CheckersEvent {

	private final CheckersGame game;

	CheckersGameEvent(CheckersGame game) {
		this.game = game;
	}

	public CheckersGame getGame() {
		return game;
	}

}
