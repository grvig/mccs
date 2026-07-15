package com.grvig.sourcemovement.core;

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

	/** Converts units per second to Minecraft blocks per tick. */
	public double toBlocksPerTick(double unitsPerSecond) {
		return unitsPerSecond / (unitsPerBlock * 20);
	}

	/** Converts Minecraft blocks per tick to units per second. */
	public double toUnitsPerSecond(double blocksPerTick) {
		return blocksPerTick * unitsPerBlock * 20;
	}
}
