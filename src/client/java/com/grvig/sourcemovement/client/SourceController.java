package com.grvig.sourcemovement.client;

import com.grvig.sourcemovement.client.mixin.EntityAccessor;
import com.grvig.sourcemovement.client.mixin.LivingEntityAccessor;
import com.grvig.sourcemovement.client.mixin.LocalPlayerAccessor;
import com.grvig.sourcemovement.core.InputTimeline;
import com.grvig.sourcemovement.core.MoveConfig;
import com.grvig.sourcemovement.core.MoveInput;
import com.grvig.sourcemovement.core.MoveState;
import com.grvig.sourcemovement.core.SourceMovement;
import com.grvig.sourcemovement.core.SubstepClock;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Runs Source movement for the local player in place of Minecraft's travel. Minecraft's velocity
 * stays the source of truth: it is read at the start of every tick and written back at the end,
 * so knockback, explosions and the switch to and from vanilla movement all carry over.
 */
public final class SourceController {
	private static final long GAME_TICK_NANOS = 50_000_000L;
	private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
	private static final double BASE_MOVEMENT_SPEED = 0.1;
	private static final double BASE_SNEAKING_SPEED = 0.3;
	private static final double BASE_JUMP_POWER = 0.42;
	private static final double BASE_GRAVITY = 0.08;

	public static MoveConfig config = new MoveConfig();
	public static boolean enabled = true;
	public static final InputTimeline TIMELINE = new InputTimeline();

	private static final MoveState STATE = new MoveState();
	private static final MoveInput INPUT = new MoveInput();
	private static SubstepClock clock = new SubstepClock();
	private static boolean simulatedLastTick;
	private static boolean simulatedThisTick;
	private static boolean crouchedLastTick;

	private SourceController() {
	}

	/** Whether Source movement drives the player right now, or vanilla does. */
	public static boolean shouldSimulate(LocalPlayer player) {
		if (!enabled || !player.isAlive() || player.isSpectator() || player.isSleeping() || player.isPassenger()) {
			return false;
		}
		if (player.getAbilities().flying || player.isFallFlying() || player.isAutoSpinAttack()) {
			return false;
		}
		if (player.isInWater() || player.isInLava() || player.isSwimming() || player.onClimbable()) {
			return false;
		}
		if (player.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(player)) {
			return false;
		}
		if (player.hasEffect(MobEffects.LEVITATION)) {
			return false;
		}
		return Minecraft.getInstance().getCameraEntity() == player;
	}

	public static void travel(LocalPlayer player) {
		long now = System.nanoTime();
		TIMELINE.addYaw(now, player.getYRot());
		if (!simulatedLastTick) {
			clock = new SubstepClock();
			TIMELINE.fill(INPUT, now - GAME_TICK_NANOS);
			STATE.oldJump = INPUT.jump;
			STATE.surfaceFriction = 1;
		}
		simulatedThisTick = true;
		tuckFeet(player);

		Vec3 velocity = player.getDeltaMovement();
		STATE.velX = config.toUnitsPerSecond(velocity.x);
		STATE.velY = config.toUnitsPerSecond(velocity.y);
		STATE.velZ = config.toUnitsPerSecond(velocity.z);
		STATE.onGround = player.onGround() && STATE.velY <= SourceMovement.NON_JUMP_VELOCITY;

		readWorld(player);
		EntityAccessor entity = (EntityAccessor) player;
		Vec3 scale = new Vec3(1, 1, 1);
		Vec3 stuck = entity.sourcemovement$getStuckSpeedMultiplier();
		if (stuck.lengthSqr() > 1.0E-7) {
			// Cobwebs, berry bushes and powder snow: vanilla scales the move and drops all velocity.
			scale = stuck;
			entity.sourcemovement$setStuckSpeedMultiplier(Vec3.ZERO);
			STATE.velX = 0;
			STATE.velY = 0;
			STATE.velZ = 0;
		}

		MinecraftCollider collider = new MinecraftCollider(player, config.unitsPerBlock, scale);
		for (long end : clock.advance(now)) {
			TIMELINE.fill(INPUT, end);
			INPUT.gravityScale = gravityScale(player);
			boolean wasOnGround = STATE.onGround;
			SourceMovement.step(STATE, INPUT, config, collider, SubstepClock.SOURCE_TICK);
			if (!wasOnGround && STATE.onGround) {
				// A bunnyhop can land and take off inside one tick, which the once per tick position
				// packet would never show. Report the landing so the server applies fall damage and
				// sees the next jump.
				player.connection.send(new ServerboundMovePlayerPacket.Pos(player.position(), true, player.horizontalCollision));
			}
			// Soul sand and honey: vanilla's once per tick speed factor, spread over the Source ticks.
			double blockFactor = Math.pow(entity.sourcemovement$getBlockSpeedFactor(), SubstepClock.SOURCE_TICK / SubstepClock.GAME_TICK);
			STATE.velX *= blockFactor;
			STATE.velZ *= blockFactor;
		}

		player.setDeltaMovement(
			config.toBlocksPerTick(STATE.velX),
			config.toBlocksPerTick(STATE.velY),
			config.toBlocksPerTick(STATE.velZ)
		);
	}

