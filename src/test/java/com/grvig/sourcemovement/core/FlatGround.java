package com.grvig.sourcemovement.core;

/**
 * An endless flat floor at y = 0 for tests. Tracks the player's position in units.
 */
class FlatGround implements Collider {
	double x;
	double y;
	double z;

	@Override
	public int move(double dx, double dy, double dz) {
		x += dx;
		y += dy;
		z += dz;
		if (y < 0) {
			y = 0;
			return BLOCKED_Y;
		}
		return 0;
	}

	@Override
	public boolean findGround(double maxDrop) {
		if (y > maxDrop) {
			return false;
		}
		y = 0;
		return true;
	}

	@Override
	public double groundFriction() {
		return 1;
	}
}
