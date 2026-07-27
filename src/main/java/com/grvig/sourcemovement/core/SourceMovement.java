package com.grvig.sourcemovement.core;

/**
 * CGameMovement::FullWalkMove from the Source SDK, one Source tick per call.
 */
public final class SourceMovement {
	/** Upward speed above which the player is never on the ground. */
	public static final double NON_JUMP_VELOCITY = 140;
	/** cl_forwardspeed and cl_sidespeed: what a fully held key asks for before clamping. */
	public static final double KEY_SPEED = 450;
	/** How far below the feet CategorizePosition looks for ground. */
	public static final double GROUND_PROBE = 2;

	private SourceMovement() {
	}

	public static void step(MoveState s, MoveInput in, MoveConfig cfg, Collider collider, double dt) {
		double gravity = cfg.gravity * in.gravityScale;
		s.velY -= gravity * 0.5 * dt;

		if (in.jump) {
			checkJump(s, in, cfg, gravity, dt);
		}
		s.oldJump = in.jump;

		if (s.onGround) {
			s.velY = 0;
			friction(s, cfg, dt);
		}
		clampVelocity(s, cfg);

		boolean walking = s.onGround;
		accelerateAndMove(s, in, cfg, collider, dt, walking);
		categorize(s, in, walking, collider);

		clampVelocity(s, cfg);
		s.velY -= gravity * 0.5 * dt;
		if (s.onGround) {
			s.velY = 0;
		}
	}

	/** The player's max speed: run speed scaled by speed effects, capped at sv_maxspeed. */
	public static double maxSpeed(MoveInput in, MoveConfig cfg) {
		return Math.min(cfg.runSpeed * in.speedScale, cfg.maxSpeed);
	}

	private static void checkJump(MoveState s, MoveInput in, MoveConfig cfg, double gravity, double dt) {
		if (!s.onGround) {
			return;
		}
		if (s.oldJump && !cfg.autohop) {
			return;
		}
		if (cfg.bhopCap()) {
			preventBunnyJumping(s, in, cfg);
		}
		s.onGround = false;
		double impulse = cfg.jumpImpulse * in.jumpScale;
		if (in.ducked) {
			s.velY = impulse;
		} else {
			s.velY += impulse;
		}
		s.velY -= gravity * 0.5 * dt;
	}

	private static void preventBunnyJumping(MoveState s, MoveInput in, MoveConfig cfg) {
		double cap = cfg.bhopCapFactor * maxSpeed(in, cfg);
		double speed = Math.sqrt(s.velX * s.velX + s.velY * s.velY + s.velZ * s.velZ);
		if (cap <= 0 || speed <= cap) {
			return;
		}
		double fraction = cap / speed;
		s.velX *= fraction;
		s.velY *= fraction;
		s.velZ *= fraction;
	}

	private static void friction(MoveState s, MoveConfig cfg, double dt) {
		double speed = Math.sqrt(s.velX * s.velX + s.velY * s.velY + s.velZ * s.velZ);
		if (speed < 0.1) {
			return;
		}
		double control = Math.max(speed, cfg.stopSpeed);
		double drop = control * cfg.friction * s.surfaceFriction * dt;
		double newSpeed = Math.max(speed - drop, 0) / speed;
		s.velX *= newSpeed;
		s.velY *= newSpeed;
		s.velZ *= newSpeed;
	}

	/**
	 * WalkMove and AirMove: builds the wish velocity from the keys and yaw, accelerates towards it, then moves.
	 */
	private static void accelerateAndMove(MoveState s, MoveInput in, MoveConfig cfg, Collider collider, double dt, boolean walking) {
		double maxSpeed = maxSpeed(in, cfg);
		double forwardMove = in.forward * KEY_SPEED;
		double sideMove = in.side * KEY_SPEED;
		double inputSpeed = Math.sqrt(forwardMove * forwardMove + sideMove * sideMove);
		if (inputSpeed > maxSpeed) {
			forwardMove *= maxSpeed / inputSpeed;
			sideMove *= maxSpeed / inputSpeed;
		}
		forwardMove *= in.inputScale;
		sideMove *= in.inputScale;
		if (walking && in.ducked) {
			forwardMove *= in.duckMultiplier;
			sideMove *= in.duckMultiplier;
		}

		double yaw = Math.toRadians(in.yaw);
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		double wishX = forwardX * forwardMove - forwardZ * sideMove;
		double wishZ = forwardZ * forwardMove + forwardX * sideMove;
		double wishSpeed = Math.sqrt(wishX * wishX + wishZ * wishZ);
		if (wishSpeed > 0) {
			wishX /= wishSpeed;
			wishZ /= wishSpeed;
		}
		wishSpeed = Math.min(wishSpeed, maxSpeed);

		if (walking) {
			s.velY = 0;
			accelerate(s, wishX, wishZ, wishSpeed, wishSpeed, cfg.accelerate, dt);
			s.velY = 0;
			if (s.horizontalSpeed() < 1) {
				s.velX = 0;
				s.velZ = 0;
				return;
			}
		} else {
			double airWishSpeed = Math.min(wishSpeed, cfg.airMaxWishSpeed);
			accelerate(s, wishX, wishZ, wishSpeed, airWishSpeed, cfg.airAccelerate, dt);
		}

		int blocked = collider.move(s.velX * dt, s.velY * dt, s.velZ * dt);
		if ((blocked & Collider.BLOCKED_X) != 0) {
			s.velX = 0;
		}
		if ((blocked & Collider.BLOCKED_Y) != 0) {
			s.velY = 0;
		}
		if ((blocked & Collider.BLOCKED_Z) != 0) {
			s.velZ = 0;
		}
	}

	/**
	 * Accelerate and AirAccelerate. The speed added along the wish direction is capped by addCap,
	 * while the acceleration itself scales with the uncapped wish speed.
	 */
	private static void accelerate(MoveState s, double wishX, double wishZ, double wishSpeed, double addCap, double accel, double dt) {
		double currentSpeed = s.velX * wishX + s.velZ * wishZ;
		double addSpeed = addCap - currentSpeed;
		if (addSpeed <= 0) {
			return;
		}
		double accelSpeed = Math.min(accel * dt * wishSpeed * s.surfaceFriction, addSpeed);
		s.velX += accelSpeed * wishX;
		s.velZ += accelSpeed * wishZ;
	}

	/**
	 * CategorizePosition, plus StayOnGround after walking so steps down keep the player grounded.
	 */
	private static void categorize(MoveState s, MoveInput in, boolean walking, Collider collider) {
		s.surfaceFriction = 1;
		if (s.velY > NON_JUMP_VELOCITY) {
			s.onGround = false;
			return;
		}
		double probe = GROUND_PROBE;
		if (walking) {
			probe = Math.max(in.stepHeight, GROUND_PROBE);
		}
		s.onGround = collider.findGround(probe);
		if (s.onGround) {
			s.surfaceFriction = collider.groundFriction();
		} else if (s.velY > 0) {
			s.surfaceFriction = 0.25;
		}
	}

	private static void clampVelocity(MoveState s, MoveConfig cfg) {
		s.velX = Math.clamp(s.velX, -cfg.maxVelocity, cfg.maxVelocity);
		s.velY = Math.clamp(s.velY, -cfg.maxVelocity, cfg.maxVelocity);
		s.velZ = Math.clamp(s.velZ, -cfg.maxVelocity, cfg.maxVelocity);
	}
}
