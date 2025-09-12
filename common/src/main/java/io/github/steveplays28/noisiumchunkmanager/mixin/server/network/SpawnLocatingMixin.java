package io.github.steveplays28.noisiumchunkmanager.mixin.server.network;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PlayerRespawnLogic.class)
public class SpawnLocatingMixin {
	// TODO: Change to an @Inject at HEAD
	/**
	 * @author Steveplays28
	 * @reason TODO
	 */
	@Overwrite
	public static @Nullable BlockPos getOverworldRespawnPos(@NotNull ServerLevel serverWorld, int searchBlockPosX, int searchBlockPosZ) {
		var noisiumServerWorldChunkManager = ((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(
				SectionPos.blockToSectionCoord(searchBlockPosX), SectionPos.blockToSectionCoord(searchBlockPosZ));
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			return null;
		}

		var chunk = noisiumServerWorldChunkManager.getChunk(chunkPosition);
		int i = serverWorld.dimensionType().hasCeiling() ? serverWorld.getChunkSource().getGenerator().getSpawnHeight(
				serverWorld) : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, searchBlockPosX & 15, searchBlockPosZ & 15);
		if (i < serverWorld.getMinBuildHeight()) {
			return null;
		}

		int j = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, searchBlockPosX & 15, searchBlockPosZ & 15);
		if (j <= i && j > chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, searchBlockPosX & 15, searchBlockPosZ & 15)) {
			return null;
		}

		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int k = i + 1; k >= serverWorld.getMinBuildHeight(); --k) {
			mutable.set(searchBlockPosX, k, searchBlockPosZ);
			BlockState blockState = serverWorld.getBlockState(mutable);
			if (!blockState.getFluidState().isEmpty()) {
				break;
			}

			if (Block.isFaceFull(blockState.getCollisionShape(serverWorld, mutable), Direction.UP)) {
				return mutable.above().immutable();
			}
		}

		return null;
	}
}
