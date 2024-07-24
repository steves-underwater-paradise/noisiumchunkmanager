package io.github.steveplays28.noisiumchunkmanager.server.packet.world.lighting;

import io.github.steveplays28.noisiumchunkmanager.server.packet.world.lighting.data.ServerWorldLightData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

/**
 * Builds a {@link LightUpdateS2CPacket} without requiring a {@link net.minecraft.world.chunk.light.LightingProvider} to be passed in.
 */
public class LightUpdateS2CPacketBuilder {
	private final int chunkPositionX;
	private final int chunkPositionZ;
	private final @NotNull ServerWorldLightData lightData;

	/**
	 * Creates a new {@link LightUpdateS2CPacketBuilder}.
	 *
	 * @param chunkPosition           The {@link ChunkPos} of the {@link net.minecraft.world.chunk.Chunk}.
	 * @param chunkHeight             The amount of vertical sections of the {@link net.minecraft.world.World} {@code + 2}.
	 * @param chunkBottomYPosition    The bottom section coordinate of the {@link net.minecraft.world.World} {@code - 1}.
	 * @param chunkSkyLightProvider   The {@link ChunkLightProvider} for sky light.
	 * @param chunkBlockLightProvider The {@link ChunkLightProvider} for block light.
	 * @param skyLightBits            The existing sky light {@link BitSet}.
	 * @param blockLightBits          The existing block light {@link BitSet}.
	 */
	public LightUpdateS2CPacketBuilder(@NotNull ChunkPos chunkPosition, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider, @Nullable BitSet skyLightBits, @Nullable BitSet blockLightBits) {
		this.chunkPositionX = chunkPosition.x;
		this.chunkPositionZ = chunkPosition.z;
		this.lightData = new ServerWorldLightData(
				chunkPosition, chunkHeight, chunkBottomYPosition, chunkSkyLightProvider,
				chunkBlockLightProvider, skyLightBits, blockLightBits
		);
	}

	/**
	 * @return A {@link LightUpdateS2CPacket}, built from the data provided to the {@link LightUpdateS2CPacketBuilder}.
	 */
	public @NotNull LightUpdateS2CPacket build() {
		@NotNull var buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeVarInt(chunkPositionX);
		buf.writeVarInt(chunkPositionZ);
		lightData.write(buf);
		return new LightUpdateS2CPacket(buf);
	}
}
