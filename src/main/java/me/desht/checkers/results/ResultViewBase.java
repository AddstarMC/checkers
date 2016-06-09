package me.desht.checkers.results;

import me.desht.checkers.CheckersException;
import me.desht.checkers.ai.CheckersAI;
import me.desht.dhutils.Debugger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Abstract base class to represent a view on the raw results data.  Subclass
 * this and implement the addResult() and getInitialScore() methods.
 */
public abstract class ResultViewBase {
	private final Results handler;
	private final String viewType;
	private final Map<String, Integer> scoreMap;
	private boolean updateDatabase = true;

	ResultViewBase(Results handler, String viewType) {
		this.viewType = viewType;
		this.handler = handler;
		this.scoreMap = new HashMap<String, Integer>();
	}

	public abstract void addResult(ResultEntry re);

	protected abstract int getInitialScore();

	/**
	 * @return the viewType
	 */
	public String getViewType() {
		return viewType;
	}

	/**
	 * Rebuild this view's data from the raw result records.  This is done right after the database data
	 * has been reloaded.
	 */
	void rebuild() {
		// don't push out database updates here; we've only just read the data in
		updateDatabase = false;
		for (ResultEntry re : handler.getEntries()) {
			addResult(re);
		}
		updateDatabase = true;
	}

	/**
	 * Get the top N scores on the server, highest first.
	 *
	 * @param count	The number of players to return
	 * @param excludeAI true if AI scores should be excluded
	 * @return a List of {@link ScoreRecord}s
	 * @throws CheckersException if called before data has finished being restored from DB
	 */
	public List<ScoreRecord> getScores(final int count, final boolean excludeAI) {
		if (!handler.isDatabaseLoaded()) {
			throw new CheckersException("No results data is available yet");
		}
		List<ScoreRecord> res = new ArrayList<ScoreRecord>();

		List<Entry<String, Integer>> list = new ArrayList<Entry<String,Integer>>();
		for (Entry<String,Integer> entry : scoreMap.entrySet()) {
			if (excludeAI && CheckersAI.isAIPlayer(entry.getKey())) {
				continue;
			}
			list.add(entry);
		}
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> m1, Entry<String, Integer> m2) {
				return (m2.getValue()).compareTo(m1.getValue());
			}
		});
		int n = 0;
		for (Entry<String,Integer> entry : list) {
			if (count > 0 && n++ > count) {
				break;
			}
			res.add(new ScoreRecord(entry.getKey(), entry.getValue()));
		}

		return res;
	}

	protected void awardPoints(String player, int score) {
		int current = getScore(player);
		setScore(player, current + score);
	}

	/**
	 * Set the score for the given player.
	 *
	 * @param player player's name
	 * @param score player's score
	 */
	public void setScore(String player, int score) {
		scoreMap.put(player, score);
		if (updateDatabase) {
			handler.queueDatabaseUpdate(new ViewScoreUpdate(player, score, viewType));
		}
	}

	/**
	 * Get the score for the given player.  If the player is not yet in the database,
	 * return the initial score (and add the player with that score).
	 *
	 * @param player	The player to check for
	 * @return			The player's score
	 */
	public int getScore(String player) {
		if (!scoreMap.containsKey(player)) {
			scoreMap.put(player, getInitialScore());
		}
		return scoreMap.get(player);
	}

	private class ViewScoreUpdate implements DatabaseSavable {
		private final String player;
		private final int score;
		private final String tableName;

		private ViewScoreUpdate(String player, int score, String tableName) {
			this.player = player;
			this.score = score;
			this.tableName = tableName;
		}

		@Override
		public void saveToDatabase(Connection conn) throws SQLException {
			String fullName = Results.getResultsHandler().getTableName(tableName);
			PreparedStatement getPlayer = conn.prepareStatement("SELECT player, score FROM " + fullName + " WHERE player = ?");
			getPlayer.setString(1, player);
			ResultSet rs = getPlayer.executeQuery();
			PreparedStatement update;
			if (!rs.next()) {
				// new insertion
				update = conn.prepareStatement("INSERT INTO " + fullName + " VALUES (?,?)");
				update.setString(1, player);
				update.setInt(2, score);
			} else if (score != rs.getInt(2)) {
				// update existing
				update = conn.prepareStatement("UPDATE " + fullName + " SET score = ? WHERE player = ?");
				update.setString(2, player);
				update.setInt(1, score);
			} else {
				return;
			}
			Debugger.getInstance().debug("execute SQL: " + update);
			update.executeUpdate();
		}
	}
}
