package com.grvig.sourcemovement.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Horizontal speed in units per second under the crosshair, like a KZ speed readout.
 */
public final class Speedometer {
	public static boolean visible;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF55FF55;
	private static final int RED = 0xFFFF5555;
	/** Smaller changes than this, in u/s per tick, are rounding and stay white. */
	private static final double CHANGE_THRESHOLD = 0.05;

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
		int color = WHITE;
		if (SourceController.speedChange > CHANGE_THRESHOLD) {
			color = GREEN;
		} else if (SourceController.speedChange < -CHANGE_THRESHOLD) {
			color = RED;
		}
		graphics.drawString(client.font, text, x, y, color);
	}
}
