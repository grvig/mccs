package com.grvig.sourcemovement.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InputTimelineTest {
	static final long MS = 1_000_000L;
	static final long T0 = 1_000_000_000L;

	SubstepClock clock = new SubstepClock();
	InputTimeline timeline = new InputTimeline();

	@Test
	void fiveMinecraftTicksRunSixteenSourceTicks() {
		int total = 0;
		for (int i = 1; i <= 5; i++) {
			total += clock.advance(T0 + i * 50 * MS).length;
		}
		assertEquals(16, total);
	}

	@Test
	void sourceTicksSplitTheTickEvenly() {
		long[] ends = clock.advance(T0);
		long start = T0 - 50 * MS;
		assertArrayEquals(new long[] {start + 15_625_000L, start + 31_250_000L, start + 46_875_000L}, ends);
	}

	/** Which of the tick's Source ticks get a scroll jump. */
	boolean[] scrollJumps(long[] ends) {
		boolean[] jumps = new boolean[ends.length];
		MoveInput cmd = new MoveInput();
		for (int i = 0; i < ends.length; i++) {
			timeline.fill(cmd, ends[i]);
			jumps[i] = cmd.scrollJump;
		}
		return jumps;
	}

	@Test
	void aScrollNotchLandsInTheSourceTickContainingIt() {
		long start = T0 - 50 * MS;
		timeline.addScroll(start + 20 * MS);
		boolean[] jumps = scrollJumps(clock.advance(T0));
		assertArrayEquals(new boolean[] {false, true, false}, jumps);
	}

	@Test
	void aScrollBeforeTheFirstUnprocessedTickGoesToTheNextOne() {
		timeline.addScroll(T0 - 80 * MS);
		boolean[] jumps = scrollJumps(clock.advance(T0));
		assertArrayEquals(new boolean[] {true, false, false}, jumps);
	}

	@Test
	void severalNotchesInOneTickCountOnce() {
		long start = T0 - 50 * MS;
		timeline.addScroll(start + 17 * MS);
		timeline.addScroll(start + 20 * MS);
		timeline.addScroll(start + 25 * MS);
		boolean[] jumps = scrollJumps(clock.advance(T0));
		assertArrayEquals(new boolean[] {false, true, false}, jumps);
	}

	@Test
	void aTapShorterThanATickStillCounts() {
		long start = T0 - 50 * MS;
		timeline.addKey(start + 33 * MS, InputTimeline.JUMP, true);
		timeline.addKey(start + 36 * MS, InputTimeline.JUMP, false);
		MoveInput cmd = new MoveInput();
		long[] ends = clock.advance(T0);
		timeline.fill(cmd, ends[0]);
		assertEquals(false, cmd.jump);
		timeline.fill(cmd, ends[1]);
		assertEquals(false, cmd.jump);
		timeline.fill(cmd, ends[2]);
		assertEquals(true, cmd.jump);
	}

	@Test
	void yawIsInterpolatedBetweenFrames() {
		timeline.addYaw(T0, 0);
		timeline.addYaw(T0 + 10 * MS, 90);
		MoveInput cmd = new MoveInput();
		timeline.fill(cmd, T0 + 5 * MS);
		assertEquals(45, cmd.yaw, 1e-9);
		timeline.fill(cmd, T0 + 20 * MS);
		assertEquals(90, cmd.yaw, 1e-9);
	}

	@Test
	void opposingKeysCancel() {
		timeline.addKey(T0, InputTimeline.FORWARD, true);
		timeline.addKey(T0, InputTimeline.LEFT, true);
		timeline.addKey(T0, InputTimeline.RIGHT, true);
		MoveInput cmd = new MoveInput();
		timeline.fill(cmd, T0 + MS);
		assertEquals(1, cmd.forward);
		assertEquals(0, cmd.side);
	}
}
