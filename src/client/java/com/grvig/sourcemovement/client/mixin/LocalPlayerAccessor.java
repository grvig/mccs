package com.grvig.sourcemovement.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
	/** How much using the held item slows movement, 0.2 for eating or drawing a bow. */
	@Invoker("itemUseSpeedMultiplier")
	float sourcemovement$itemUseSpeedMultiplier();
}
