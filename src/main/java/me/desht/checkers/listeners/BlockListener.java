package me.desht.checkers.listeners;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener extends CheckersBaseListener {

	public BlockListener(CheckersPlugin plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent event) {
		if (!plugin.getConfig().getBoolean("protection.no_building", true)) {
			return;
		}
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0);
		if (bv == null) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!plugin.getConfig().getBoolean("protection.no_building", true)) {
			return;
		}
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0);
		if (bv == null) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!plugin.getConfig().getBoolean("protection.no_building", true)) {
			return;
		}
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0);
		if (bv == null) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (!plugin.getConfig().getBoolean("protection.no_burning", true)) {
			return;
		}
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 1) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (!plugin.getConfig().getBoolean("protection.no_burning", true)) {
			return;
		}
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Cancelling liquid flow events makes it possible to use water & lava for walls & chess pieces.
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		} else if (BoardViewManager.getManager().partOfChessBoard(event.getToBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Snow doesn't usually form on boards due to the high light level.  But if the light level
	 * is dimmed, we might see boards getting covered.
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}
}
