package io.github.steveplays28.noisiumchunkmanager.server.world.entity.player;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.ChunkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public class ServerWorldPlayerChunkLoader {
	private final @NotNull ServerLevel serverWorld;
	private final @NotNull Function<ChunkPos, CompletableFuture<LevelChunk>> worldChunkLoadFunction;
	private final @NotNull Consumer<ChunkPos> worldChunkUnloadConsumer;
	private final @NotNull Supplier<Integer> serverViewDistanceSupplier;

	private final @NotNull Executor threadPoolExecutor;
	private final @NotNull Map<Integer, Vec3> previousPlayerPositions;

	public ServerWorldPlayerChunkLoader(
			@NotNull ServerLevel serverWorld,
			@NotNull BiFunction<ChunkPos, Integer, Map<ChunkPos, CompletableFuture<LevelChunk>>> worldChunksInRadiusLoadFunction,
			@NotNull Function<ChunkPos, CompletableFuture<LevelChunk>> worldChunkLoadFunction,
			@NotNull Consumer<ChunkPos> worldChunkUnloadConsumer,
			@NotNull Supplier<Integer> serverViewDistanceSupplier
	) {
		this.serverWorld = serverWorld;
		this.worldChunkLoadFunction = worldChunkLoadFunction;
		this.worldChunkUnloadConsumer = worldChunkUnloadConsumer;
		this.serverViewDistanceSupplier = serverViewDistanceSupplier;

		this.threadPoolExecutor = Executors.newFixedThreadPool(
				1, new ThreadFactoryBuilder().setNameFormat("Noisium Server Player Chunk Loader %d").build());
		this.previousPlayerPositions = new HashMap<>();

		PlayerEvent.PLAYER_JOIN.register(player -> {
			if (!player.serverLevel().equals(serverWorld)) {
				return;
			}

			@NotNull var playerBlockPosition = player.blockPosition();
			// Send new render distance center to the player asynchronously
			CompletableFuture.runAsync(() -> player.connection.send(
					new ClientboundSetChunkCacheCenterPacket(
							SectionPos.blockToSectionCoord(playerBlockPosition.getX()),
							SectionPos.blockToSectionCoord(playerBlockPosition.getZ())
					)), threadPoolExecutor);
			// Send chunks around the player to the player asynchronously
			ChunkUtil.sendWorldChunksToPlayerAsync(
					serverWorld,
					new ArrayList<>(worldChunksInRadiusLoadFunction.apply(player.chunkPosition(), serverViewDistanceSupplier.get()).values()),
					threadPoolExecutor
			);
			previousPlayerPositions.put(player.getId(), player.position());
		});
		PlayerEvent.PLAYER_QUIT.register(player -> previousPlayerPositions.remove(player.getId()));
		TickEvent.SERVER_LEVEL_POST.register(instance -> {
			if (!instance.equals(serverWorld)) {
				return;
			}

			tick();
		});
	}

	// TODO: Enable ticking/update chunk tracking in ServerEntityManager
	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void tick() {
		@NotNull var players = serverWorld.players();
		if (players.isEmpty() || previousPlayerPositions.isEmpty()) {
			return;
		}

		for (int i = 0; i < players.size(); i++) {
			@NotNull var player = players.get(i);
			@NotNull var playerBlockPos = player.blockPosition();
			@Nullable var previousPlayerPos = previousPlayerPositions.get(player.getId());
			if (previousPlayerPos == null || playerBlockPos.closerToCenterThan(previousPlayerPos, 16d)) {
				continue;
			}

			// Send world chunks that should be loaded to the player asynchronously
			@NotNull var previousPlayerChunkPositionsInServerViewDistance = ChunkUtil.getChunkPositionsAtPositionInRadius(
					new ChunkPos(
							new BlockPos(
									Math.round((float) previousPlayerPos.x()),
									Math.round((float) previousPlayerPos.y()),
									Math.round((float) previousPlayerPos.z())
							)
					), serverViewDistanceSupplier.get()
			);
			@NotNull var playerChunkPositionsInServerViewDistance = ChunkUtil.getChunkPositionsAtPositionInRadius(
					player.chunkPosition(), serverViewDistanceSupplier.get());
			@NotNull final var chunkPositionsToLoad = ChunkUtil.getChunkPositionDifferences(
					playerChunkPositionsInServerViewDistance, previousPlayerChunkPositionsInServerViewDistance);
			for (int chunkPositionsToLoadIndex = 0; chunkPositionsToLoadIndex < chunkPositionsToLoad.size(); chunkPositionsToLoadIndex++) {
				ChunkUtil.sendWorldChunkToPlayerAsync(
						serverWorld, worldChunkLoadFunction.apply(chunkPositionsToLoad.get(chunkPositionsToLoadIndex)), threadPoolExecutor);
			}

			// Send new render distance center to the player asynchronously
			CompletableFuture.runAsync(() -> player.connection.send(
					new ClientboundSetChunkCacheCenterPacket(
							SectionPos.blockToSectionCoord(playerBlockPos.getX()),
							SectionPos.blockToSectionCoord(playerBlockPos.getZ())
					)), threadPoolExecutor);

			// Unload world chunks that aren't required anymore asynchronously
			// TODO: Check if other players are still in range of these chunks
			//  Unload chunks at the end of the players fori loop instead
			@NotNull final var chunkPositionsToUnload = ChunkUtil.getChunkPositionDifferences(
					previousPlayerChunkPositionsInServerViewDistance, playerChunkPositionsInServerViewDistance);
			for (int chunkPositionsToUnloadIndex = 0; chunkPositionsToUnloadIndex < chunkPositionsToUnload.size(); chunkPositionsToUnloadIndex++) {
				worldChunkUnloadConsumer.accept(chunkPositionsToUnload.get(chunkPositionsToUnloadIndex));
			}

			previousPlayerPositions.put(player.getId(), player.position());
		}
	}
}
