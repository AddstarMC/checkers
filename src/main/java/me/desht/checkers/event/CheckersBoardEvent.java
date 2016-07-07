package me.desht.checkers.event;

import me.desht.checkers.view.BoardView;

public abstract class CheckersBoardEvent extends CheckersEvent {

	private final BoardView boardView;

	CheckersBoardEvent(BoardView boardView) {
		this.boardView = boardView;
	}

	public BoardView getBoardView() {
		return boardView;
	}
}
