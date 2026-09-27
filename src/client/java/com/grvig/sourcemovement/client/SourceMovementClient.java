package com.grvig.sourcemovement.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class SourceMovementClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sourcemovement", "main"));

	private KeyMapping toggleKey;
	private KeyMapping speedometerKey;

	@Override
	public void onInitializeClient() {
		SourceController.config = ConfigFile.load();
		toggleKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.sourcemovement.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY)
		);

		speedometerKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.sourcemovement.speedometer", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY)
		);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("sourcemovement", "speedometer"), Speedometer::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SourceController.endTick();
			while (toggleKey.consumeClick()) {
				toggle(client);
			}
			while (speedometerKey.consumeClick()) {
				Speedometer.visible = !Speedometer.visible;
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommandManager.literal("sourcemovement")
				.then(ClientCommandManager.literal("reload").executes(context -> {
					SourceController.config = ConfigFile.load();
					context.getSource().sendFeedback(Component.literal("Reloaded " + ConfigFile.path().getFileName()));
					for (String problem : ConfigFile.problems) {
						context.getSource().sendError(Component.literal("Fixed: " + problem));
					}
					return 1;
				}))
				.then(ClientCommandManager.literal("toggle").executes(context -> {
					toggle(context.getSource().getClient());
					return 1;
				}))
		));
	}

	private static void toggle(Minecraft client) {
		SourceController.enabled = !SourceController.enabled;
		if (client.player != null) {
			String state = "off";
			if (SourceController.enabled) {
				state = "on";
			}
			String message = "Source movement " + state;
			if (SourceController.enabled && !SourceController.activeHere()) {
				message = "Source movement stays off on servers unless allowMultiplayer is set";
			}
			client.player.displayClientMessage(Component.literal(message), true);
		}
	}
}
