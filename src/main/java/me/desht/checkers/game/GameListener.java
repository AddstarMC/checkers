package me.desht.checkers.game;

import me.desht.checkers.model.RowCol;
import me.desht.checkers.player.CheckersPlayer;

public interface GameListener {
	void gameDeleted(CheckersGame game);
	void playerAdded(CheckersGame checkersGame, CheckersPlayer checkersPlayer);
	void gameStarted(CheckersGame checkersGame);
	boolean tryStakeChange(double newStake);
	void stakeChanged(double newStake);
	boolean tryTimeControlChange(String tcSpec);
	void timeControlChanged(String tcSpec);
	void selectSquare(RowCol square);
}
