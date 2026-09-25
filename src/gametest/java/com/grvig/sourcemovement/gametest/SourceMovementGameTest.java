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

}
