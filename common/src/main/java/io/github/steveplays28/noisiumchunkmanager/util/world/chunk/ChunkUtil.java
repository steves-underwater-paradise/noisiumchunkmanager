package io.github.steveplays28.noisiumchunkmanager.util.world.chunk;

import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
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
	 * WARNING: This method blocks the server thread. Prefer using {@link ChunkUtil#sendWorldChunkToPlayerAsync(ServerWorld, CompletableFuture, Executor)} instead.
	 *
	 * @param serverWorld The world the {@link WorldChunk} resides in.
	 * @param worldChunk  The {@link WorldChunk}.
	 */
	public static void sendWorldChunkToPlayer(@NotNull ServerWorld serverWorld, @NotNull WorldChunk worldChunk) {
		try {
			var chunkDataS2CPacket = new ChunkDataS2CPacket(worldChunk, serverWorld.getLightingProvider(), null, null);
			for (int i = 0; i < serverWorld.getPlayers().size(); i++) {
				serverWorld.getPlayers().get(i).sendChunkPacket(worldChunk.getPos(), chunkDataS2CPacket);
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
	 * @param serverWorld      The world the {@link WorldChunk} resides in.
	 * @param worldChunkFuture The {@link CompletableFuture<WorldChunk>}.
	 */
	public static void sendWorldChunkToPlayerAsync(@NotNull ServerWorld serverWorld, @NotNull CompletableFuture<WorldChunk> worldChunkFuture, @NotNull Executor executor) {
		worldChunkFuture.whenCompleteAsync((worldChunk, throwable) -> sendWorldChunkToPlayer(serverWorld, worldChunk), executor);
	}

	/**
	 * Sends a {@link List} of {@link WorldChunk}s to all players in the specified world.
	 * WARNING: This method blocks the server thread. Prefer using {@link ChunkUtil#sendWorldChunksToPlayerAsync(ServerWorld, List, Executor)} instead.
	 *
	 * @param serverWorld The world the {@link WorldChunk} resides in.
	 * @param worldChunks The {@link List} of {@link WorldChunk}s.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendWorldChunksToPlayer(@NotNull ServerWorld serverWorld, @NotNull List<WorldChunk> worldChunks) {
		// TODO: Send a whole batch of chunks to the player at once to save on network traffic
		for (int i = 0; i < worldChunks.size(); i++) {
			sendWorldChunkToPlayer(serverWorld, worldChunks.get(i));
		}
	}

	/**
	 * Sends a {@link List} of {@link CompletableFuture<WorldChunk>}s to all players in the specified world.
	 * This method is ran asynchronously.
	 *
	 * @param serverWorld       The world the {@link WorldChunk} resides in.
	 * @param worldChunkFutures The {@link List} of {@link CompletableFuture<WorldChunk>}s
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendWorldChunksToPlayerAsync(@NotNull ServerWorld serverWorld, @NotNull List<CompletableFuture<WorldChunk>> worldChunkFutures, @NotNull Executor executor) {
		// TODO: Send a whole batch of chunks to the player at once to save on network traffic
		for (int i = 0; i < worldChunkFutures.size(); i++) {
			worldChunkFutures.get(i).whenCompleteAsync(
					(worldChunk, throwable) -> sendWorldChunkToPlayer(serverWorld, worldChunk), executor);
		}
	}

	/**
	 * Sends a light update to a {@link List} of players.
	 *
	 * @param players          The {@link List} of players.
	 * @param lightingProvider The {@link LightingProvider} of the world.
	 * @param chunkPos         The {@link ChunkPos} at which the light update happened.
	 * @param skyLightBits     The skylight {@link BitSet}.
	 * @param blockLightBits   The blocklight {@link BitSet}.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendLightUpdateToPlayers(@NotNull List<ServerPlayerEntity> players, @NotNull LightingProvider lightingProvider, @NotNull ChunkPos chunkPos, @NotNull BitSet skyLightBits, @NotNull BitSet blockLightBits) {
		for (int i = 0; i < players.size(); i++) {
			players.get(i).networkHandler.sendPacket(new LightUpdateS2CPacket(chunkPos, lightingProvider, skyLightBits, blockLightBits));
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
