package me.desht.checkers.view.controlpanel;

import me.desht.checkers.model.PlayerColour;

class WhiteNoButton extends YesNoButton {

	public WhiteNoButton(ControlPanel panel) {
		super(panel, 1, 0, PlayerColour.WHITE, false);
	}

}
