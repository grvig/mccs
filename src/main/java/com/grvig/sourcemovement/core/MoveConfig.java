package com.grvig.sourcemovement.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Movement settings, named after the CS:GO convars they mirror. Speeds are in units per second.
 */
public class MoveConfig {
	public String preset = "csgo_bhop";
	public double unitsPerBlock = 40;

	public double accelerate = 5.5;
	public double airAccelerate = 12;
	public double friction = 5.2;
	public double stopSpeed = 80;
	public double maxSpeed = 320;
	public double gravity = 800;
	public double jumpImpulse = 301.993377;
	public double maxVelocity = 3500;
	public double airMaxWishSpeed = 30;

	public double runSpeed = 250;
	public double duckSpeedMultiplier = 0.34;
	public double duckJumpLift = 9;

	public double bhopCapFactor = 1.1;
	public Boolean bhopCapEnabled;
	public boolean autohop = false;
	public String scrollJump = "both";
	/** Off by default: servers do not expect Source speeds and may kick or rubber band the player. */
	public boolean allowMultiplayer = false;

	/**
	 * Whether jumps are capped to bhopCapFactor times the max speed. An explicit
	 * bhopCapEnabled value wins over the preset.
	 */
	public boolean bhopCap() {
		if (bhopCapEnabled != null) {
			return bhopCapEnabled;
		}
		return preset.equals("csgo");
	}

	/**
	 * Puts any missing or out of range value back to its default and returns a line describing each
	 * fix, so a typo in the file never reaches the movement code.
	 */
	public List<String> validate() {
		MoveConfig defaults = new MoveConfig();
		List<String> problems = new ArrayList<>();
		if (preset == null || !List.of("csgo", "csgo_bhop").contains(preset)) {
			problems.add("preset must be csgo or csgo_bhop, was " + preset);
			preset = defaults.preset;
		}
		if (scrollJump == null || !List.of("off", "up", "down", "both").contains(scrollJump)) {
			problems.add("scrollJump must be off, up, down or both, was " + scrollJump);
			scrollJump = defaults.scrollJump;
		}
		unitsPerBlock = inRange(problems, "unitsPerBlock", unitsPerBlock, 1, 1000, defaults.unitsPerBlock);
		accelerate = inRange(problems, "accelerate", accelerate, 0, 1000, defaults.accelerate);
		airAccelerate = inRange(problems, "airAccelerate", airAccelerate, 0, 1000, defaults.airAccelerate);
		friction = inRange(problems, "friction", friction, 0, 1000, defaults.friction);
		stopSpeed = inRange(problems, "stopSpeed", stopSpeed, 0, 10000, defaults.stopSpeed);
		maxSpeed = inRange(problems, "maxSpeed", maxSpeed, 1, 10000, defaults.maxSpeed);
		gravity = inRange(problems, "gravity", gravity, 0, 10000, defaults.gravity);
		jumpImpulse = inRange(problems, "jumpImpulse", jumpImpulse, 0, 10000, defaults.jumpImpulse);
		maxVelocity = inRange(problems, "maxVelocity", maxVelocity, 1, 100000, defaults.maxVelocity);
		airMaxWishSpeed = inRange(problems, "airMaxWishSpeed", airMaxWishSpeed, 0, 10000, defaults.airMaxWishSpeed);
		runSpeed = inRange(problems, "runSpeed", runSpeed, 1, 10000, defaults.runSpeed);
		duckSpeedMultiplier = inRange(problems, "duckSpeedMultiplier", duckSpeedMultiplier, 0, 1, defaults.duckSpeedMultiplier);
		// The sneaking hull is 0.3 blocks shorter, so a bigger lift would push the head up through blocks.
		duckJumpLift = inRange(problems, "duckJumpLift", duckJumpLift, 0, 0.3 * unitsPerBlock, defaults.duckJumpLift);
		bhopCapFactor = inRange(problems, "bhopCapFactor", bhopCapFactor, 0, 100, defaults.bhopCapFactor);
		return problems;
	}

	private static double inRange(List<String> problems, String name, double value, double min, double max, double fallback) {
		if (value >= min && value <= max) {
			return value;
		}
		problems.add(name + " must be between " + min + " and " + max + ", was " + value);
		return fallback;
	}

	/** Converts units per second to Minecraft blocks per tick. */
	public double toBlocksPerTick(double unitsPerSecond) {
		return unitsPerSecond / (unitsPerBlock * 20);
	}

	/** Converts Minecraft blocks per tick to units per second. */
	public double toUnitsPerSecond(double blocksPerTick) {
		return blocksPerTick * unitsPerBlock * 20;
	}
}
