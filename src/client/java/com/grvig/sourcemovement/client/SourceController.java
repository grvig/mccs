package com.grvig.sourcemovement.client;

import com.grvig.sourcemovement.core.InputTimeline;
import com.grvig.sourcemovement.core.MoveConfig;
import com.grvig.sourcemovement.core.MoveInput;
import com.grvig.sourcemovement.core.MoveState;
import com.grvig.sourcemovement.core.SourceMovement;
import com.grvig.sourcemovement.core.SubstepClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Runs Source movement for the local player in place of Minecraft's travel. Minecraft's velocity
 * stays the source of truth: it is read at the start of every tick and written back at the end,
 * so knockback, explosions and the switch to and from vanilla movement all carry over.
 */
public final class SourceController {
	private static final long GAME_TICK_NANOS = 50_000_000L;

	public static MoveConfig config = new MoveConfig();
	public static boolean enabled = true;
	public static final InputTimeline TIMELINE = new InputTimeline();

	private static final MoveState STATE = new MoveState();
	private static final MoveInput INPUT = new MoveInput();
	private static SubstepClock clock = new SubstepClock();
	private static boolean simulatedLastTick;
	private static boolean simulatedThisTick;

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

		Vec3 velocity = player.getDeltaMovement();
		STATE.velX = config.toUnitsPerSecond(velocity.x);
		STATE.velY = config.toUnitsPerSecond(velocity.y);
		STATE.velZ = config.toUnitsPerSecond(velocity.z);
		STATE.onGround = player.onGround() && STATE.velY <= SourceMovement.NON_JUMP_VELOCITY;

		MinecraftCollider collider = new MinecraftCollider(player, config.unitsPerBlock, new Vec3(1, 1, 1));
		for (long end : clock.advance(now)) {
			TIMELINE.fill(INPUT, end);
			SourceMovement.step(STATE, INPUT, config, collider, SubstepClock.SOURCE_TICK);
		}

		player.setDeltaMovement(
			config.toBlocksPerTick(STATE.velX),
			config.toBlocksPerTick(STATE.velY),
			config.toBlocksPerTick(STATE.velZ)
		);
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
