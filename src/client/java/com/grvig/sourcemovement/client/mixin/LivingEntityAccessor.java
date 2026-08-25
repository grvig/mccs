package com.grvig.sourcemovement.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	/** Vanilla jump velocity including Jump Boost and the block jump factor, 0.42 normally. */
	@Invoker("getJumpPower")
	float sourcemovement$getJumpPower();
}
