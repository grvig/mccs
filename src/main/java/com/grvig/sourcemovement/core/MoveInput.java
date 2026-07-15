package com.grvig.sourcemovement.core;

/**
 * One Source tick's user command, plus the scales the game world applies to it.
 */
public class MoveInput {
	/** -1 to 1, positive is forward. */
	public double forward;
	/** -1 to 1, positive is right. */
	public double side;
	/** View yaw in degrees using Minecraft's convention: 0 faces +Z, 90 faces -X. */
	public double yaw;
	public boolean jump;
	public boolean ducked;

	public double speedScale = 1;
	public double inputScale = 1;
	public double duckMultiplier = 0.34;
	public double jumpScale = 1;
	public double gravityScale = 1;
	/** Furthest drop in units that walking will stick to the ground across. */
	public double stepHeight = 18;
}
