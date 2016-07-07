package me.desht.checkers.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.PersistenceHandler;
import me.desht.checkers.event.CheckersBoardCreatedEvent;
import me.desht.checkers.event.CheckersBoardDeletedEvent;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.util.TerrainBackup;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BoardViewManager {

	private static BoardViewManager instance = null;

	private final Map<String, BoardView> gameBoards = new HashMap<>();
	private final Map<String, Set<File>> deferred = new HashMap<>();
	private PersistableLocation globalTeleportOutDest = null;

	private BoardViewManager() {
	}

	public static synchronized BoardViewManager getManager() {
		if (instance == null) {
			instance = new BoardViewManager();
		}
		return instance;
	}

	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * @return the globalTeleportOutDest
	 */
	public Location getGlobalTeleportOutDest() {
		return globalTeleportOutDest == null ? null : globalTeleportOutDest.getLocation();
	}

	/**
	 * @param globalTeleportOutDest the globalTeleportOutDest to set
	 */
	public void setGlobalTeleportOutDest(Location globalTeleportOutDest) {
		this.globalTeleportOutDest = globalTeleportOutDest == null ? null : new PersistableLocation(globalTeleportOutDest);
	}

	public void registerView(BoardView view) {
		gameBoards.put(view.getName(), view);

		Bukkit.getPluginManager().callEvent(new CheckersBoardCreatedEvent(view));
	}

	public void unregisterBoardView(String name) {
		BoardView bv;
		try {
			bv = getBoardView(name);
			gameBoards.remove(name);
			Bukkit.getPluginManager().callEvent(new CheckersBoardDeletedEvent(bv));
		} catch (CheckersException  e) {
			LogUtils.warning("removeBoardView: unknown board name " + name);
		}
	}

	public void removeAllBoardViews() {
		for (BoardView bv : listBoardViews()) {
			Bukkit.getPluginManager().callEvent(new CheckersBoardDeletedEvent(bv));
		}
		gameBoards.clear();
	}

	public boolean boardViewExists(String name) {
		return gameBoards.containsKey(name);
	}

	public BoardView getBoardView(String name) throws CheckersException {
		if (!gameBoards.containsKey(name)) {
			throw new CheckersException(Messages.getString("Board.noSuchBoard", name));
		}
		return gameBoards.get(name);
	}

	public Collection<BoardView> listBoardViews() {
		return gameBoards.values();
	}

	public Collection<BoardView> listBoardViewsSorted() {
		SortedSet<String> sorted = new TreeSet<>(gameBoards.keySet());
		List<BoardView> res = new ArrayList<>();
		for (String name : sorted) {
			res.add(gameBoards.get(name));
		}
		return res;
	}

	/**
	 * Get a board that does not have a game running.
	 *
	 * @return the first free board found
	 * @throws CheckersException if no free board was found
	 */
	public BoardView getFreeBoard() throws CheckersException {
		for (BoardView bv : listBoardViews()) {
			if (bv.getGame() == null) {
				return bv;
			}
		}
		throw new CheckersException(Messages.getString("Board.noFreeBoards")); //$NON-NLS-1$
	}

	/**
	 * Check if a location is any part of any board including the frame and enclosure.
	 *
	 * @param loc	location to check
	 * @return the boardview that matches, or null if none
	 */
	public BoardView partOfBoard(Location loc) {
		return partOfBoard(loc, 0);
	}

	public BoardView partOfBoard(Location loc, int fudge) {
		for (BoardView bv : listBoardViews()) {
			if (bv.getBoard().isPartOfBoard(loc, fudge)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Check if location is above a board square but below the roof
	 *
	 * @param loc  location to check
	 * @return the boardview that matches, or null if none
	 */
	public BoardView aboveBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.getBoard().isAboveBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Check if location is part of a board square
	 *
	 * @param loc	location to check
	 * @return the boardview that matches, or null if none
	 */
	public BoardView onBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.getBoard().isOnBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	public BoardView findBoardForGame(CheckersGame game) {
		for (BoardView bv : listBoardViews()) {
			if (bv.getGame() != null && bv.getGame().getName().equals(game.getName())) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Teleport the player in a sensible manner, depending on where they are now.
	 *
	 * @param player the player to teleport
	 * @throws CheckersException if the player can't be teleported for any reason
	 */
	public void teleportOut(Player player) throws CheckersException {
		PermissionUtils.requirePerms(player, "checkers.commands.teleport");

		BoardView bv = partOfBoard(player.getLocation(), 0);
		Location prev = CheckersPlugin.getInstance().getPlayerTracker().getLastPos(player);
		if (bv != null && bv.hasTeleportOutDestination()) {
			// board has a specific location defined
			Location loc = bv.getTeleportOutDestination();
			CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, loc);
		} else if (bv != null && globalTeleportOutDest != null) {
			// maybe there's a global location defined
			CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, getGlobalTeleportOutDest());
		} else if (bv != null && (prev == null || partOfBoard(prev, 0) == bv)) {
			// try to get the player out of this board safely
			Location loc = bv.findSafeLocationOutside();
			if (loc != null) {
				CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, loc);
			} else {
				CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, player.getWorld().getSpawnLocation());
				MiscUtil.errorMessage(player, Messages.getString("Board.goingToSpawn"));
			}
		} else if (prev != null) {
			// go back to previous location
			CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, prev);
		} else {
			throw new CheckersException(Messages.getString("Board.notOnBoard"));
		}
	}

	/**
	 * Convenience method to create a new board and do all the associated setup tasks.
	 *
	 * @param boardName name of the board to create
	 * @param loc location of the origin (centre of A1 square)
	 * @param rotation the board rotation (which direction black faces)
	 * @param style the board style name
	 * @param size an int the size
	 * @return a fully initialised and painted board
	 */
	public BoardView createBoard(String boardName, Location loc, BoardRotation rotation, String style, int size) {
		BoardView view = new BoardView(boardName, loc, rotation, style, size);
		registerView(view);
		if (CheckersPlugin.getInstance().getWorldEdit() != null) {
			TerrainBackup.save(view);
		}
		view.save();
		view.repaint();

		return view;
	}

	/**
	 * Mark a board as deferred loading - its world wasn't available so we'll record the board
	 * file name for later.
	 *
	 * @param worldName name of the world
	 * @param f file from which the board is loaded
	 */
	public void deferLoading(String worldName, File f) {
		if (!deferred.containsKey(worldName)) {
			deferred.put(worldName, new HashSet<>());
		}
		deferred.get(worldName).add(f);
	}

	/**
	 * Load any deferred boards for the given world.
	 *
	 * @param worldName name of the world
	 */
	public void loadDeferred(String worldName) {
		if (!deferred.containsKey(worldName)) {
			return;
		}
		for (File f : deferred.get(worldName)) {
			LogUtils.info("Doing deferred board load for " + f);
			CheckersPlugin.getInstance().getPersistenceHandler().loadBoard(f);
		}
		deferred.get(worldName).clear();
	}

	/**
	 * Called when a world is unloaded.  Put any boards in that world back on the deferred list.
	 *
	 * @param worldName name of the world
	 */
	public void unloadBoardsForWorld(String worldName) {
		for (BoardView bv : new ArrayList<>(listBoardViews())) {
			if (bv.getWorldName().equals(worldName)) {
				bv.deleteTemporary();
				File f = new File(bv.getSaveDirectory(), PersistenceHandler.makeSafeFileName(bv.getName()) + ".yml");
				deferLoading(bv.getWorldName(), f);
				LogUtils.info("unloaded board '" + bv.getName() + "' (world has been unloaded)");
			}
		}
	}
}

