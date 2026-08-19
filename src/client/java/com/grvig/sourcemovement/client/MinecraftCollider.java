package com.grvig.sourcemovement.client;

import com.grvig.sourcemovement.core.Collider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Moves the local player through Minecraft's own collision, so step up, sneaking at edges and
 * fall damage all stay vanilla. Minecraft is axis aligned, so an axis that did not move its full
 * distance is the same as Source clipping the velocity against that wall.
 */
public class MinecraftCollider implements Collider {
	private static final double MISS = 1.0E-5;

	private final LocalPlayer player;
	private final double unitsPerBlock;
	private final Vec3 scale;

	public MinecraftCollider(LocalPlayer player, double unitsPerBlock, Vec3 scale) {
		this.player = player;
		this.unitsPerBlock = unitsPerBlock;
		this.scale = scale;
	}

	@Override
	public int move(double dx, double dy, double dz) {
		Vec3 wanted = new Vec3(dx, dy, dz).scale(1 / unitsPerBlock).multiply(scale);
		Vec3 before = player.position();
		player.move(MoverType.SELF, wanted);
		Vec3 moved = player.position().subtract(before);
		int blocked = 0;
		if (Math.abs(moved.x - wanted.x) > MISS) {
			blocked |= BLOCKED_X;
		}
		if (Math.abs(moved.y - wanted.y) > MISS) {
			blocked |= BLOCKED_Y;
		}
		if (Math.abs(moved.z - wanted.z) > MISS) {
			blocked |= BLOCKED_Z;
		}
		return blocked;
	}

	@Override
	public boolean findGround(double maxDrop) {
		double drop = maxDrop / unitsPerBlock;
		if (player.getAvailableSpaceBelow(drop) >= drop) {
			return false;
		}
		player.move(MoverType.SELF, new Vec3(0, -drop, 0));
		return player.onGround();
	}

	/** Maps block slipperiness onto Source surface friction: 1 for normal blocks, 0.05 for ice. */
	@Override
	public double groundFriction() {
		BlockPos pos = player.getBlockPosBelowThatAffectsMyMovement();
		double slipperiness = player.level().getBlockState(pos).getBlock().getFriction();
		return Math.clamp((1 - slipperiness) / (1 - 0.6), 0.01, 1);
	}
}
