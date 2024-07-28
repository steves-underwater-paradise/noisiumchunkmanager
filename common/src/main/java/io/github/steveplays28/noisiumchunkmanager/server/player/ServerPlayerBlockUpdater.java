package io.github.steveplays28.noisiumchunkmanager.server.player;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.utils.value.IntValue;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.ChunkUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerPlayerBlockUpdater {
	public ServerPlayerBlockUpdater() {
		// TODO: Replace this event with an event that runs when ServerChunkManager#markForUpdate is called
		//  The mixin for that event should cancel the existing method to improve performance (it'll do nothing anyway)
		BlockEvent.PLACE.register(this::onBlockPlace);
		BlockEvent.BREAK.register(this::onBlockBreak);
	}

	private @NotNull EventResult onBlockPlace(@NotNull World world, @NotNull BlockPos blockPosition, @NotNull BlockState blockState, @Nullable Entity blockPlacer) {
		if (world.isClient()) {
			return EventResult.pass();
		}

		ChunkUtil.sendBlockUpdateToPlayers(((ServerWorld) world).getPlayers(), blockPosition, Blocks.AIR.getDefaultState());
		return EventResult.pass();
	}

	private @NotNull EventResult onBlockBreak(@NotNull World world, @NotNull BlockPos blockPosition, @NotNull BlockState blockState, @NotNull ServerPlayerEntity serverPlayerEntity, @Nullable IntValue xp) {
		if (world.isClient()) {
			return EventResult.pass();
		}

		ChunkUtil.sendBlockUpdateToPlayers(((ServerWorld) world).getPlayers(), blockPosition, Blocks.AIR.getDefaultState());
		return EventResult.pass();
	}
}
