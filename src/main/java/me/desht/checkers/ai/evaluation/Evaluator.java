package me.desht.checkers.ai.evaluation;

import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.Position;

public interface Evaluator {
	/**
	 * Evaluate the given position from the point of view of the given colour.
	 *
	 * @param position
	 * @return
	 */
	public int evaluate(Position position, PlayerColour colour);
}