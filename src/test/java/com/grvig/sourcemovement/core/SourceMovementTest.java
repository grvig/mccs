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
}
