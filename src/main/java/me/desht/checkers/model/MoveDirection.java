package me.desht.checkers.model;

/**
 * Represents the four directions a checkers piece may move, with the
 * associated row/column offsets.
 */
public enum MoveDirection {
	NORTH_EAST(1, 1),
	NORTH_WEST(1, -1),
	SOUTH_EAST(-1, 1),
	SOUTH_WEST(-1, -1);

	private final int rowOff, colOff;

	MoveDirection(int rowOff, int colOff) {
		this.rowOff = rowOff;
		this.colOff = colOff;
	}

	/**
	 * @return the rowOff
	 */
	public int getRowOffset() {
		return rowOff;
	}

	/**
	 * @return the colOff
	 */
	public int getColOffset() {
		return colOff;
	}

	/**
	 * @param dist the dist to offset
	 * @return the rowOff
	 */
	public int getRowOffset(int dist) {
		return rowOff * dist;
	}

	/**
	 * @param dist the dist to offset
	 * @return the colOff
	 */
	public int getColOffset(int dist) {
		return colOff * dist;
	}
}
