package io.github.steveplays28.noisiumchunkmanager.server.packet.world.lighting;

import io.github.steveplays28.noisiumchunkmanager.server.packet.world.lighting.data.ServerWorldLightData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

/**
 * Builds a {@link ChunkDataS2CPacket} without requiring a {@link net.minecraft.world.chunk.light.LightingProvider} to be passed in.
 */
public class ChunkDataS2CPacketBuilder {
	private final int chunkPositionX;
	private final int chunkPositionZ;
	private final @NotNull ChunkData chunkData;
	private final @NotNull ServerWorldLightData lightData;

	/**
	 * Creates a new {@link ChunkDataS2CPacketBuilder}.
	 *
	 * @param worldChunk              The {@link WorldChunk}.
	 * @param chunkHeight             The amount of vertical sections of the {@link net.minecraft.world.World} {@code + 2}.
	 * @param chunkBottomYPosition    The bottom section coordinate of the {@link net.minecraft.world.World} {@code - 1}.
	 * @param chunkSkyLightProvider   The {@link ChunkLightProvider} for sky light.
	 * @param chunkBlockLightProvider The {@link ChunkLightProvider} for block light.
	 * @param skyLightBits            The existing sky light {@link BitSet}.
	 * @param blockLightBits          The existing block light {@link BitSet}.
	 */
	public ChunkDataS2CPacketBuilder(@NotNull WorldChunk worldChunk, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider, @Nullable BitSet skyLightBits, @Nullable BitSet blockLightBits) {
		@NotNull var chunkPosition = worldChunk.getPos();
		this.chunkPositionX = chunkPosition.x;
		this.chunkPositionZ = chunkPosition.z;
		this.chunkData = new ChunkData(worldChunk);
		this.lightData = new ServerWorldLightData(
				chunkPosition, chunkHeight, chunkBottomYPosition, chunkSkyLightProvider,
				chunkBlockLightProvider, skyLightBits, blockLightBits
		);
	}

	/**
	 * @return A {@link ChunkDataS2CPacket}, built from the data provided to the {@link ChunkDataS2CPacketBuilder}.
	 */
	public @NotNull ChunkDataS2CPacket build() {
		@NotNull var buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeInt(chunkPositionX);
		buf.writeInt(chunkPositionZ);
		chunkData.write(buf);
		lightData.write(buf);
		return new ChunkDataS2CPacket(buf);
	}
}
