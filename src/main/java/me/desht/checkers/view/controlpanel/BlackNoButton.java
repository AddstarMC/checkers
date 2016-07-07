package me.desht.checkers.view.controlpanel;

import me.desht.checkers.model.PlayerColour;

class BlackNoButton extends YesNoButton {

	public BlackNoButton(ControlPanel panel) {
		super(panel, 7, 0, PlayerColour.BLACK, false);
	}

}
