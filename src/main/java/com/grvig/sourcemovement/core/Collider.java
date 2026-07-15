package com.grvig.sourcemovement.core;

/**
 * The world the player moves through. Distances are in units.
 */
public interface Collider {
	int BLOCKED_X = 1;
	int BLOCKED_Y = 2;
	int BLOCKED_Z = 4;

	/** Moves the player and returns the BLOCKED_ flags of the axes that did not move the full distance. */
	int move(double dx, double dy, double dz);

	/** Looks for standable ground up to maxDrop below the player and settles onto it if found. */
	boolean findGround(double maxDrop);

	/** The Source surface friction of the ground the player stands on. */
	double groundFriction();
}
