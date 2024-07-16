package io.github.steveplays28.noisiumchunkmanager.world.chunk;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.BasicTickScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link ServerWorld} {@link WorldChunk} with disk (IO) access.
 */
// TODO: Add disk (IO) access
public class IoWorldChunk extends WorldChunk {
	public IoWorldChunk(@NotNull World world, @NotNull ChunkPos chunkPosition) {
		super(world, chunkPosition);
	}

	@Override
	public @Nullable BasicTickScheduler<Block> getBlockTickScheduler() {
		return null;
	}

	@Override
	public @Nullable BasicTickScheduler<Fluid> getFluidTickScheduler() {
		return null;
	}

	@Override
	public @Nullable TickSchedulers getTickSchedulers() {
		return null;
	}

	@Override
	public @NotNull NbtCompound getPackedBlockEntityNbt(@NotNull BlockPos blockPosition) {
		@Nullable var packedBlockEntityNbt = super.getPackedBlockEntityNbt(blockPosition);
		return packedBlockEntityNbt == null ? new NbtCompound() : packedBlockEntityNbt;
	}
}
