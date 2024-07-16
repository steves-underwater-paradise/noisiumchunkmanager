package io.github.steveplays28.noisiumchunkmanager.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@SuppressWarnings("unused")
public abstract class ModLoaderUtil {
	/**
	 * Checks if a mod is present during loading.
	 */
	@ExpectPlatform
	public static boolean isModPresent(String id) {
		throw new AssertionError("Platform implementation expected.");
	}

	/**
	 * @return The config directory of the mod loader.
	 */
	@ExpectPlatform
	public static @NotNull Path getConfigDir() {
		throw new AssertionError("Platform implementation expected.");
	}
}
