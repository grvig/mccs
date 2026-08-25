package com.grvig.sourcemovement.client.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
	@Accessor("stuckSpeedMultiplier")
	Vec3 sourcemovement$getStuckSpeedMultiplier();

	@Accessor("stuckSpeedMultiplier")
	void sourcemovement$setStuckSpeedMultiplier(Vec3 multiplier);

	@Invoker("getBlockSpeedFactor")
	float sourcemovement$getBlockSpeedFactor();
}
