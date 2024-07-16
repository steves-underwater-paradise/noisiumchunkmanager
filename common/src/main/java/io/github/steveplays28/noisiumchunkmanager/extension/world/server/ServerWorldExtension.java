package io.github.steveplays28.noisiumchunkmanager.extension.world.server;

import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import net.minecraft.world.gen.noise.NoiseConfig;

public interface ServerWorldExtension {
	ServerWorldChunkManager noisiumchunkmanager$getServerWorldChunkManager();

	NoiseConfig noisiumchunkmanager$getNoiseConfig();
}
