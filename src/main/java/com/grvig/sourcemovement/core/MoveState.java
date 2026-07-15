package com.grvig.sourcemovement.core;

/**
 * Position independent player state carried between Source ticks. Y is up, velocity is in units per second.
 */
public class MoveState {
	public double velX;
	public double velY;
	public double velZ;
	public boolean onGround;
	public boolean oldJump;
	public double surfaceFriction = 1;

	public double horizontalSpeed() {
		return Math.sqrt(velX * velX + velZ * velZ);
	}
}
