package me.desht.checkers.view.controlpanel;

abstract class CounterLabel extends AbstractSignLabel {

	private int count;

	CounterLabel(ControlPanel panel, String labelKey, int x, int y) {
		super(panel, labelKey, x, y);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public String[] getCustomSignText() {
		String[] res = getSignText();
		res[2] = getIndicatorColour() + count;
		return res;
	}
}
