package io.github.steveplays28.noisiumchunkmanager.server.extension.world;

import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.levelgen.RandomState;
import org.jetbrains.annotations.NotNull;

public interface ServerWorldExtension {
	ServerWorldChunkManager noisiumchunkmanager$getServerWorldChunkManager();

	RandomState noisiumchunkmanager$getNoiseConfig();

	void noisiumchunkmanager$sendPacketToNearbyPlayers(@NotNull Packet<?> packet);
}
