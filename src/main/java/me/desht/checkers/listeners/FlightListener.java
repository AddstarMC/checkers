package me.desht.checkers.listeners;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.event.CheckersBoardCreatedEvent;
import me.desht.checkers.event.CheckersBoardDeletedEvent;
import me.desht.checkers.event.CheckersBoardModifiedEvent;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.block.BlockType;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class FlightListener extends CheckersBaseListener {

	private static final int MESSAGE_COOLDOWN = 5000;
	private static final int BOUNCE_COOLDOWN = 300;

	// notes if the player is currently allowed to fly due to being on/near a board
	// maps the player name to the previous flight speed for the player
	private final Map<String,PreviousSpeed> allowedToFly = new HashMap<String,PreviousSpeed>();
	// cache of the regions in which board flight is allowed
	private final List<Cuboid> flightRegions = new ArrayList<Cuboid>();
	// notes when a player was last messaged about flight, to reduce spam
	private final Map<String,Long> lastMessagedIn = new HashMap<String,Long>();
	private final Map<String,Long> lastMessagedOut = new HashMap<String,Long>();
	// notes when player was last bounced back while flying
	private final Map<String,Long> lastBounce = new HashMap<String, Long>();

	private boolean enabled;
	private boolean captive;

	public FlightListener(CheckersPlugin plugin) {
		super(plugin);

		enabled = plugin.getConfig().getBoolean("flying.allowed");
		captive = plugin.getConfig().getBoolean("flying.captive");
	}

	/**
	 * Globally enable or disable board flight for all players.
	 */
	public void setEnabled(boolean enabled) {
		if (enabled == this.enabled)
			return;

		if (enabled) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				setFlightAllowed(player, getFlightRegion(player.getLocation()) != null);
			}
		} else {
			for (String playerName : allowedToFly.keySet()) {
				Player player = Bukkit.getPlayerExact(playerName);
				if (player != null) {
					player.setAllowFlight(gameModeAllowsFlight(player));
					// restore previous flight/walk speed
					allowedToFly.get(playerName).restoreSpeeds();
					MiscUtil.alertMessage(player, Messages.getString("Flight.flightDisabledByAdmin"));
				}
			}
			allowedToFly.clear();
		}

		this.enabled = enabled;

	}

	/**
	 * Set the "captive" mode.  Captive prevents flying players from flying too far from a
	 * board.  Non-captive just disables flight if players try to fly too far.
	 * 
	 * @param captive
	 */
	public void setCaptive(boolean captive) {
		this.captive = captive;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerJoined(PlayerJoinEvent event) {
		if (!enabled)
			return;

		Player player = event.getPlayer();
		setFlightAllowed(player, getFlightRegion(player.getLocation()) != null);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLeft(PlayerQuitEvent event) {
		String playerName = event.getPlayer().getName();
		if (allowedToFly.containsKey(playerName)) {
			allowedToFly.get(playerName).restoreSpeeds();
			allowedToFly.remove(playerName);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		final Player player = event.getPlayer();
		final boolean isFlying = player.isFlying();
		if (event.getNewGameMode() != GameMode.CREATIVE && allowedToFly.containsKey(player.getName())) {
			// If switching away from creative mode and on/near a board, allow flight to continue.
			// Seems a delayed task is needed here - calling setAllowFlight() directly from the event handler
			// leaves getAllowFlight() returning true, but the player is still not allowed to fly.  (CraftBukkit bug?)
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					player.setAllowFlight(true);
					player.setFlying(isFlying);
				}
			});
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		//		long now = System.nanoTime();
		if (!enabled)
			return;

		Location from = event.getFrom();
		Location to = event.getTo();

		// we only care if the player has actually moved to a different block
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
			return;
		}

		Player player = event.getPlayer();
		boolean flyingNow = allowedToFly.containsKey(player.getName()) && player.isFlying();
		boolean boardFlightAllowed = getFlightRegion(to) != null;
		boolean otherFlightAllowed = gameModeAllowsFlight(player);

		//		LogUtils.fine("move: boardflight = " + boardFlightAllowed + " otherflight = " + otherFlightAllowed);
		if (captive) {
			// captive mode - if flying, prevent movement too far from a board by bouncing the
			// player towards the centre of the board they're trying to leave
			if (flyingNow && !boardFlightAllowed && !otherFlightAllowed) {
				Long last = lastBounce.get(player.getName());
				if (last == null) last = 0L;
				if (System.currentTimeMillis() - last > BOUNCE_COOLDOWN) {
					event.setCancelled(true);
					Cuboid c = getFlightRegion(from);
					Location origin = c == null ? from : c.getCenter().subtract(0, c.getSizeY(), 0);
					Vector vec = origin.toVector().subtract(to.toVector()).normalize();
					player.setVelocity(vec);
					lastBounce.put(player.getName(), System.currentTimeMillis());
				}
			} else {
				setFlightAllowed(player, boardFlightAllowed);
			}
		} else {
			// otherwise, free movement, but flight cancelled if player moves too far
			setFlightAllowed(player, boardFlightAllowed);
		}
		//		System.out.println("move handler: " + (System.nanoTime() - now) + " ns");
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!enabled)
			return;

		final Player player = event.getPlayer();
		final boolean boardFlightAllowed = getFlightRegion(event.getTo()) != null;
		final boolean crossWorld = event.getTo().getWorld() != event.getFrom().getWorld();

		LogUtils.fine("teleport: boardflight = " + boardFlightAllowed + ", crossworld = " + crossWorld);

		// Seems a delayed task is needed here - calling setAllowFlight() directly from the event handler
		// leaves getAllowFlight() returning true, but the player is still not allowed to fly.  (CraftBukkit bug?)
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				if (crossWorld) {
					// Player flight seems to be automatically disabled when the world changes, so in that case we
					// force a re-enablement.  Without this, the following call to setFlightAllowed() would be ignored.
					setFlightAllowed(player, false);
				}
				setFlightAllowed(player, boardFlightAllowed);
			}
		});
	}

	/**
	 * Prevent the player from interacting with any block outside a board while enjoying temporary flight.  It's
	 * called early (priority LOWEST) to cancel the event ASAP.
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFlyingInteraction(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (allowedToFly.containsKey(player.getName()) && !gameModeAllowsFlight(player) && player.isFlying()) {
			if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (BoardViewManager.getManager().partOfBoard(event.getClickedBlock().getLocation(), 0) == null) {
					MiscUtil.errorMessage(player, Messages.getString("Flight.interactionStopped"));
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onBoardCreated(CheckersBoardCreatedEvent event) {
		recalculateFlightRegions();
	}

	@EventHandler
	public void onBoardDeleted(CheckersBoardDeletedEvent event) {
		recalculateFlightRegions();
	}

	@EventHandler
	public void onBoardModifed(CheckersBoardModifiedEvent event) {
		if (event.getChangedAttributes().contains("enclosure")) {
			recalculateFlightRegions();
		}
	}

	/**
	 * Cache the regions in which flight is allowed.  We do this to avoid calculation in the
	 * code which is (frequently) called from the PlayerMoveEvent handler.
	 */
	public void recalculateFlightRegions() {
		int above = plugin.getConfig().getInt("flying.upper_limit");
		int outside = plugin.getConfig().getInt("flying.outer_limit");

		flightRegions.clear();

		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			Cuboid c = bv.getBoard().getFullBoard();
			MaterialWithData mat = bv.getBoard().getBoardStyle().getEnclosureMaterial();
			if (BlockType.canPassThrough(mat.getId())) {
				c = c.expand(CuboidDirection.Up, Math.max(5, (c.getSizeY() * above) / 100));
				c = c.outset(CuboidDirection.Horizontal, Math.max(5, (c.getSizeX() * outside) / 100));
			}
			flightRegions.add(c);
		}
	}

	/**
	 * Check if the player may fly (in a Checkers context) given their current position.
	 * 
	 * @param player
	 * @return
	 */
	public Cuboid getFlightRegion(Location loc) {
		for (Cuboid c : flightRegions) {
			if (c.contains(loc))
				return c;
		}
		return null;
	}

	/**
	 * Mark the player as being allowed to fly or not.  If the player was previously allowed to fly by
	 * virtue of creative mode, he can continue to fly even if board flying is being disabled.
	 * 
	 * @param player
	 * @param flying
	 */
	private void setFlightAllowed(final Player player, boolean flying) {
		String playerName = player.getName();

		boolean currentlyAllowed = allowedToFly.containsKey(playerName);

		if (flying && currentlyAllowed || !flying && !currentlyAllowed)
			return;

		LogUtils.fine("set board flight allowed " + player.getName() + " = " + flying);

		player.setAllowFlight(flying || gameModeAllowsFlight(player));

		long now = System.currentTimeMillis();

		if (flying) {
			allowedToFly.put(playerName, new PreviousSpeed(player));
			player.setFlySpeed((float) plugin.getConfig().getDouble("flying.fly_speed"));
			player.setWalkSpeed((float) plugin.getConfig().getDouble("flying.walk_speed"));
			if (plugin.getConfig().getBoolean("flying.auto")) {
				final int blockId = player.getLocation().subtract(0, 2, 0).getBlock().getTypeId();
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run() {
						if (!BlockType.canPassThrough(blockId)) {
							// give player a kick upwards iff they're standing on something solid
							player.setVelocity(new Vector(0, 1.0, 0));
						}
						player.setFlying(true);
					}
				});
			}
			long last = lastMessagedIn.containsKey(playerName) ? lastMessagedIn.get(playerName) : 0;
			if (now - last > MESSAGE_COOLDOWN  && player.getGameMode() != GameMode.CREATIVE) {
				MiscUtil.alertMessage(player, Messages.getString("Flight.flightEnabled"));
				lastMessagedIn.put(playerName, System.currentTimeMillis());
			}
		} else {
			allowedToFly.get(playerName).restoreSpeeds();
			allowedToFly.remove(playerName);
			long last = lastMessagedOut.containsKey(playerName) ? lastMessagedOut.get(playerName) : 0;
			if (now - last > MESSAGE_COOLDOWN && player.getGameMode() != GameMode.CREATIVE) {
				MiscUtil.alertMessage(player, Messages.getString("Flight.flightDisabled"));
				lastMessagedOut.put(playerName, System.currentTimeMillis());
			}
		}

		if (!player.getAllowFlight()) {
			// prevent fall damage so players don't fall to their death by flying too far from a board
			Location loc = player.getLocation();
			int dist = player.getWorld().getHighestBlockYAt(loc) - loc.getBlockY();
			if (dist < -1) {
				player.setFallDistance(dist);
			}
		}
	}

	private boolean gameModeAllowsFlight(Player player) {
		return player.getGameMode() == GameMode.CREATIVE;
	}

	/**
	 * Update current fly/walk speeds for all players currently enjoying board flight mode.  Called when
	 * the fly/walk speeds are changed in config.
	 */
	public void updateSpeeds() {
		for (String playerName : allowedToFly.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.setFlySpeed((float) plugin.getConfig().getDouble("flying.fly_speed"));
				player.setWalkSpeed((float) plugin.getConfig().getDouble("flying.walk_speed"));
			}
		}
	}

	/**
	 * Restore previous fly/walk speeds for all players who have a modified speed.  Called when the
	 * plugin is disabled.
	 */
	public void restoreSpeeds() {
		for (String playerName : allowedToFly.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				allowedToFly.get(playerName).restoreSpeeds();
			}
		}
	}

	private class PreviousSpeed {
		private final WeakReference<Player> player;
		private final float flySpeed;
		private final float walkSpeed;

		public PreviousSpeed(Player p) {
			player = new WeakReference<Player>(p);
			flySpeed = p.getFlySpeed();
			walkSpeed = p.getWalkSpeed();
			LogUtils.fine("player " + p.getName() + ": store previous speed: walk=" + walkSpeed + " fly=" + flySpeed);
		}

		public void restoreSpeeds() {
			Player p = player.get();
			if (p == null)
				return;
			p.setFlySpeed(flySpeed);
			p.setWalkSpeed(walkSpeed);
			LogUtils.fine("player " + p.getName() + " restore previous speed: walk=" + walkSpeed + " fly=" + flySpeed);
		}
	}
}
