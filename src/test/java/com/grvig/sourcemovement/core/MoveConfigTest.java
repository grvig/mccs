package com.grvig.sourcemovement.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoveConfigTest {
	@Test
	void defaultsAreValid() {
		assertTrue(new MoveConfig().validate().isEmpty());
	}

	@Test
	void badValuesFallBackToDefaults() {
		MoveConfig cfg = new MoveConfig();
		cfg.scrollJump = "sideways";
		cfg.runSpeed = -5;
		cfg.friction = Double.NaN;
		assertEquals(3, cfg.validate().size());
		assertEquals("both", cfg.scrollJump);
		assertEquals(250, cfg.runSpeed);
		assertEquals(5.2, cfg.friction);
	}

	@Test
	void aMissingPresetIsRestored() {
		MoveConfig cfg = new MoveConfig();
		cfg.preset = null;
		assertFalse(cfg.validate().isEmpty());
		assertEquals("csgo_bhop", cfg.preset);
	}

	@Test
	void duckLiftCannotExceedTheHullDifference() {
		MoveConfig cfg = new MoveConfig();
		cfg.duckJumpLift = 13;
		assertFalse(cfg.validate().isEmpty());
		assertEquals(9, cfg.duckJumpLift);
	}

	@Test
	void anExplicitCapOverridesThePreset() {
		MoveConfig cfg = new MoveConfig();
		cfg.preset = "csgo";
		assertTrue(cfg.bhopCap());
		cfg.bhopCapEnabled = false;
		assertFalse(cfg.bhopCap());
	}
}
