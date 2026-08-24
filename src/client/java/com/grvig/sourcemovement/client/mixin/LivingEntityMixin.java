package com.grvig.sourcemovement.client.mixin;

import com.grvig.sourcemovement.client.SourceController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Source movement does its own jumping, so the vanilla jump from aiStep is skipped. */
	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void sourcemovement$skipVanillaJump(CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && SourceController.shouldSimulate(player)) {
			ci.cancel();
		}
	}
}
