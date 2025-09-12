package io.github.steveplays28.noisiumchunkmanager.server.world.chunk.tick;

import dev.architectury.event.events.common.TickEvent;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;

public class ServerWorldChunkTicker {
	private final @NotNull ServerLevel serverWorld;
	private final @NotNull List<ChunkPos> loadedChunkPositions;

	public ServerWorldChunkTicker(@NotNull ServerLevel serverWorld) {
		this.serverWorld = serverWorld;

		this.loadedChunkPositions = new ArrayList<>();

		ServerChunkEvent.WORLD_CHUNK_LOADED.register((instance, worldChunk) -> {
			if (instance != serverWorld) {
				return;
			}

			onWorldChunkLoaded(worldChunk);
		});
		ServerChunkEvent.WORLD_CHUNK_UNLOADED.register((instance, worldChunkPosition) -> {
			if (instance != serverWorld) {
				return;
			}

			onWorldChunkUnloaded(worldChunkPosition);
		});
		TickEvent.SERVER_LEVEL_POST.register(instance -> {
			if (instance != serverWorld) {
				return;
			}

			tick();
		});
	}

	private void onWorldChunkLoaded(@NotNull LevelChunk worldChunk) {
		loadedChunkPositions.add(worldChunk.getPos());
	}

	private void onWorldChunkUnloaded(@NotNull ChunkPos worldChunkPosition) {
		loadedChunkPositions.remove(worldChunkPosition);
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void tick() {
		for (int loadedChunkPositionIndex = 0; loadedChunkPositionIndex < loadedChunkPositions.size(); loadedChunkPositionIndex++) {
			@Nullable var loadedChunkPosition = loadedChunkPositions.get(loadedChunkPositionIndex);
			if (loadedChunkPosition == null) {
				continue;
			}

			// TODO: Re-use ChunkPos via a new getChunk(ChunkPos) method in ServerWorldExtension
			serverWorld.tickChunk(
					serverWorld.getChunk(loadedChunkPosition.x, loadedChunkPosition.z),
					serverWorld.getGameRules().getInt(GameRules.RULE_RANDOMTICKING)
			);
		}
	}
}
