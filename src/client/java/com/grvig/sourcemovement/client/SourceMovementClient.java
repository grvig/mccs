package com.grvig.sourcemovement.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class SourceMovementClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SourceController.config = ConfigFile.load();
		ClientTickEvents.END_CLIENT_TICK.register(client -> SourceController.endTick());
	}
}
