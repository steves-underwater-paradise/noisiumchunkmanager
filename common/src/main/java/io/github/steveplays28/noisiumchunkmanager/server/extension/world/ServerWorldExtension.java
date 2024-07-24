package io.github.steveplays28.noisiumchunkmanager.server.extension.world;

import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.world.lighting.ServerWorldLightingProvider;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface ServerWorldExtension {
	ServerWorldChunkManager noisiumchunkmanager$getServerWorldChunkManager();

	NoiseConfig noisiumchunkmanager$getNoiseConfig();

	ServerWorldLightingProvider noisiumchunkmanager$getServerWorldLightingProvider();

	@NotNull CompletableFuture<WorldChunk> noisiumchunkmanager$getChunkAsync(@NotNull ChunkPos chunkPosition);

	void noisiumchunkmanager$sendPacketToNearbyPlayers(@NotNull Packet<?> packet);
}
