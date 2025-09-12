package io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
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
		 * Invoked after a {@link LevelChunk} has been loaded by {@link ServerWorldChunkManager}, either via world generation or from save data.
		 *
		 * @param serverWorld The {@link ServerLevel} of the loaded {@link LevelChunk}.
		 * @param worldChunk  The loaded {@link LevelChunk}.
		 */
		void onWorldChunkLoaded(@NotNull ServerLevel serverWorld, @NotNull LevelChunk worldChunk);
	}

	@FunctionalInterface
	interface WorldChunkUnloaded {
		/**
		 * Invoked after a {@link LevelChunk} has been unloaded by {@link ServerWorldChunkManager}.
		 *
		 * @param serverWorld        The {@link ServerLevel} of the unloaded {@link LevelChunk}.
		 * @param worldChunkPosition The {@link ChunkPos} of the unloaded {@link LevelChunk}.
		 */
		void onWorldChunkUnloaded(@NotNull ServerLevel serverWorld, @NotNull ChunkPos worldChunkPosition);
	}

	@FunctionalInterface
	interface LightUpdate {
		/**
		 * Invoked before a {@link LevelChunk} has had a light update processed by {@link ServerWorldChunkManager}.
		 *
		 * @param lightType            The {@link LightLayer} of the {@link LevelChunk}.
		 * @param chunkSectionPosition The {@link SectionPos} of the {@link LevelChunk}.
		 */
		void onLightUpdate(@NotNull LightLayer lightType, @NotNull SectionPos chunkSectionPosition);
	}

	@FunctionalInterface
	interface BlockChange {
		/**
		 * Invoked before a {@link LevelChunk} has had a block change processed by {@link ServerWorldChunkManager}.
		 *
		 * @param blockPos      The {@link BlockPos} where the block change has happened.
		 * @param oldBlockState The old {@link BlockState} at the {@link BlockPos}.
		 * @param newBlockState The new {@link BlockState} at the {@link BlockPos}.
		 */
		void onBlockChange(@NotNull BlockPos blockPos, @NotNull BlockState oldBlockState, @NotNull BlockState newBlockState);
	}
}
