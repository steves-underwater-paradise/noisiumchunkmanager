package io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

public interface ServerChunkEvent {
	/**
	 * @see WorldChunkLoaded
	 */
	Event<WorldChunkLoaded> WORLD_CHUNK_LOADED = EventFactory.createLoop();
	/**
	 * @see WorldChunkUnloaded
	 */
	Event<WorldChunkUnloaded> WORLD_CHUNK_UNLOADED = EventFactory.createLoop();
	/**
	 * @see LightUpdate
	 */
	Event<LightUpdate> LIGHT_UPDATE = EventFactory.createLoop();
	/**
	 * @see BlockChange
	 */
	Event<BlockChange> BLOCK_CHANGE = EventFactory.createLoop();

	@FunctionalInterface
	interface WorldChunkLoaded {
		/**
		 * Invoked after a {@link WorldChunk} has been loaded by {@link ServerWorldChunkManager}, either via world generation or from save data.
		 *
		 * @param serverWorld The {@link ServerWorld} of the loaded {@link WorldChunk}.
		 * @param worldChunk  The loaded {@link WorldChunk}.
		 */
		void onWorldChunkLoaded(@NotNull ServerWorld serverWorld, @NotNull WorldChunk worldChunk);
	}

	@FunctionalInterface
	interface WorldChunkUnloaded {
		/**
		 * Invoked after a {@link WorldChunk} has been unloaded by {@link ServerWorldChunkManager}.
		 *
		 * @param serverWorld        The {@link ServerWorld} of the unloaded {@link WorldChunk}.
		 * @param worldChunkPosition The {@link ChunkPos} of the unloaded {@link WorldChunk}.
		 */
		void onWorldChunkUnloaded(@NotNull ServerWorld serverWorld, @NotNull ChunkPos worldChunkPosition);
	}

	@FunctionalInterface
	interface LightUpdate {
		/**
		 * Invoked before a {@link WorldChunk} has had a light update processed by {@link ServerWorldChunkManager}.
		 *
		 * @param lightType            The {@link LightType} of the {@link WorldChunk}.
		 * @param chunkSectionPosition The {@link ChunkSectionPos} of the {@link WorldChunk}.
		 */
		void onLightUpdate(@NotNull LightType lightType, @NotNull ChunkSectionPos chunkSectionPosition);
	}

	@FunctionalInterface
	interface BlockChange {
		/**
		 * Invoked before a {@link WorldChunk} has had a block change processed by {@link ServerWorldChunkManager}.
		 *
		 * @param blockPos      The {@link BlockPos} where the block change has happened.
		 * @param oldBlockState The old {@link BlockState} at the {@link BlockPos}.
		 * @param newBlockState The new {@link BlockState} at the {@link BlockPos}.
		 */
		void onBlockChange(@NotNull BlockPos blockPos, @NotNull BlockState oldBlockState, @NotNull BlockState newBlockState);
	}
}
