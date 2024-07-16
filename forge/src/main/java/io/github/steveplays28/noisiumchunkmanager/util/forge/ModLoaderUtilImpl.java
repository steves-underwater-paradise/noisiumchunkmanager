package io.github.steveplays28.noisiumchunkmanager.util.forge;

import io.github.steveplays28.noisiumchunkmanager.util.ModLoaderUtil;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
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
		return LoadingModList.get().getModFileById(id) != null;
	}

	/**
	 * @return The config directory of the mod loader.
	 */
	public static @NotNull Path getConfigDir() {
		return FMLPaths.CONFIGDIR.get();
	}
}
