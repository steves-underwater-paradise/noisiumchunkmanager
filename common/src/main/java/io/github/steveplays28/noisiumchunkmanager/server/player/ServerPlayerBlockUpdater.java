package io.github.steveplays28.noisiumchunkmanager.server.player;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.ChunkUtil;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ServerPlayerBlockUpdater {
	public ServerPlayerBlockUpdater() {
		// TODO: Replace this event with an event that runs when ServerChunkManager#markForUpdate is called
		//  The mixin for that event should cancel the existing method to improve performance (it'll do nothing anyway)
		BlockEvent.BREAK.register((world, blockPos, blockState, player, xp) -> onBlockBreak(world, blockPos));
	}

	private EventResult onBlockBreak(@NotNull World world, @NotNull BlockPos blockPos) {
		if (world.isClient()) {
			return EventResult.pass();
		}

		ChunkUtil.sendBlockUpdateToPlayers(((ServerWorld) world).getPlayers(), blockPos, Blocks.AIR.getDefaultState());
		return EventResult.pass();
	}
}
