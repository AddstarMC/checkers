package me.desht.checkers.model;

public interface PositionListener {
	void moveMade(Position position, Move move);
	void squareChanged(RowCol square, PieceType piece);
	void plyCountChanged(int plyCount);
	void toMoveChanged(PlayerColour toMove);
	void lastMoveUndone(Position position);
	void halfMoveClockChanged(int halfMoveClock);
}
