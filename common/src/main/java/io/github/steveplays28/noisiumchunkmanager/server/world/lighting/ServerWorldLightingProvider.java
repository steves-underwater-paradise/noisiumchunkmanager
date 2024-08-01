package io.github.steveplays28.noisiumchunkmanager.server.world.lighting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import io.github.steveplays28.noisiumchunkmanager.config.NoisiumChunkManagerConfig;
import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.WorldChunkExtension;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.ChunkUtil;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager.MOD_NAME;

/**
 * A lighting provider for {@link ServerWorld}s.
 */
@SuppressWarnings("DataFlowIssue")
public class ServerWorldLightingProvider extends ServerLightingProvider {
	private final @NotNull ServerWorld serverWorld;
	private final @NotNull Executor threadPoolExecutor;
	private final @NotNull List<CompletableFuture<Void>> lightChunkCompletableFutures;

	private boolean isStopping;
	private CompletableFuture<Void> lightUpdatesCompletableFuture;

	public ServerWorldLightingProvider(@NotNull ServerWorld serverWorld) {
		super(new ChunkProvider() {
			@Override
			public @Nullable LightSourceView getChunk(int chunkX, int chunkZ) {
				return serverWorld.getChunk(chunkX, chunkZ);
			}

			@Override
			public @NotNull BlockView getWorld() {
				return serverWorld;
			}

			@Override
			public void onLightUpdate(@NotNull LightType lightType, @NotNull ChunkSectionPos chunkSectionPosition) {
				ServerChunkEvent.LIGHT_UPDATE.invoker().onLightUpdate(serverWorld, lightType, chunkSectionPosition);
			}
		}, null, true, null, null);
		this.serverWorld = serverWorld;

		this.threadPoolExecutor = Executors.newFixedThreadPool(
				NoisiumChunkManagerConfig.HANDLER.instance().serverWorldChunkManagerLightingThreads,
				new ThreadFactoryBuilder().setNameFormat(
						"Noisium Server World Lighting Provider " + serverWorld.getDimension().effects() + " %d").build()
		);
		this.lightChunkCompletableFutures = new ArrayList<>();

		ServerChunkEvent.WORLD_CHUNK_LOADED.register((instance, worldChunk) -> {
			if (worldChunk.isLightOn() || instance.getPlayers().isEmpty() || isStopping) {
				return;
			}

			@NotNull var worldChunkPosition = worldChunk.getPos();
			// TODO: Change to a functional interface
			instance.getChunkManager().addTicket(ChunkTicketType.LIGHT, worldChunkPosition, 2, worldChunkPosition);
			initializeLight(worldChunk, false).thenCompose(chunkWithInitializedLighting ->
					light(chunkWithInitializedLighting, false)).whenComplete((litChunk, throwable) -> {
				if (throwable != null) {
					NoisiumChunkManager.LOGGER.error(
							"Exception thrown while lighting a chunk asynchronously:\n{}",
							ExceptionUtils.getStackTrace(ExceptionUtils.getRootCause(throwable))
					);
					return;
				}

				// TODO: Change to a functional interface
				instance.getChunkManager().removeTicket(ChunkTicketType.LIGHT, worldChunkPosition, 1, worldChunkPosition);
			});
		});
		ServerChunkEvent.LIGHT_UPDATE.register((instance, lightType, chunkSectionPosition) -> {
			if (instance != serverWorld || isStopping) {
				return;
			}

			onLightUpdateAsync(lightType, chunkSectionPosition);
		});
		TickEvent.SERVER_LEVEL_POST.register(instance -> {
			if (instance != serverWorld || instance.getPlayers().isEmpty() || isStopping) {
				return;
			}

			tick();
		});
		LifecycleEvent.SERVER_STOPPING.register(instance -> {
			this.isStopping = true;
			for (var lightChunkCompletableFuture : lightChunkCompletableFutures) {
				lightChunkCompletableFuture.cancel(true);
			}

			lightChunkCompletableFutures.clear();
		});
	}

	@Override
	public void checkBlock(BlockPos blockPosition) {
		blockLightProvider.checkBlock(blockPosition);
		skyLightProvider.checkBlock(blockPosition);
	}

	@Override
	public boolean hasUpdates() {
		return this.skyLightProvider.hasUpdates() || this.blockLightProvider.hasUpdates();
	}

	@Override
	public int doLightUpdates() {
		int i = 0;
		i += blockLightProvider.doLightUpdates();
		i += skyLightProvider.doLightUpdates();

		return i;
	}

	@Override
	public void setSectionStatus(ChunkSectionPos chunkSectionPosition, boolean notReady) {
		blockLightProvider.setSectionStatus(chunkSectionPosition, notReady);
		skyLightProvider.setSectionStatus(chunkSectionPosition, notReady);
	}

	@Override
	public void setColumnEnabled(ChunkPos chunkPosition, boolean retainLightingData) {
		blockLightProvider.setColumnEnabled(chunkPosition, retainLightingData);
		skyLightProvider.setColumnEnabled(chunkPosition, retainLightingData);
	}

	@Override
	public void propagateLight(ChunkPos chunkPosition) {
		blockLightProvider.propagateLight(chunkPosition);
		skyLightProvider.propagateLight(chunkPosition);
	}

	@Override
	public void setRetainData(@NotNull ChunkPos chunkPosition, boolean retainLightingData) {
		blockLightProvider.setRetainColumn(chunkPosition, retainLightingData);
		skyLightProvider.setRetainColumn(chunkPosition, retainLightingData);
	}

