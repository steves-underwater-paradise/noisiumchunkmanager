package io.github.steveplays28.noisiumchunkmanager.util.world.chunk;

import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.packet.world.lighting.ChunkDataS2CPacketBuilder;
import io.github.steveplays28.noisiumchunkmanager.server.packet.world.lighting.LightUpdateS2CPacketBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ChunkUtil {
	/**
	 * Sends a {@link WorldChunk} to all players in the specified world.
	 * WARNING: This method blocks the server thread. Prefer using {@link ChunkUtil#sendWorldChunkToPlayerAsync} instead.
	 *
	 * @param serverWorld             The world the {@link WorldChunk} resides in.
	 * @param worldChunk              The {@link WorldChunk}.
	 * @param chunkHeight             The amount of vertical sections of the {@link net.minecraft.world.World} {@code + 2}.
	 * @param chunkBottomYPosition    The bottom section coordinate of the {@link net.minecraft.world.World} {@code - 1}.
	 * @param chunkSkyLightProvider   The {@link ChunkLightProvider} for sky light.
	 * @param chunkBlockLightProvider The {@link ChunkLightProvider} for block light.
	 */
	public static void sendWorldChunkToPlayer(@NotNull ServerWorld serverWorld, @NotNull WorldChunk worldChunk, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider) {
		try {
			for (int i = 0; i < serverWorld.getPlayers().size(); i++) {
				serverWorld.getPlayers().get(i).sendChunkPacket(worldChunk.getPos(), new ChunkDataS2CPacketBuilder(
						worldChunk, chunkHeight, chunkBottomYPosition, chunkSkyLightProvider,
						chunkBlockLightProvider, null, null
				).build());
			}
		} catch (CrashException e) {
			NoisiumChunkManager.LOGGER.error(
					"Exception thrown while trying to send a chunk packet to all players in a server world:\n{}",
					ExceptionUtils.getStackTrace(e)
			);
		}
	}

	/**
	 * Sends a {@link WorldChunk} to all players in the specified world.
	 * This method is ran asynchronously.
	 *
	 * @param serverWorld             The world the {@link WorldChunk} resides in.
	 * @param worldChunkFuture        The {@link CompletableFuture<WorldChunk>}.
	 * @param chunkHeight             The amount of vertical sections of the {@link net.minecraft.world.World} {@code + 2}.
	 * @param chunkBottomYPosition    The bottom section coordinate of the {@link net.minecraft.world.World} {@code - 1}.
	 * @param chunkSkyLightProvider   The {@link ChunkLightProvider} for sky light.
	 * @param chunkBlockLightProvider The {@link ChunkLightProvider} for block light.
	 */
	public static void sendWorldChunkToPlayerAsync(@NotNull ServerWorld serverWorld, @NotNull CompletableFuture<WorldChunk> worldChunkFuture, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider, @NotNull Executor executor) {
		worldChunkFuture.whenCompleteAsync(
				(worldChunk, throwable) -> sendWorldChunkToPlayer(serverWorld, worldChunk, chunkHeight, chunkBottomYPosition,
						chunkSkyLightProvider, chunkBlockLightProvider
				), executor);
	}

	/**
	 * Sends a {@link List} of {@link WorldChunk}s to all players in the specified world.
	 * WARNING: This method blocks the server thread. Prefer using {@link ChunkUtil#sendWorldChunksToPlayerAsync} instead.
	 *
	 * @param serverWorld             The world the {@link WorldChunk} resides in.
	 * @param worldChunks             The {@link List} of {@link WorldChunk}s.
	 * @param chunkHeight             The amount of vertical sections of the {@link net.minecraft.world.World} {@code + 2}.
	 * @param chunkBottomYPosition    The bottom section coordinate of the {@link net.minecraft.world.World} {@code - 1}.
	 * @param chunkSkyLightProvider   The {@link ChunkLightProvider} for sky light.
	 * @param chunkBlockLightProvider The {@link ChunkLightProvider} for block light.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendWorldChunksToPlayer(@NotNull ServerWorld serverWorld, @NotNull List<WorldChunk> worldChunks, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider) {
		// TODO: Send a whole batch of chunks to the player at once to save on network traffic
		for (int i = 0; i < worldChunks.size(); i++) {
			sendWorldChunkToPlayer(
					serverWorld, worldChunks.get(i), chunkHeight, chunkBottomYPosition, chunkSkyLightProvider, chunkBlockLightProvider);
		}
	}

	/**
	 * Sends a {@link List} of {@link CompletableFuture<WorldChunk>}s to all players in the specified world.
	 * This method is ran asynchronously.
	 *
	 * @param serverWorld             The world the {@link WorldChunk} resides in.
	 * @param worldChunkFutures       The {@link List} of {@link CompletableFuture<WorldChunk>}s.
	 * @param chunkHeight             The amount of vertical sections of the {@link net.minecraft.world.World} {@code + 2}.
	 * @param chunkBottomYPosition    The bottom section coordinate of the {@link net.minecraft.world.World} {@code - 1}.
	 * @param chunkSkyLightProvider   The {@link ChunkLightProvider} for sky light.
	 * @param chunkBlockLightProvider The {@link ChunkLightProvider} for block light.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendWorldChunksToPlayerAsync(@NotNull ServerWorld serverWorld, @NotNull List<CompletableFuture<WorldChunk>> worldChunkFutures, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider, @NotNull Executor executor) {
		// TODO: Send a whole batch of chunks to the player at once to save on network traffic
		for (int i = 0; i < worldChunkFutures.size(); i++) {
			worldChunkFutures.get(i).whenCompleteAsync(
					(worldChunk, throwable) -> sendWorldChunkToPlayer(serverWorld, worldChunk, chunkHeight, chunkBottomYPosition,
							chunkSkyLightProvider, chunkBlockLightProvider
					), executor);
		}
	}

	/**
	 * Sends a light update to a {@link List} of players.
	 *
	 * @param players                 The {@link List} of players.
	 * @param chunkPosition           The {@link ChunkPos} of the {@link net.minecraft.world.chunk.Chunk}.
	 * @param chunkHeight             The amount of vertical sections of the {@link net.minecraft.world.World} {@code + 2}.
	 * @param chunkBottomYPosition    The bottom section coordinate of the {@link net.minecraft.world.World} {@code - 1}.
	 * @param chunkSkyLightProvider   The {@link ChunkLightProvider} for sky light.
	 * @param chunkBlockLightProvider The {@link ChunkLightProvider} for block light.
	 * @param skyLightBits            The existing sky light {@link BitSet}.
	 * @param blockLightBits          The existing block light {@link BitSet}.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendLightUpdateToPlayers(@NotNull List<ServerPlayerEntity> players, @NotNull ChunkPos chunkPosition, int chunkHeight, int chunkBottomYPosition, @NotNull ChunkLightProvider<?, ?> chunkSkyLightProvider, @NotNull ChunkLightProvider<?, ?> chunkBlockLightProvider, @NotNull BitSet skyLightBits, @NotNull BitSet blockLightBits) {
		for (int i = 0; i < players.size(); i++) {
			players.get(i).networkHandler.sendPacket(
					new LightUpdateS2CPacketBuilder(
							chunkPosition, chunkHeight, chunkBottomYPosition, chunkSkyLightProvider,
							chunkBlockLightProvider, skyLightBits, blockLightBits
					).build()
			);
		}
	}

	/**
	 * Sends a block update to a {@link List} of players.
	 *
	 * @param players    The {@link List} of players.
	 * @param blockPos   The {@link BlockPos} of the block update that should be sent to the {@link List} of players.
	 * @param blockState The {@link BlockState} at the specified {@link BlockPos} of the block update that should be sent to the {@link List} of players.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendBlockUpdateToPlayers(@NotNull List<ServerPlayerEntity> players, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
		for (int i = 0; i < players.size(); i++) {
			players.get(i).networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, blockState));
		}
	}

	public static @NotNull List<ChunkPos> getChunkPositionsAtPositionInRadius(@NotNull ChunkPos chunkPosition, int radius) {
		@NotNull final List<ChunkPos> chunkPositions = new ArrayList<>();
		for (int chunkPositionX = chunkPosition.x - radius; chunkPositionX < chunkPosition.x + radius; chunkPositionX++) {
			for (int chunkPositionZ = chunkPosition.z - radius; chunkPositionZ < chunkPosition.z + radius; chunkPositionZ++) {
				chunkPositions.add(new ChunkPos(chunkPositionX, chunkPositionZ));
			}
		}

		return chunkPositions;
	}

	/**
	 * @param chunkPositions      A {@link List} of {@link ChunkPos}s.
	 * @param otherChunkPositions Another {@link List} of {@link ChunkPos}s.
	 * @return The {@link ChunkPos}s that are in {@code chunkPositions}, but not in {@code otherChunkPositions}.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static @NotNull List<ChunkPos> getChunkPositionDifferences(@NotNull List<ChunkPos> chunkPositions, @NotNull List<ChunkPos> otherChunkPositions) {
		@NotNull final List<ChunkPos> chunkPositionDifferences = new ArrayList<>(chunkPositions);
		for (int i = 0; i < otherChunkPositions.size(); i++) {
			chunkPositionDifferences.remove(otherChunkPositions.get(i));
		}

		return chunkPositionDifferences;
	}
}
