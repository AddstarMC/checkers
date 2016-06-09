package me.desht.checkers.view.controlpanel;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.checkers.player.HumanCheckersPlayer;
import me.desht.checkers.responses.DrawResponse;
import me.desht.checkers.responses.SwapResponse;
import me.desht.checkers.responses.UndoResponse;
import me.desht.checkers.responses.YesNoResponse;
import me.desht.dhutils.responsehandler.ResponseHandler;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class YesNoButton extends AbstractSignButton {

	private final PlayerColour colour;
	private final boolean yesOrNo;

	public YesNoButton(ControlPanel panel, int x, int y, PlayerColour colour, boolean yesOrNo) {
		super(panel, yesOrNo ? "yesBtn" : "noBtn", null, x, y);
		this.colour = colour;
		this.yesOrNo = yesOrNo;
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		YesNoResponse.handleYesNoResponse(event.getPlayer(), yesOrNo);
	}

	@Override
	public boolean isEnabled() {
		return !getOfferText().isEmpty();
	}

	@Override
	public String[] getCustomSignText() {
		String[] text = getSignText();

		text[0] = getOfferText();

		return text;
	}

	private String getOfferText() {
		CheckersGame game = getGame();
		if (game == null) return "";

		CheckersPlayer cp = game.getPlayer(colour);
		if (cp == null || !cp.isHuman())
			return "";

		Player player = ((HumanCheckersPlayer) cp).getBukkitPlayer();

		ResponseHandler rh = CheckersPlugin.getInstance().getResponseHandler();

		if (player == null) {
			// gone offline, perhaps?
			return "";
		} else if (rh.isExpecting(player, DrawResponse.class)) {
			return Messages.getString("ControlPanel.acceptDrawBtn");
		} else if (rh.isExpecting(player, SwapResponse.class)) {
			return Messages.getString("ControlPanel.acceptSwapBtn");
		} else if (rh.isExpecting(player, UndoResponse.class)) {
			return Messages.getString("ControlPanel.acceptUndoBtn");
		} else {
			return "";
		}
	}
}
