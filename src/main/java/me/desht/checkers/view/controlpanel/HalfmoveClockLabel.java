package me.desht.checkers.view.controlpanel;

public class HalfmoveClockLabel extends CounterLabel {

	public HalfmoveClockLabel(ControlPanel panel) {
		super(panel, "halfmoveClock", 2, 0);
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

}
