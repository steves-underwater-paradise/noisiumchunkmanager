package io.github.steveplays28.noisiumchunkmanager.server.player;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class ServerPlayerBlockUpdater {
	public ServerPlayerBlockUpdater() {
		// TODO: Replace this event with an event that runs when ServerChunkManager#markForUpdate is called
		//  The mixin for that event should cancel the existing method to improve performance (it'll do nothing anyway)
		BlockEvent.BREAK.register((world, blockPos, blockState, player, xp) -> onBlockBreak(world, blockPos));
	}

	private EventResult onBlockBreak(@NotNull Level world, @NotNull BlockPos blockPos) {
		if (world.isClientSide()) {
			return EventResult.pass();
		}

		ChunkUtil.sendBlockUpdateToPlayers(((ServerLevel) world).players(), blockPos, Blocks.AIR.defaultBlockState());
		return EventResult.pass();
	}
}
