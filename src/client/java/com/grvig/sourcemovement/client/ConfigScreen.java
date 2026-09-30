package com.grvig.sourcemovement.client;

import com.grvig.sourcemovement.core.MoveConfig;
import java.util.function.DoubleConsumer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The settings screen, built with Cloth Config. Only loaded when Cloth Config is installed.
 */
public final class ConfigScreen {
	private ConfigScreen() {
	}

	public static Screen create(Screen parent) {
		MoveConfig config = SourceController.config;
		MoveConfig defaults = new MoveConfig();
		ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.literal("Source Movement"));
		ConfigEntryBuilder entries = builder.entryBuilder();

		ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
		general.addEntry(entries.startSelector(Component.literal("Preset"), new String[] {"csgo_bhop", "csgo"}, config.preset)
			.setDefaultValue(defaults.preset)
			.setTooltip(Component.literal("csgo_bhop: no jump speed cap. csgo: matchmaking's 1.1x cap on every jump."))
			.setSaveConsumer(value -> config.preset = value)
			.build());
		general.addEntry(entries.startBooleanToggle(Component.literal("Autohop"), config.autohop)
			.setDefaultValue(defaults.autohop)
			.setTooltip(Component.literal("Holding jump jumps again on every landing."))
			.setSaveConsumer(value -> config.autohop = value)
			.build());
		general.addEntry(entries.startSelector(Component.literal("Scroll jump"), new String[] {"both", "up", "down", "off"}, config.scrollJump)
			.setDefaultValue(defaults.scrollJump)
			.setSaveConsumer(value -> config.scrollJump = value)
			.build());
		general.addEntry(entries.startBooleanToggle(Component.literal("Allow on servers"), config.allowMultiplayer)
			.setDefaultValue(defaults.allowMultiplayer)
			.setTooltip(Component.literal("Servers may kick or rubber band you for Source speeds."))
			.setSaveConsumer(value -> config.allowMultiplayer = value)
			.build());
		number(general, entries, "Crouch jump lift", config.duckJumpLift, defaults.duckJumpLift, value -> config.duckJumpLift = value);

		builder.setSavingRunnable(() -> {
			config.validate();
			ConfigFile.save(config);
		});
		return builder.build();
	}

	private static void number(ConfigCategory category, ConfigEntryBuilder entries, String name, double value, double fallback, DoubleConsumer save) {
		category.addEntry(entries.startDoubleField(Component.literal(name), value)
			.setDefaultValue(fallback)
			.setSaveConsumer(save::accept)
			.build());
	}
}
