package com.grvig.sourcemovement.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HopStatsTest {
	HopStats stats = new HopStats();

	/** One jump turning right a degree every air tick, with the given strafe input on each tick. */
	void jump(double takeoff, double landing, double... sides) {
		stats.update(true, takeoff, 0, 0);
		double yaw = 0;
		for (double side : sides) {
			yaw += 1;
			stats.update(false, takeoff, yaw, side);
		}
		stats.update(true, landing, yaw, 0);
	}

	@Test
	void recordsTakeoffAndGain() {
		jump(250, 290, 1, 1, 1);
		assertEquals(250, stats.takeoffSpeed);
		assertEquals(40, stats.gain);
	}

	@Test
	void matchingKeysAndTurnIsFullSync() {
		jump(250, 260, 1, 1, 1, 1);
		assertEquals(1, stats.sync);
	}

	@Test
	void strafingAgainstTheTurnCountsAgainstSync() {
		jump(250, 260, 1, -1, 1, 0);
		assertEquals(0.5, stats.sync);
	}

	@Test
	void eachJumpStartsFresh() {
		jump(250, 260, -1, -1);
		jump(260, 280, 1, 1);
		assertEquals(260, stats.takeoffSpeed);
		assertEquals(20, stats.gain);
		assertEquals(1, stats.sync);
	}
}
