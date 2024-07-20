package io.github.steveplays28.noisiumchunkmanager.extension.world.server;

import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.NotNull;

public interface ServerWorldExtension {
	ServerWorldChunkManager noisiumchunkmanager$getServerWorldChunkManager();

	NoiseConfig noisiumchunkmanager$getNoiseConfig();

	void noisiumchunkmanager$sendPacketToNearbyPlayers(@NotNull Packet<?> packet);
}
