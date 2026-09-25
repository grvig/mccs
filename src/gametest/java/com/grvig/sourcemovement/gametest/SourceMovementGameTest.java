package com.grvig.sourcemovement.gametest;

import com.grvig.sourcemovement.client.SourceController;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
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

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
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
		}
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

	private static void jumpHeight(ClientGameTestContext context) {
		waitForGround(context);
		double startY = position(context).y;
		context.getInput().pressKey(options -> options.keyJump);
		double apex = 0;
		for (int i = 0; i < 40; i++) {
			context.waitTick();
			apex = Math.max(apex, (position(context).y - startY) * UNITS);
		}
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
		server.runCommand("execute as @a at @s run tp @s ~ ~10 ~");
		context.waitTicks(5);
		// Health is read every tick because full saturation heals it again within a few ticks.
		float lowest = 20;
		for (int i = 0; i < 60; i++) {
			context.waitTick();
			float health = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getHealth());
			lowest = Math.min(lowest, health);
		}
		check(lowest == 13, "a 10 block fall does vanilla's 7 damage, lowest health " + lowest);
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
}
