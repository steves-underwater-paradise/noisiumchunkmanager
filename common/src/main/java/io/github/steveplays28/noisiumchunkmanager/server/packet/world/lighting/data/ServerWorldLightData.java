package io.github.steveplays28.noisiumchunkmanager.server.packet.world.lighting.data;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class ServerWorldLightData {
	private final @NotNull BitSet initializedSkyLight;
	private final @NotNull BitSet initializedBlockLight;
	private final @NotNull BitSet uninitializedSkyLight;
	private final @NotNull BitSet uninitializedBlockLight;
	private final @NotNull List<byte[]> skyLightNibbles;
	private final @NotNull List<byte[]> blockLightNibbles;

	public ServerWorldLightData(@NotNull ChunkPos chunkPosition, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider, @Nullable BitSet skyLightBits, @Nullable BitSet blockLightBits) {
		this.initializedSkyLight = new BitSet();
		this.initializedBlockLight = new BitSet();
		this.uninitializedSkyLight = new BitSet();
		this.uninitializedBlockLight = new BitSet();
		this.skyLightNibbles = new ArrayList<>();
		this.blockLightNibbles = new ArrayList<>();

		for (int positionY = 0; positionY < chunkHeight; ++positionY) {
			if (skyLightBits == null || skyLightBits.get(positionY)) {
				this.addChunkLightDataToNibbles(
						chunkSkyLightProvider.getLightSection(ChunkSectionPos.from(chunkPosition, chunkBottomYPosition + positionY)),
						positionY, this.initializedSkyLight, this.uninitializedSkyLight, this.skyLightNibbles
				);
			}

			if (blockLightBits == null || blockLightBits.get(positionY)) {
				this.addChunkLightDataToNibbles(
						chunkBlockLightProvider.getLightSection(ChunkSectionPos.from(chunkPosition, chunkBottomYPosition + positionY)),
						positionY, this.initializedBlockLight, this.uninitializedBlockLight, this.blockLightNibbles
				);
			}
		}
	}

	public void write(@NotNull PacketByteBuf buf) {
		buf.writeBitSet(this.initializedSkyLight);
		buf.writeBitSet(this.initializedBlockLight);
		buf.writeBitSet(this.uninitializedSkyLight);
		buf.writeBitSet(this.uninitializedBlockLight);
		buf.writeCollection(this.skyLightNibbles, PacketByteBuf::writeByteArray);
		buf.writeCollection(this.blockLightNibbles, PacketByteBuf::writeByteArray);
	}

	private void addChunkLightDataToNibbles(@Nullable ChunkNibbleArray chunkLightNibbleArray, int positionY, @NotNull BitSet initializedLight, @NotNull BitSet uninitializedLight, @NotNull List<byte[]> lightNibbles) {
		if (chunkLightNibbleArray == null) {
			return;
		}
		if (chunkLightNibbleArray.isUninitialized()) {
			uninitializedLight.set(positionY);
			return;
		}

		initializedLight.set(positionY);
		lightNibbles.add(chunkLightNibbleArray.copy().asByteArray());
	}
}
