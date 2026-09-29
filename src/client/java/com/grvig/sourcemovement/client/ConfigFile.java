package com.grvig.sourcemovement.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.grvig.sourcemovement.core.MoveConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads config/sourcemovement.json, writing the defaults there the first time.
 */
public final class ConfigFile {
	private static final Logger LOGGER = LoggerFactory.getLogger("sourcemovement");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigFile() {
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("sourcemovement.json");
	}

	/** What the last load had to fix, one line each. Empty when the file was fine. */
	public static List<String> problems = List.of();

	public static MoveConfig load() {
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				MoveConfig config = GSON.fromJson(reader, MoveConfig.class);
				if (config != null) {
					problems = config.validate();
					for (String problem : problems) {
						LOGGER.warn("{}: {}", path.getFileName(), problem);
					}
					return config;
				}
			} catch (IOException | JsonParseException e) {
				LOGGER.error("Could not read {}, using defaults", path, e);
				problems = List.of("could not be read: " + e.getMessage());
				return new MoveConfig();
			}
		}
		problems = List.of();
		MoveConfig config = new MoveConfig();
		save(config);
		return config;
	}

	public static void save(MoveConfig config) {
		Path path = path();
		try {
			Files.writeString(path, GSON.toJson(config));
		} catch (IOException e) {
			LOGGER.error("Could not write {}", path, e);
		}
	}
}
