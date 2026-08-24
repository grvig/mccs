package com.grvig.sourcemovement.client.mixin;

import com.grvig.sourcemovement.client.SourceController;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
	/** Every key state change, keyboard or mouse button, toggle or hold, goes through setDown. */
	@Inject(method = "setDown", at = @At("HEAD"))
	private void sourcemovement$recordKey(boolean down, CallbackInfo ci) {
		KeyMapping self = (KeyMapping) (Object) this;
		if (self.isDown() != down) {
			SourceController.onKey(self, down);
		}
	}
}
