package me.desht.checkers.commands;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.util.EconomyUtil;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class StakeCommand extends AbstractCheckersCommand {
	public StakeCommand() {
		super("checkers stake", 1, 1);
		setPermissionNode("checkers.commands.stake");
		setUsage("/<command> stake <amount>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (!EconomyUtil.enabled()) {
            return true;
        }
        notFromConsole(sender);

		Player player = (Player) sender;
		String stakeStr = args[0];
		try {
			CheckersGame game = CheckersGameManager.getManager().getCurrentGame((Player) sender, true);
			double amount = Double.parseDouble(stakeStr);
			game.setStake(player, amount);
			MiscUtil.statusMessage(sender, Messages.getString("Game.stakeChanged", EconomyUtil.formatStakeStr(amount)));
		} catch (NumberFormatException e) {
			throw new CheckersException(Messages.getString("Misc.invalidNumeric", stakeStr));
		}
		return true;
	}

}
