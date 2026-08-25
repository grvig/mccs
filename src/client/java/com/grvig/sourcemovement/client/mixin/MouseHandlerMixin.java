package com.grvig.sourcemovement.client.mixin;

import com.grvig.sourcemovement.client.SourceController;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	/** The view turns once per rendered frame, right here, so this samples every frame's yaw. */
	@Inject(method = "handleAccumulatedMovement", at = @At("TAIL"))
	private void sourcemovement$recordYaw(CallbackInfo ci) {
		if (minecraft.player != null) {
			SourceController.TIMELINE.addYaw(System.nanoTime(), minecraft.player.getYRot());
		}
	}

	/**
	 * Only reached with no screen open and outside spectator mode, so menus, chat and spectator
	 * fly speed keep vanilla scrolling.
	 */
	@WrapOperation(
		method = "onScroll",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V")
	)
	private void sourcemovement$scrollJump(Inventory inventory, int slot, Operation<Void> original, @Local Vector2i notches) {
		if (!SourceController.onScroll(notches.y)) {
			original.call(inventory, slot);
		}
	}
}
