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

		ConfigCategory physics = builder.getOrCreateCategory(Component.literal("Physics"));
		number(physics, entries, "sv_accelerate", config.accelerate, defaults.accelerate, value -> config.accelerate = value);
		number(physics, entries, "sv_airaccelerate", config.airAccelerate, defaults.airAccelerate, value -> config.airAccelerate = value);
		number(physics, entries, "sv_air_max_wishspeed", config.airMaxWishSpeed, defaults.airMaxWishSpeed, value -> config.airMaxWishSpeed = value);
		number(physics, entries, "sv_friction", config.friction, defaults.friction, value -> config.friction = value);
		number(physics, entries, "sv_stopspeed", config.stopSpeed, defaults.stopSpeed, value -> config.stopSpeed = value);
		number(physics, entries, "Run speed", config.runSpeed, defaults.runSpeed, value -> config.runSpeed = value);
		number(physics, entries, "sv_maxspeed", config.maxSpeed, defaults.maxSpeed, value -> config.maxSpeed = value);
		number(physics, entries, "Duck speed multiplier", config.duckSpeedMultiplier, defaults.duckSpeedMultiplier, value -> config.duckSpeedMultiplier = value);
		number(physics, entries, "sv_gravity", config.gravity, defaults.gravity, value -> config.gravity = value);
		number(physics, entries, "sv_jump_impulse", config.jumpImpulse, defaults.jumpImpulse, value -> config.jumpImpulse = value);
		number(physics, entries, "sv_maxvelocity", config.maxVelocity, defaults.maxVelocity, value -> config.maxVelocity = value);
		number(physics, entries, "Bhop cap factor", config.bhopCapFactor, defaults.bhopCapFactor, value -> config.bhopCapFactor = value);
		number(physics, entries, "Units per block", config.unitsPerBlock, defaults.unitsPerBlock, value -> config.unitsPerBlock = value);

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
