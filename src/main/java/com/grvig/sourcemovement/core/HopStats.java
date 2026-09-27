package com.grvig.sourcemovement.core;

/**
 * Numbers for practising strafes, like a KZ server's jump stats: the speed a jump left the ground
 * with, how much it gained by landing, and how well the mouse turn matched the strafe keys.
 */
public class HopStats {
	/** Horizontal speed at the last takeoff. */
	public double takeoffSpeed;
	/** Landing speed minus takeoff speed of the last finished jump. */
	public double gain;
	/** Share of turning air ticks where the strafe key matched the turn, for the last finished jump. */
	public double sync;

	private boolean airborne;
	private int turningTicks;
	private int syncedTicks;
	private double lastYaw;
	private boolean hasYaw;

	/** Feed one Source tick: ground state and speed after the move, and the tick's yaw and strafe input. */
	public void update(boolean onGround, double speed, double yaw, double side) {
		if (!onGround && !airborne) {
			airborne = true;
			takeoffSpeed = speed;
			turningTicks = 0;
			syncedTicks = 0;
		} else if (onGround && airborne) {
			airborne = false;
			gain = speed - takeoffSpeed;
			sync = 0;
			if (turningTicks > 0) {
				sync = (double) syncedTicks / turningTicks;
			}
		}

		if (airborne && hasYaw) {
			double turn = yaw - lastYaw;
			if (turn != 0) {
				turningTicks++;
				// Yaw grows when turning right, and positive side input is the right strafe key.
				if (turn * side > 0) {
					syncedTicks++;
				}
			}
		}
		lastYaw = yaw;
		hasYaw = true;
	}

	/** Forgets the last yaw, for when ticks were skipped and the next turn would be meaningless. */
	public void reset() {
		airborne = false;
		hasYaw = false;
	}
}
