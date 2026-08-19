package com.grvig.sourcemovement.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SourceMovementTest {
	static final double DT = 1.0 / 64;
	static final double EPS = 1e-9;

	MoveConfig cfg = new MoveConfig();
	MoveState state = new MoveState();
	MoveInput input = new MoveInput();
	FlatGround ground = new FlatGround();

	SourceMovementTest() {
		state.onGround = true;
	}

	void step() {
		SourceMovement.step(state, input, cfg, ground, DT);
	}

	@Test
	void standingJumpPeaksWhereTheSdkOrderPutsIt() {
		input.jump = true;
		double apex = 0;
		int ticks = 0;
		do {
			step();
			ticks++;
			apex = Math.max(apex, ground.y);
		} while (!state.onGround);

		// Takeoff speed after the jump tick's two half gravities, then n ticks of rise.
		double v0 = cfg.jumpImpulse - cfg.gravity * DT;
		int n = (int) Math.floor(v0 / (cfg.gravity * DT)) + 1;
		double expected = DT * (n * v0 - cfg.gravity * DT * n * (n - 1) / 2.0);
		assertEquals(54.6538, expected, 1e-4);
		assertEquals(expected, apex, EPS);
		// The 2 unit ground probe catches the floor on tick 47, one tick before the hull reaches it.
		assertEquals(47, ticks);
	}

	@Test
	void duckedJumpSetsTheImpulseAndPeaksAt57() {
		input.jump = true;
		input.ducked = true;
		double apex = 0;
		do {
			step();
			apex = Math.max(apex, ground.y);
		} while (!state.onGround);
		assertEquals(57.0, apex, 0.01);
	}

	/** One tick of Friction followed by Accelerate along the velocity, done by hand. */
	double handGroundTick(double speed, double wishSpeed) {
		if (speed >= 0.1) {
			double control = Math.max(speed, cfg.stopSpeed);
			speed = Math.max(speed - control * cfg.friction * DT, 0);
		}
		double add = wishSpeed - speed;
		if (add > 0) {
			speed += Math.min(cfg.accelerate * DT * wishSpeed, add);
		}
		if (speed < 1) {
			speed = 0;
		}
		return speed;
	}

	@Test
	void groundAccelerationReachesRunSpeedOnSchedule() {
		input.forward = 1;
		double hand = 0;
		int ticks = 0;
		while (state.horizontalSpeed() < cfg.runSpeed - EPS) {
			step();
			hand = handGroundTick(hand, cfg.runSpeed);
			ticks++;
			assertEquals(hand, state.horizontalSpeed(), EPS);
		}
		assertEquals(35, ticks);
		for (int i = 0; i < 10; i++) {
			step();
		}
		assertEquals(cfg.runSpeed, state.horizontalSpeed(), EPS);
	}

	@Test
	void frictionStopsFromRunSpeedThroughTheStopSpeedRegime() {
		state.velZ = 250;
		double hand = 250;
		int ticks = 0;
		while (state.horizontalSpeed() > 0) {
			step();
			hand = handGroundTick(hand, 0);
			ticks++;
			assertEquals(hand, state.horizontalSpeed(), EPS);
		}
		// Above 80 u/s speed falls by 8.125% a tick, below it by a flat 6.5 u/s.
		assertEquals(26, ticks);
	}

	@Test
	void duckingOnTheGroundCapsSpeedAt85() {
		input.forward = 1;
		input.ducked = true;
		for (int i = 0; i < 200; i++) {
			step();
		}
		assertEquals(cfg.runSpeed * cfg.duckSpeedMultiplier, state.horizontalSpeed(), 1e-6);
	}

	@Test
	void airStrafeGainsTheCappedWishSpeedEachTick() {
		state.onGround = false;
		ground.y = 100000;
		state.velZ = 250;
		input.side = 1;
		for (int i = 0; i < 64; i++) {
			// Face along the velocity so the strafe key's wish direction is perpendicular to it.
			input.yaw = Math.toDegrees(Math.atan2(-state.velX, state.velZ));
			double before = state.horizontalSpeed();
			step();
			double expected = Math.sqrt(before * before + cfg.airMaxWishSpeed * cfg.airMaxWishSpeed);
			assertEquals(expected, state.horizontalSpeed(), 1e-6);
		}
		assertEquals(Math.sqrt(250 * 250 + 64 * 30 * 30), state.horizontalSpeed(), 1e-6);
	}

}
