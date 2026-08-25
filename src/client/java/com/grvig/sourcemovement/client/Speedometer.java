package com.grvig.sourcemovement.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Horizontal speed in units per second under the crosshair, like a KZ speed readout.
 */
public final class Speedometer {
	public static boolean visible;

	private Speedometer() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (!visible || client.player == null || client.options.hideGui) {
			return;
		}
		long speed = Math.round(SourceController.horizontalSpeed(client.player));
		String text = speed + " u/s";
		int x = (graphics.guiWidth() - client.font.width(text)) / 2;
		int y = graphics.guiHeight() / 2 + 12;
		graphics.drawString(client.font, text, x, y, 0xFFFFFFFF);
	}
}
