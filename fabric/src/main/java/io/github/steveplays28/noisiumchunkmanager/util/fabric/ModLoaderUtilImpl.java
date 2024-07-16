package io.github.steveplays28.noisiumchunkmanager.util.fabric;

import io.github.steveplays28.noisiumchunkmanager.util.ModLoaderUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Implements {@link ModLoaderUtil}.
 */
@SuppressWarnings("unused")
public class ModLoaderUtilImpl {
	/**
	 * Checks if a mod is present during loading.
	 */
	public static boolean isModPresent(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}

	/**
	 * @return The config directory of the mod loader.
	 */
	public static @NotNull Path getConfigDir() {
		return FabricLoader.getInstance().getConfigDir();
	}
}
