package io.github.steveplays28.noisiumchunkmanager.mixin.server.network;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.SpawnLocating;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SpawnLocating.class)
public class SpawnLocatingMixin {
	// TODO: Change to an @Inject at HEAD
	/**
	 * @author Steveplays28
	 * @reason
	 */
	@Overwrite
	public static @Nullable BlockPos findOverworldSpawn(@NotNull ServerWorld serverWorld, int searchBlockPosX, int searchBlockPosZ) {
		var noisiumServerWorldChunkManager = ((io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.server.ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(
				ChunkSectionPos.getSectionCoord(searchBlockPosX), ChunkSectionPos.getSectionCoord(searchBlockPosZ));
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			return null;
		}

		var chunk = ((io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.server.ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager().getChunk(chunkPosition);
		int i = serverWorld.getDimension().hasCeiling() ? serverWorld.getChunkManager().getChunkGenerator().getSpawnHeight(
				serverWorld) : chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, searchBlockPosX & 15, searchBlockPosZ & 15);
		if (i < serverWorld.getBottomY()) {
			return null;
		}

		int j = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, searchBlockPosX & 15, searchBlockPosZ & 15);
		if (j <= i && j > chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, searchBlockPosX & 15, searchBlockPosZ & 15)) {
			return null;
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int k = i + 1; k >= serverWorld.getBottomY(); --k) {
			mutable.set(searchBlockPosX, k, searchBlockPosZ);
			BlockState blockState = serverWorld.getBlockState(mutable);
			if (!blockState.getFluidState().isEmpty()) {
				break;
			}

			if (Block.isFaceFullSquare(blockState.getCollisionShape(serverWorld, mutable), Direction.UP)) {
				return mutable.up().toImmutable();
			}
		}

		return null;
	}
}
