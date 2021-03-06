package me.desht.checkers.commands;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TeleportCommand extends AbstractCheckersCommand {

	public TeleportCommand() {
		super("checkers tp", 0, 2);
		addAlias("checkers teleport");
		setPermissionNode("checkers.commands.teleport");
		setUsage(new String[] {
				"/<command> tp [<game-name>]",
				"/<command> tp -b <board-name>",
				"/<command> tp -set [<board-name>]",
				"/<command> tp -clear [<board-name>]",
				"/<command> tp -list"
		});
		setOptions("b", "set", "clear", "list");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (getBooleanOption("list")) {
			showTeleportDests(sender);
			return true;
		}
		notFromConsole(sender);

		if (!plugin.getConfig().getBoolean("teleporting")) {
			throw new CheckersException(Messages.getString("Misc.noTeleporting"));
		}

		Player player = (Player)sender;

		if (getBooleanOption("set")) {
			PermissionUtils.requirePerms(sender, "checkers.commands.teleport.set");
			if (args.length == 0) {
				// set global teleport-out location
				BoardViewManager.getManager().setGlobalTeleportOutDest(player.getLocation());
				MiscUtil.statusMessage(player, Messages.getString("Misc.globalTeleportSet"));
			} else {
				// set per-board teleport-out location
				BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
				bv.setTeleportOutDestination(player.getLocation());
				MiscUtil.statusMessage(player, Messages.getString("Misc.boardTeleportSet", bv.getName()));
			}
		} else if (getBooleanOption("clear")) {
			PermissionUtils.requirePerms(sender, "checkers.commands.teleport.set");
			if (args.length == 0) {
				// clear global teleport-out location
				BoardViewManager.getManager().setGlobalTeleportOutDest(null);
				MiscUtil.statusMessage(player, Messages.getString("Misc.globalTeleportCleared"));
			} else {
				// clear per-board teleport-out location
				BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
				bv.setTeleportOutDestination(null);
				MiscUtil.statusMessage(player, Messages.getString("Misc.boardTeleportCleared", bv.getName()));
			}
		} else if (getBooleanOption("b") && args.length > 0) {
			// teleport to board
			PermissionUtils.requirePerms(sender, "checkers.commands.teleport.board");
			BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
			((CheckersPlugin) plugin).getPlayerTracker().teleportPlayer(player, bv.getTeleportInDestination());
		} else if (args.length == 0) {
			// teleport out of (or back to) current game
			BoardViewManager.getManager().teleportOut(player);
		} else {
			// teleport to game, or maybe board
			BoardView bv;
			if (CheckersGameManager.getManager().checkGame(args[0])) {
				CheckersGame game = CheckersGameManager.getManager().getGame(args[0]);
				bv = BoardViewManager.getManager().findBoardForGame(game);
			} else {
				PermissionUtils.requirePerms(player, "checkers.commands.teleport.board");
				bv = BoardViewManager.getManager().getBoardView(args[0]);
			}
			((CheckersPlugin) plugin).getPlayerTracker().teleportPlayer(player, bv.getTeleportInDestination());
		}

		return true;
	}

	private void showTeleportDests(CommandSender sender) {
		String bullet = MessagePager.BULLET + ChatColor.DARK_PURPLE;
		MessagePager pager = MessagePager.getPager(sender).clear();
		Location loc = BoardViewManager.getManager().getGlobalTeleportOutDest();
		if (loc != null) {
			pager.add(bullet + ChatColor.YELLOW + "[GLOBAL]" + ChatColor.WHITE + ": " + MiscUtil.formatLocation(loc));
		}
		for (BoardView bv : BoardViewManager.getManager().listBoardViewsSorted()) {
			if (bv.hasTeleportOutDestination()) {
				loc = bv.getTeleportOutDestination();
				pager.add(bullet + ChatColor.YELLOW + bv.getName() + ChatColor.WHITE + ": " + MiscUtil.formatLocation(loc));
			}
		}
		pager.showPage();
	}

}
