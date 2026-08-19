package com.grvig.sourcemovement.core;

/**
 * Runs Source's 64 Hz ticks inside Minecraft's 20 Hz ones. Each Minecraft tick covers the real time
 * since the previous one, and every Source tick that ends inside it gets its own slice of that time.
 */
public class SubstepClock {
	public static final double SOURCE_TICK = 1.0 / 64;
	public static final double GAME_TICK = 0.05;
	private static final long GAME_TICK_NANOS = 50_000_000L;
	private static final long STALE_NANOS = 250_000_000L;

	private double owed;
	private long lastTick;
	private boolean started;

	/** Returns when each Source tick due this Minecraft tick ends, in real time. */
	public long[] advance(long now) {
		long start = lastTick;
		if (!started || now - lastTick > STALE_NANOS) {
			start = now - GAME_TICK_NANOS;
		}
		started = true;
		lastTick = now;

		double carried = owed;
		owed += GAME_TICK;
		int count = (int) Math.floor(owed / SOURCE_TICK + 1e-9);
		owed -= count * SOURCE_TICK;

		long[] ends = new long[count];
		for (int k = 0; k < count; k++) {
			double gameEnd = (k + 1) * SOURCE_TICK - carried;
			ends[k] = start + Math.round(gameEnd / GAME_TICK * (now - start));
		}
		return ends;
	}
}
