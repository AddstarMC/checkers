package me.desht.checkers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import me.desht.checkers.util.CheckersUtils;
import me.desht.dhutils.LogUtils;

import com.google.common.base.Joiner;

public class TimeControl implements ConfigurationSerializable {
	public enum ControlType { NONE, ROLLOVER, MOVE_IN, GAME_IN };

	private final String spec;
	private final ControlType controlType;
	private final long totalTime;			// milliseconds
	private long remainingTime;		// milliseconds
	private long elapsed;				// milliseconds
	private int rolloverPhase;
	private int rolloverMovesMade;
	private final List<RolloverPhase> rollovers = new ArrayList<TimeControl.RolloverPhase>();
	private long lastChecked = System.currentTimeMillis();
	private boolean active = false;
	private boolean newPhase;

	public TimeControl() {
		this(0L);
	}

	public TimeControl(String specStr) {
		spec = specStr.toUpperCase();
		if (spec.isEmpty() || spec.startsWith("N")) {
			totalTime = 0L;
			controlType = ControlType.NONE;
		} else if (spec.startsWith("G/")) {
			// game in - minutes
			int t = Integer.parseInt(spec.substring(2));
			remainingTime = totalTime = t * 60000;
			controlType = ControlType.GAME_IN;
		} else if (spec.startsWith("M/")) {
			// move in - seconds
			int t = Integer.parseInt(spec.substring(2));
			remainingTime = totalTime = t * 1000;
			controlType = ControlType.MOVE_IN;
		} else if (!spec.isEmpty() && Character.isDigit(spec.charAt(0))) {
			totalTime = 0L;
			for (String s0 : spec.split(";")) {
				rollovers.add(new RolloverPhase(s0));
			}
			rolloverPhase = rolloverMovesMade = 0;
			remainingTime = rollovers.get(0).getMinutes() * 60000;
			controlType = ControlType.ROLLOVER;
		} else {
			throw new CheckersException("Invalid time control specification: " + spec);
		}
	}

	public TimeControl(long elapsed) {
		controlType = ControlType.NONE;
		this.elapsed = elapsed;
		this.spec = "";
		this.remainingTime = this.totalTime = 0L;
		this.rolloverMovesMade = this.rolloverPhase = 0;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("spec", spec);
		res.put("elapsed", elapsed);
		res.put("remainingTime", remainingTime);
		res.put("rolloverPhase", rolloverPhase);
		res.put("rolloverMovesMade", rolloverMovesMade);
		return res;
	}

	public static TimeControl deserialize(Map<String, Object> map) {
		TimeControl tc = new TimeControl((String) map.get("spec"));
		tc.elapsed = Long.parseLong(map.get("elapsed").toString());
		tc.remainingTime = Long.parseLong(map.get("remainingTime").toString());
		tc.rolloverMovesMade = (Integer) map.get("rolloverMovesMade");
		tc.rolloverPhase = (Integer) map.get("rolloverPhase");
		return tc;
	}

	public ControlType getControlType() {
		return controlType;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public long getElapsed() {
		return elapsed;
	}

	public String getSpec() {
		return spec;
	}

	public boolean isNewPhase() {
		return newPhase;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		lastChecked = System.currentTimeMillis();	// ensure the next tick() gets a good offset
		this.active = active;
	}

	public long getRemainingTime() {
		return controlType == ControlType.NONE ? Long.MAX_VALUE : remainingTime;
	}

	public String getClockString() {
		switch (getControlType()) {
		case NONE:
			return CheckersUtils.milliSecondsToHMS(getElapsed());
		default:
			return CheckersUtils.milliSecondsToHMS(getRemainingTime());
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO: i18n needed here
		switch (controlType) {
		case MOVE_IN:
			return "Move in " + (totalTime / 1000) + "s";
		case GAME_IN:
			return "Game in " + (totalTime / 60000) + "m";
		case ROLLOVER:
			List<String> l = new ArrayList<String>();
			for (int i = 0; i < rollovers.size(); i++) {
				if (i == rolloverPhase) {
					l.add("[ " + rollovers.get(i).toString() + " ]");
				} else {
					l.add(rollovers.get(i).toString());
				}
			}
			return Joiner.on(" => ").join(l);
		case NONE:
			return "None";
		default:
			return "???";	
		}
	}

	public String phaseString() {
		return rollovers.get(rolloverPhase).toString();
	}

	public String phaseString(int phase) {
		return rollovers.get(phase).toString();
	}

	/**
	 * Process a clock tick.
	 */
	public void tick() {
		long offset = System.currentTimeMillis() - lastChecked;
		lastChecked = System.currentTimeMillis();
		if (active) {
			elapsed += offset;
			if (controlType != ControlType.NONE) {
				remainingTime -= offset;	
			}
		}
	}

	public RolloverPhase getCurrentPhase() {
		return rollovers.get(rolloverPhase);
	}

	/**
	 * The player has made a move - adjust time control accordingly, and deactivate the clock.
	 */
	public void moveMade() {
		newPhase = false;
		switch (controlType) {
		case MOVE_IN:
			remainingTime = totalTime;
			break;
		case ROLLOVER:
			rolloverMovesMade++;
			LogUtils.fine("moves made = " + rolloverMovesMade + ", phase = " + rolloverPhase);
			LogUtils.fine("need " + rollovers.get(rolloverPhase).getMoves());
			if (rolloverMovesMade == rollovers.get(rolloverPhase).getMoves()) {
				rolloverMovesMade = 0;
				rolloverPhase = (rolloverPhase + 1) % rollovers.size();
				remainingTime += rollovers.get(rolloverPhase).getMinutes() * 60000;
				newPhase = true;
			}
			remainingTime += rollovers.get(rolloverPhase).getIncrement();
		default:
			break;
		}
		setActive(false);
	}

	public class RolloverPhase {
		private long increment;	// milliseconds
		private int moves;
		private int minutes;

		RolloverPhase(String spec) {
			String[] fields = spec.split("/");
			switch (fields.length) {
			case 3:
				this.increment = Long.parseLong(fields[2]) * 1000;
				// fall through
			case 2:
				this.moves = Integer.parseInt(fields[0]);
				this.minutes = Integer.parseInt(fields[1]);
				break;
			default:
				throw new IllegalArgumentException("invalid rollover specification: " + spec);
			}
		}

		public long getIncrement() {
			return increment;
		}

		public int getMoves() {
			return moves;
		}

		public int getMinutes() {
			return minutes;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(getMoves()).append("Mv / ").append(getMinutes()).append("m");
			if (getIncrement() > 0) {
				s.append(" + ").append(getIncrement() / 1000).append("s");
			}
			return s.toString();
		}
	}
}
