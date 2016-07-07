package me.desht.checkers.model;

import me.desht.checkers.model.rules.GameRules;

public interface Position {
	PieceType getPieceAt(int row, int col);
	PieceType getPieceAt(RowCol square);
	Move[] getLegalMoves();
	void makeMove(Move moves);
	Move[] getMoveHistory();
	Move getLastMove();
	PlayerColour getToMove();
	void newGame();
	boolean isJumpInProgress();
	void addPositionListener(PositionListener listener);
	void undoLastMove(int nMoves);
	int getPlyCount();
	int getHalfMoveClock();
	Position tryMove(Move move);
	GameRules getRules();
	void setRules(String ruleId);
	boolean isMarkedCaptured(RowCol square);
	int getBoardSize();
}
