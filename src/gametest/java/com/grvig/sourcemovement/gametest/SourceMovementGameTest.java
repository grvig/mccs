package com.grvig.sourcemovement.gametest;

import com.grvig.sourcemovement.client.ModMenuIntegration;
import com.grvig.sourcemovement.client.SourceController;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays the mod in a real singleplayer world: running, jumping, strafing, scroll jumps and the
 * vanilla fallbacks, checking the numbers against the Source maths and the server's view.
 */
public class SourceMovementGameTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("sourcemovement-gametest");
	private static final double UNITS = 40;
	/** Where a player stands on the default superflat world. */
	private static final int SURFACE_Y = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
		settingsScreen(context);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("difficulty peaceful");
			server.runCommand("execute as @a at @s run tp @s 0 ~ 0 0 0");
			context.waitTicks(20);

			runSpeed(context);
			jumpHeight(context);
			scrollJump(context);
			airStrafe(context);
			serverAgrees(context, server);
			fallDamage(context, server);
			waterFallsBackToVanilla(context, server);
			speedEffect(context, server);
			jumpBoost(context, server);
			ice(context, server);
			soulSand(context, server);
			cobweb(context, server);
			slabSteps(context, server);
			ladder(context, server);
			knockback(context, server);
		}
	}

	private static void settingsScreen(ClientGameTestContext context) {
		Screen title = context.computeOnClient(client -> client.screen);
		Screen screen = context.computeOnClient(client -> new ModMenuIntegration().getModConfigScreenFactory().create(title));
		check(screen != null, "Mod Menu gets a settings screen");
		context.setScreen(() -> screen);
		context.waitTicks(5);
		boolean open = context.computeOnClient(client -> client.screen == screen);
		check(open, "the settings screen opens");
		context.setScreen(() -> title);
	}

	private static LocalPlayer player(Minecraft client) {
		return client.player;
	}

	private static double speed(ClientGameTestContext context) {
		return context.computeOnClient(client -> SourceController.horizontalSpeed(player(client)));
	}

	private static Vec3 position(ClientGameTestContext context) {
		return context.computeOnClient(client -> player(client).position());
	}

	private static boolean onGround(ClientGameTestContext context) {
		return context.computeOnClient(client -> player(client).onGround());
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			LOGGER.error("FAIL: {}", message);
			throw new AssertionError(message);
		}
		LOGGER.info("PASS: {}", message);
	}

	private static void waitForGround(ClientGameTestContext context) {
		context.waitFor(client -> player(client).onGround(), 200);
		context.waitTicks(5);
	}

	private static void runSpeed(ClientGameTestContext context) {
		TestInput input = context.getInput();
		input.holdKey(options -> options.keyUp);
		context.waitTicks(40);
		double running = speed(context);
		input.releaseKey(options -> options.keyUp);
		check(Math.abs(running - 250) < 0.5, "runs at 250 u/s, got " + running);
		context.waitTicks(15);
		double stopped = speed(context);
		check(stopped < 0.01, "friction stops the player, got " + stopped);
	}

	/** Taps jump and returns the highest point reached, in units above the start. */
	private static double jumpApex(ClientGameTestContext context) {
		waitForGround(context);
		double startY = position(context).y;
		context.getInput().pressKey(options -> options.keyJump);
		double apex = 0;
		for (int i = 0; i < 50; i++) {
			context.waitTick();
			apex = Math.max(apex, (position(context).y - startY) * UNITS);
		}
		return apex;
	}

	private static void jumpHeight(ClientGameTestContext context) {
		double apex = jumpApex(context);
		check(Math.abs(apex - 54.65) < 1.0, "jump apex near 54.65 units, got " + apex);
		check(onGround(context), "lands again after a jump");
	}

	private static void scrollJump(ClientGameTestContext context) {
		waitForGround(context);
		int slot = context.computeOnClient(client -> player(client).getInventory().getSelectedSlot());
		double startY = position(context).y;
		context.getInput().scroll(1);
		context.waitTicks(4);
		double rise = (position(context).y - startY) * UNITS;
		int slotAfter = context.computeOnClient(client -> player(client).getInventory().getSelectedSlot());
		check(rise > 10, "scrolling up jumps, rose " + rise + " units");
		check(slot == slotAfter, "scroll jump leaves the hotbar slot alone");
		waitForGround(context);
	}

	private static void airStrafe(ClientGameTestContext context) {
		waitForGround(context);
		TestInput input = context.getInput();
		SourceController.config.autohop = true;
		input.holdKey(options -> options.keyUp);
		context.waitTicks(30);
		input.releaseKey(options -> options.keyUp);
		input.holdKey(options -> options.keyJump);
		input.holdKey(options -> options.keyRight);
		double best = 0;
		// Test frames come once a tick, so the view turns in steps rather than smoothly. 60 pixels
		// at default sensitivity is 9 degrees a tick, slow enough to gain speed even in steps.
		for (int i = 0; i < 100; i++) {
			input.moveCursor(60, 0);
			context.waitTick();
			best = Math.max(best, speed(context));
			if (i % 20 == 0) {
				LOGGER.info("strafe tick {}: {} u/s", i, speed(context));
			}
		}
		input.releaseKey(options -> options.keyRight);
		input.releaseKey(options -> options.keyJump);
		SourceController.config.autohop = false;
		check(best > 300, "strafing while bunnyhopping builds speed past 300 u/s, got " + best);
		// Right strafe while turning right the whole time is perfect sync.
		double sync = SourceController.STATS.sync;
		check(sync > 0.9, "jump stats see the strafe as in sync, got " + sync);
		waitForGround(context);
	}

	private static void serverAgrees(ClientGameTestContext context, TestServerContext server) {
		context.waitTicks(10);
		Vec3 client = position(context);
		Vec3 onServer = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).position());
		double gap = client.distanceTo(onServer);
		check(gap < 0.01, "server and client positions agree after strafing, gap " + gap);
	}

	private static void fallDamage(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamerule natural_health_regeneration false");
		server.runCommand("execute as @a at @s run tp @s ~ ~10 ~");
		context.waitTicks(5);
		float lowest = 20;
		for (int i = 0; i < 60; i++) {
			context.waitTick();
			float health = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getHealth());
			lowest = Math.min(lowest, health);
		}
		check(lowest == 13, "a 10 block fall does vanilla's 7 damage, lowest health " + lowest);
		server.runCommand("gamerule natural_health_regeneration true");
		server.runCommand("effect give @a instant_health 1 5");
	}

	private static void waterFallsBackToVanilla(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("execute at @a run setblock ~ ~ ~ water");
		context.waitTicks(10);
		boolean simulating = context.computeOnClient(client -> SourceController.shouldSimulate(player(client)));
		check(!simulating, "water hands movement back to vanilla");
		server.runCommand("execute at @a run setblock ~ ~ ~ air");
		context.waitTicks(10);
	}

	/**
	 * Puts the player on clean ground at the given x, facing +Z, so each test gets its own strip.
	 * The height is absolute because soul sand leaves the player standing a little low.
	 */
	private static void moveTo(ClientGameTestContext context, TestServerContext server, int x) {
		server.runCommand("tp @a " + x + " " + SURFACE_Y + " 0 0 0");
		context.waitTicks(10);
		waitForGround(context);
	}

	/** Holds forward for some ticks and returns the speed at the end. */
	private static double runForward(ClientGameTestContext context, int ticks) {
		TestInput input = context.getInput();
		input.holdKey(options -> options.keyUp);
		context.waitTicks(ticks);
		double speed = speed(context);
		input.releaseKey(options -> options.keyUp);
		return speed;
	}

	private static void speedEffect(ClientGameTestContext context, TestServerContext server) {
		moveTo(context, server, 30);
		server.runCommand("effect give @a speed 30 0 true");
		context.waitTicks(2);
		double running = runForward(context, 40);
		server.runCommand("effect clear @a speed");
		check(Math.abs(running - 300) < 0.5, "Speed I runs 20% faster at 300 u/s, got " + running);
		context.waitTicks(15);
	}

	private static void jumpBoost(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("effect give @a jump_boost 30 0 true");
		context.waitTicks(2);
		double apex = jumpApex(context);
		server.runCommand("effect clear @a jump_boost");
		// Vanilla jump power 0.52 instead of 0.42 scales the impulse to 373.9, which peaks at 84.46.
		check(Math.abs(apex - 84.46) < 1.0, "Jump Boost I raises the apex to about 84.5 units, got " + apex);
	}

	private static void ice(ClientGameTestContext context, TestServerContext server) {
		moveTo(context, server, 40);
		server.runCommand("execute at @a run fill ~-3 ~-1 ~-3 ~3 ~-1 ~30 ice");
		context.waitTicks(5);
		double running = runForward(context, 60);
		context.waitTicks(10);
		double coasting = speed(context);
		check(running > 50 && running < 240, "ice is slow to get going, " + running + " u/s after 3 seconds");
		check(coasting > running * 0.8, "ice keeps sliding, " + coasting + " u/s half a second after letting go");
		context.waitTicks(60);
	}

	private static void soulSand(ClientGameTestContext context, TestServerContext server) {
		moveTo(context, server, 50);
		server.runCommand("execute at @a run fill ~-3 ~-1 ~-3 ~3 ~-1 ~30 soul_sand");
		context.waitTicks(5);
		double running = runForward(context, 40);
		check(running > 20 && running < 125, "soul sand slows running, got " + running);
		context.waitTicks(15);
	}

	private static void cobweb(ClientGameTestContext context, TestServerContext server) {
		moveTo(context, server, 60);
		server.runCommand("execute at @a run fill ~ ~ ~ ~ ~1 ~6 cobweb");
		context.waitTicks(5);
		double startZ = position(context).z;
		runForward(context, 20);
		double moved = position(context).z - startZ;
		server.runCommand("execute at @a run fill ~-1 ~ ~-8 ~1 ~1 ~8 air replace cobweb");
		check(moved > 0.01 && moved < 1.0, "cobwebs hold the player to a crawl, moved " + moved + " blocks in a second");
		context.waitTicks(10);
	}

	private static void slabSteps(ClientGameTestContext context, TestServerContext server) {
		moveTo(context, server, 70);
		server.runCommand("execute at @a run fill ~-1 ~ ~3 ~1 ~ ~6 smooth_stone_slab");
		context.waitTicks(5);
		double startY = position(context).y;
		TestInput input = context.getInput();
		input.holdKey(options -> options.keyUp);
		double highest = 0;
		for (int i = 0; i < 40; i++) {
			context.waitTick();
			highest = Math.max(highest, position(context).y - startY);
		}
		double speed = speed(context);
		input.releaseKey(options -> options.keyUp);
		double end = position(context).y - startY;
		check(Math.abs(highest - 0.5) < 0.01, "walks up onto a slab without jumping, rose " + highest);
		check(Math.abs(end) < 0.01 && speed > 240, "walks back down at full speed, ended " + end + " high at " + speed + " u/s");
		context.waitTicks(15);
	}

	private static void ladder(ClientGameTestContext context, TestServerContext server) {
		moveTo(context, server, 80);
		server.runCommand("execute at @a run setblock ~ ~ ~-1 stone");
		server.runCommand("execute at @a run setblock ~ ~ ~ ladder[facing=south]");
		context.waitTicks(10);
		boolean simulating = context.computeOnClient(client -> SourceController.shouldSimulate(player(client)));
		check(!simulating, "ladders hand movement back to vanilla");
		server.runCommand("execute at @a run setblock ~ ~ ~ air");
		context.waitTicks(10);
	}

	private static void knockback(ClientGameTestContext context, TestServerContext server) {
		moveTo(context, server, 90);
		Vec3 start = position(context);
		server.runOnServer(s -> {
			var player = s.getPlayerList().getPlayers().get(0);
			player.knockback(1.0, 0, -1);
			player.hurtMarked = true;
		});
		context.waitTicks(15);
		Vec3 end = position(context);
		double pushed = Math.hypot(end.x - start.x, end.z - start.z);
		check(pushed > 1.0, "knockback still pushes the player, moved " + pushed + " blocks");
		waitForGround(context);
	}
}
