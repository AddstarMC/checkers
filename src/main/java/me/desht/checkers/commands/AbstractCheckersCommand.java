package me.desht.checkers.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.DirectoryStructure;
import me.desht.checkers.ai.AIFactory.AIDefinition;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.view.BoardStyle;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

abstract class AbstractCheckersCommand extends AbstractCommand {

	AbstractCheckersCommand(String label) {
		super(label);
	}
	AbstractCheckersCommand(String label, int minArgs) {
		super(label, minArgs);
	}
	AbstractCheckersCommand(String label, int minArgs, int maxArgs) {
		super(label, minArgs, maxArgs);
	}

	List<String> getGameCompletions(CommandSender sender, String prefix) {
		List<String> res = new ArrayList<>();

		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix)) {
				res.add(game.getName());
			}
		}
		return getResult(res, sender, true);
	}

	List<String> getPlayerInGameCompletions(Player player, String prefix) {
		List<String> res = new ArrayList<>();

		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix) && game.hasPlayer(player)) {
				res.add(game.getName());
			}
		}
		return getResult(res, player, true);
	}

	List<String> getPlayerCompletions(Plugin plugin, CommandSender sender, String prefix, boolean aiOnly) {
		List<String> res = new ArrayList<>();
		if (!aiOnly) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				res.add(p.getName());
			}
		}
		for (AIDefinition aiDef : ((CheckersPlugin) plugin).getAIFactory().listAIDefinitions()) {
			if (!aiDef.isEnabled())
				continue;
			res.add(aiDef.getName());
		}
		return filterPrefix(sender, res, prefix);
	}

	List<String> getBoardCompletions(CommandSender sender, String prefix) {
		List<String> res = new ArrayList<>();

		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			if (bv.getName().startsWith(prefix)) {
				res.add(bv.getName());
			}
		}
		return getResult(res, sender, true);
	}

	List<String> getBoardStyleCompletions(CommandSender sender, String prefix) {
		List<String> styleNames = new ArrayList<>();
		for (BoardStyle style : getAllBoardStyles()) {
			styleNames.add(style.getName());
		}
		return filterPrefix(sender, styleNames, prefix);
	}

	List<BoardStyle> getAllBoardStyles() {
		Map<String, BoardStyle> res = new HashMap<>();

		File dir = DirectoryStructure.getBoardStyleDirectory();
		File customDir = new File(dir, "custom");

		for (File f : customDir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			res.put(styleName, BoardStyle.loadStyle(styleName));
		}
		for (File f : dir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			if (res.containsKey(styleName)) continue;
			res.put(styleName, BoardStyle.loadStyle(styleName));
		}
		return MiscUtil.asSortedList(res.values());
	}
}
