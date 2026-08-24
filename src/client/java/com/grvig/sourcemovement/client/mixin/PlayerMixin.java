package com.grvig.sourcemovement.client.mixin;

import com.grvig.sourcemovement.client.SourceController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void sourcemovement$travel(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && SourceController.shouldSimulate(player)) {
			SourceController.travel(player);
			ci.cancel();
		}
	}
}
