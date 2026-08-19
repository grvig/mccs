package com.grvig.sourcemovement.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Timestamped view and key input, sliced into Source ticks so each one sees the input of its own
 * slice of real time, like a Source usercmd. Times are System.nanoTime values.
 */
public class InputTimeline {
	public static final int FORWARD = 0;
	public static final int BACK = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	public static final int JUMP = 4;
	private static final int KEYS = 5;

	private record KeyEvent(long time, int key, boolean down) {
	}

	private record YawSample(long time, double yaw) {
	}

	private final ArrayDeque<KeyEvent> keyEvents = new ArrayDeque<>();
	private final ArrayDeque<Long> scrolls = new ArrayDeque<>();
	private final List<YawSample> yaws = new ArrayList<>();
	private final boolean[] down = new boolean[KEYS];

	public void addKey(long time, int key, boolean isDown) {
		keyEvents.add(new KeyEvent(time, key, isDown));
	}

	public void addScroll(long time) {
		scrolls.add(time);
	}

	public void addYaw(long time, double yaw) {
		yaws.add(new YawSample(time, yaw));
	}

	/**
	 * Fills the keys, scroll jump and yaw of the Source tick whose slice of real time ends at end.
	 * A key counts if it was down at any point in the slice, and every scroll notch up to end lands
	 * in this tick, so notches older than the slice go to the first tick that consumes them.
	 */
	public void fill(MoveInput cmd, long end) {
		boolean[] held = down.clone();
		while (!keyEvents.isEmpty() && keyEvents.peek().time() <= end) {
			KeyEvent event = keyEvents.poll();
			down[event.key()] = event.down();
			if (event.down()) {
				held[event.key()] = true;
			}
		}
		cmd.forward = axis(held[FORWARD], held[BACK]);
		cmd.side = axis(held[RIGHT], held[LEFT]);
		cmd.jump = held[JUMP];

		cmd.scrollJump = false;
		while (!scrolls.isEmpty() && scrolls.peek() <= end) {
			scrolls.poll();
			cmd.scrollJump = true;
		}
		cmd.yaw = yawAt(end);
	}

	private static double axis(boolean positive, boolean negative) {
		double value = 0;
		if (positive) {
			value += 1;
		}
		if (negative) {
			value -= 1;
		}
		return value;
	}

	/** The view yaw at a moment, interpolated between the frames around it. */
	private double yawAt(long time) {
		while (yaws.size() >= 2 && yaws.get(1).time() <= time) {
			yaws.remove(0);
		}
		if (yaws.isEmpty()) {
			return 0;
		}
		YawSample first = yaws.get(0);
		if (yaws.size() == 1 || time <= first.time()) {
			return first.yaw();
		}
		YawSample second = yaws.get(1);
		double t = (double) (time - first.time()) / (second.time() - first.time());
		return first.yaw() + (second.yaw() - first.yaw()) * t;
	}
}
