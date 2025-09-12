package io.github.steveplays28.noisiumchunkmanager.world.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link ServerLevel} {@link LevelChunk} with disk (IO) access.
 */
// TODO: Add disk (IO) access
public class IoWorldChunk extends LevelChunk {
	public IoWorldChunk(@NotNull Level world, @NotNull ChunkPos chunkPosition) {
		super(world, chunkPosition);
	}

	@Override
	public @Nullable TickContainerAccess<Block> getBlockTicks() {
		return null;
	}

	@Override
	public @Nullable TickContainerAccess<Fluid> getFluidTicks() {
		return null;
	}

	@Override
	public @Nullable TicksToSave getTicksForSerialization() {
		return null;
	}

	@Override
	public @NotNull CompoundTag getBlockEntityNbtForSaving(@NotNull BlockPos blockPosition) {
		@Nullable var packedBlockEntityNbt = super.getBlockEntityNbtForSaving(blockPosition);
		return packedBlockEntityNbt == null ? new CompoundTag() : packedBlockEntityNbt;
	}
}