	/**
	 * Ducking in the air pulls the feet up by duckJumpLift like CS:GO, which is what makes a crouch
	 * jump reach higher. Standing up in the air lowers them again. Minecraft has already shrunk or
	 * grown the hull from the top by the time this runs.
	 */
	private static void tuckFeet(LocalPlayer player) {
		boolean crouched = player.getPose() == Pose.CROUCHING;
		boolean changed = crouched != crouchedLastTick;
		crouchedLastTick = crouched;
		if (!changed || !simulatedLastTick || player.onGround()) {
			return;
		}
		double lift = config.duckJumpLift / config.unitsPerBlock;
		if (!crouched) {
			player.move(MoverType.SELF, new Vec3(0, -lift, 0));
			return;
		}
		if (player.level().noCollision(player, player.getBoundingBox().move(0, lift, 0))) {
			player.move(MoverType.SELF, new Vec3(0, lift, 0));
		}
	}

	/** Sets the parts of the user command that come from effects, attributes and items. */
	private static void readWorld(LocalPlayer player) {
		// Sprinting adds 30% to the speed attribute, but in Source the sprint key does not change speed.
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		double speedValue = speed.getValue();
		if (speed.hasModifier(SPRINTING_MODIFIER_ID)) {
			speedValue /= 1.3;
		}
		INPUT.speedScale = speedValue / BASE_MOVEMENT_SPEED;

		INPUT.inputScale = 1;
		if (player.isUsingItem()) {
			INPUT.inputScale = ((LocalPlayerAccessor) player).sourcemovement$itemUseSpeedMultiplier();
		}
		INPUT.ducked = player.isMovingSlowly();
		INPUT.duckMultiplier = config.duckSpeedMultiplier * player.getAttributeValue(Attributes.SNEAKING_SPEED) / BASE_SNEAKING_SPEED;
		INPUT.jumpScale = ((LivingEntityAccessor) player).sourcemovement$getJumpPower() / BASE_JUMP_POWER;
		INPUT.stepHeight = player.maxUpStep() * config.unitsPerBlock;
	}

	/** Gravity attribute and Slow Falling relative to vanilla's 0.08, checked like vanilla against the fall direction. */
	private static double gravityScale(LocalPlayer player) {
		double gravity = player.getGravity();
		if (STATE.velY <= 0 && player.hasEffect(MobEffects.SLOW_FALLING)) {
			gravity = Math.min(gravity, 0.01);
		}
		return gravity / BASE_GRAVITY;
	}

	/** Records a movement or jump key changing state, with the time it happened. */
	public static void onKey(KeyMapping key, boolean down) {
		Options options = Minecraft.getInstance().options;
		if (options == null) {
			return;
		}
		int index = -1;
		if (key == options.keyUp) {
			index = InputTimeline.FORWARD;
		} else if (key == options.keyDown) {
			index = InputTimeline.BACK;
		} else if (key == options.keyLeft) {
			index = InputTimeline.LEFT;
		} else if (key == options.keyRight) {
			index = InputTimeline.RIGHT;
		} else if (key == options.keyJump) {
			index = InputTimeline.JUMP;
		}
		if (index >= 0) {
			TIMELINE.addKey(System.nanoTime(), index, down);
		}
	}

	/**
	 * Handles a vertical hotbar scroll. Returns true when the scroll was taken as a jump, in which
	 * case the hotbar must not move.
	 */
	public static boolean onScroll(int notches) {
		if (!enabled || notches == 0) {
			return false;
		}
		String mode = config.scrollJump;
		boolean up = notches > 0;
		boolean jumps = mode.equals("both");
		if (up && mode.equals("up")) {
			jumps = true;
		}
		if (!up && mode.equals("down")) {
			jumps = true;
		}
		if (jumps) {
			TIMELINE.addScroll(System.nanoTime());
		}
		return jumps;
	}

	/** Runs at the end of every client tick. Keeps the timeline drained while vanilla is in charge. */
	public static void endTick() {
		if (!simulatedThisTick) {
			TIMELINE.fill(INPUT, System.nanoTime());
		}
		simulatedLastTick = simulatedThisTick;
		simulatedThisTick = false;
	}

	/** Horizontal speed in units per second, for the speedometer. */
	public static double horizontalSpeed(LocalPlayer player) {
		Vec3 velocity = player.getDeltaMovement();
		return config.toUnitsPerSecond(velocity.horizontalDistance());
	}
}
