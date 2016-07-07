package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ResignCommand extends AbstractCheckersCommand {

	public ResignCommand() {
		super("checkers resign", 0, 1);
		setPermissionNode("checkers.commands.resign");
		setUsage("/<command> resign [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		Player player = (Player) sender;
		CheckersGame game;
		if (args.length >= 1) {
			game = CheckersGameManager.getManager().getGame(args[0]);
		} else {
			game = CheckersGameManager.getManager().getCurrentGame(player, true);
		}

		if (game != null) {
			game.resign(player.getUniqueId().toString());
		}

		return true;
	}

	@NotNull
	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1 && sender instanceof Player) {
			return getPlayerInGameCompletions((Player) sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

}