	@Override
	public void enqueueSectionData(@NotNull LightType lightType, @NotNull ChunkSectionPos chunkSectionPosition, @Nullable ChunkNibbleArray chunkLightingDataNibbleArray) {
		var chunkSectionPositionAsLong = chunkSectionPosition.asLong();
		switch (lightType) {
			case BLOCK -> blockLightProvider.enqueueSectionData(chunkSectionPositionAsLong, chunkLightingDataNibbleArray);
			case SKY -> skyLightProvider.enqueueSectionData(chunkSectionPositionAsLong, chunkLightingDataNibbleArray);
		}
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Override
	public @NotNull ChunkLightProvider<?, ?> get(@NotNull LightType lightType) {
		@NotNull ChunkLightProvider<?, ?> chunkLightProvider;
		switch (lightType) {
			case BLOCK -> chunkLightProvider = blockLightProvider;
			case SKY -> chunkLightProvider = skyLightProvider;
			default -> chunkLightProvider = skyLightProvider;
		}
		return chunkLightProvider;
	}

	@Override
	public @NotNull CompletableFuture<Chunk> initializeLight(@NotNull Chunk chunk, boolean retainLightingData) {
		if (isStopping) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					String.format("Can't initialise chunk lighting because %s Server World Lighting Provider is stopping.", MOD_NAME)));
		}

		return CompletableFuture.supplyAsync(() -> {
			@NotNull var chunkPosition = chunk.getPos();
			@NotNull var chunkSections = chunk.getSectionArray();
			for (int i = 0; i < chunkSections.length; ++i) {
				@NotNull var chunkSection = chunkSections[i];
				if (chunkSection.isEmpty()) {
					continue;
				}

				@NotNull var chunkSectionPosition = ChunkSectionPos.from(chunkPosition, serverWorld.sectionIndexToCoord(i));
				enqueueSectionData(
						LightType.SKY, chunkSectionPosition, get(LightType.SKY).getLightSection(chunkSectionPosition));
				enqueueSectionData(
						LightType.BLOCK, chunkSectionPosition, get(LightType.BLOCK).getLightSection(chunkSectionPosition));
				setSectionStatus(chunkSectionPosition, false);
			}

			setColumnEnabled(chunkPosition, retainLightingData);
			setRetainData(chunkPosition, false);
			return chunk;
		}, threadPoolExecutor);
	}

	@Override
	public @NotNull CompletableFuture<Chunk> light(@NotNull Chunk chunk, boolean excludeBlocks) {
		if (isStopping) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					String.format("Can't light chunk because %s Server World Lighting Provider is stopping.", MOD_NAME)));
		}

		return CompletableFuture.supplyAsync(() -> {
			@NotNull ChunkPos chunkPos = chunk.getPos();
			chunk.setLightOn(false);

			if (!excludeBlocks) {
				propagateLight(chunkPos);
			}

			chunk.setLightOn(true);
			return chunk;
		}, threadPoolExecutor);
	}

	/**
	 * Updates the chunk's lighting at the specified {@link ChunkSectionPos}.
	 * This method is ran asynchronously.
	 *
	 * @param lightType            The {@link LightType} that should be updated for this {@link WorldChunk}.
	 * @param chunkSectionPosition The {@link ChunkSectionPos} of the {@link WorldChunk}.
	 */
	private void onLightUpdateAsync(@NotNull LightType lightType, @NotNull ChunkSectionPos chunkSectionPosition) {
		int bottomY = serverWorld.getBottomSectionCoord() - 1;
		var chunkSectionYPosition = chunkSectionPosition.getSectionY();
		if (chunkSectionYPosition < bottomY || chunkSectionYPosition > serverWorld.countVerticalSections() + 2) {
			return;
		}

		var chunkPosition = chunkSectionPosition.toChunkPos();
		((ServerWorldExtension) serverWorld).noisiumchunkmanager$getChunkAsync(chunkPosition).whenCompleteAsync((worldChunk, throwable) -> {
			var worldChunkExtension = (WorldChunkExtension) worldChunk;
			var skyLightBits = worldChunkExtension.noisiumchunkmanager$getBlockLightBits();
			var blockLightBits = worldChunkExtension.noisiumchunkmanager$getSkyLightBits();
			int chunkSectionYPositionDifference = chunkSectionYPosition - bottomY;

			if (lightType == LightType.SKY) {
				skyLightBits.set(chunkSectionYPositionDifference);
			} else {
				blockLightBits.set(chunkSectionYPositionDifference);
			}
			ChunkUtil.sendLightUpdateToPlayers(
					serverWorld.getPlayers(), chunkPosition, serverWorld.countVerticalSections() + 2,
					serverWorld.getBottomSectionCoord() - 1, skyLightProvider, blockLightProvider,
					skyLightBits, blockLightBits
			);
			skyLightBits.clear();
			blockLightBits.clear();
		}, threadPoolExecutor);
	}

	@Override
	public void tick() {
		doLightUpdatesAsync();
	}

	private void doLightUpdatesAsync() {
		if (!hasUpdates() || (lightUpdatesCompletableFuture != null && !lightUpdatesCompletableFuture.isDone())) {
			return;
		}

		lightUpdatesCompletableFuture = CompletableFuture.runAsync(() -> {
			blockLightProvider.doLightUpdates();
			skyLightProvider.doLightUpdates();
		}, threadPoolExecutor);
	}
}
