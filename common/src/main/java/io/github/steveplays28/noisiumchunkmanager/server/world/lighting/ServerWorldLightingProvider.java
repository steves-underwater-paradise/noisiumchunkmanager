package io.github.steveplays28.noisiumchunkmanager.server.world.lighting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.architectury.event.events.common.TickEvent;
import io.github.steveplays28.noisiumchunkmanager.config.NoisiumChunkManagerConfig;
import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.WorldChunkExtension;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.ChunkUtil;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A lighting provider for {@link ServerWorld}s.
 * This class cannot extend {@link ServerLightingProvider} or {@link LightingProvider} due to the superclass being hard to change, causing {@link ServerWorldLightingProvider} to require a lot of workarounds to function.
 */
public class ServerWorldLightingProvider implements LightingView {
	private final @NotNull ServerWorld serverWorld;
	private final @NotNull Executor threadPoolExecutor;
	private final @NotNull ChunkBlockLightProvider blockLightProvider;
	private final @NotNull ChunkSkyLightProvider skyLightProvider;

	public ServerWorldLightingProvider(@NotNull ServerWorld serverWorld) {
		this.serverWorld = serverWorld;

		this.threadPoolExecutor = Executors.newFixedThreadPool(
				NoisiumChunkManagerConfig.HANDLER.instance().serverWorldChunkManagerLightingThreads,
				new ThreadFactoryBuilder().setNameFormat(
						"Noisium Server World Lighting Provider " + serverWorld.getDimension().effects() + " %d").build()
		);

		@NotNull var chunkProvider = new ChunkProvider() {
			@Override
			public @Nullable LightSourceView getChunk(int chunkX, int chunkZ) {
				return serverWorld.getChunk(chunkX, chunkZ);
			}

			@Override
			public BlockView getWorld() {
				return serverWorld;
			}

			@Override
			public void onLightUpdate(@NotNull LightType lightType, @NotNull ChunkSectionPos chunkSectionPosition) {
				ServerChunkEvent.LIGHT_UPDATE.invoker().onLightUpdate(lightType, chunkSectionPosition);
			}
		};
		this.blockLightProvider = new ChunkBlockLightProvider(chunkProvider);
		this.skyLightProvider = new ChunkSkyLightProvider(chunkProvider);

		ServerChunkEvent.LIGHT_UPDATE.register(this::onLightUpdateAsync);
		TickEvent.SERVER_LEVEL_POST.register(instance -> {
			if (instance != serverWorld) {
				return;
			}

			tick();
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

	@SuppressWarnings("DuplicateBranchesInSwitch")
	public @NotNull ChunkLightProvider<?, ?> getChunkLightingView(@NotNull LightType lightType) {
		@NotNull ChunkLightProvider<?, ?> chunkLightProvider;
		switch (lightType) {
			case BLOCK -> chunkLightProvider = blockLightProvider;
			case SKY -> chunkLightProvider = skyLightProvider;
			default -> chunkLightProvider = skyLightProvider;
		}
		return chunkLightProvider;
	}

	public void setRetainLightingData(@NotNull ChunkPos chunkPosition, boolean retainLightingData) {
		blockLightProvider.setRetainColumn(chunkPosition, retainLightingData);
		skyLightProvider.setRetainColumn(chunkPosition, retainLightingData);
	}

	public void enqueueChunkSectionLightingData(@NotNull LightType lightType, @NotNull ChunkSectionPos chunkSectionPosition, @Nullable ChunkNibbleArray chunkLightingDataNibbleArray) {
		var chunkSectionPositionAsLong = chunkSectionPosition.asLong();
		switch (lightType) {
			case BLOCK -> blockLightProvider.enqueueSectionData(chunkSectionPositionAsLong, chunkLightingDataNibbleArray);
			case SKY -> skyLightProvider.enqueueSectionData(chunkSectionPositionAsLong, chunkLightingDataNibbleArray);
		}
	}

	public @NotNull CompletableFuture<Chunk> initializeLight(@NotNull Chunk chunk, boolean retainLightingData) {
		return CompletableFuture.supplyAsync(() -> {
			@NotNull var chunkPosition = chunk.getPos();
			@NotNull var chunkSections = chunk.getSectionArray();
			for (int i = 0; i < chunkSections.length; ++i) {
				@NotNull var chunkSection = chunkSections[i];
				if (chunkSection.isEmpty()) {
					continue;
				}

				@NotNull var chunkSectionPosition = ChunkSectionPos.from(chunkPosition, serverWorld.sectionIndexToCoord(i));
				enqueueChunkSectionLightingData(
						LightType.SKY, chunkSectionPosition, getChunkLightingView(LightType.SKY).getLightSection(chunkSectionPosition));
				enqueueChunkSectionLightingData(
						LightType.BLOCK, chunkSectionPosition, getChunkLightingView(LightType.BLOCK).getLightSection(chunkSectionPosition));
				setSectionStatus(chunkSectionPosition, false);
			}

			setColumnEnabled(chunkPosition, retainLightingData);
			setRetainLightingData(chunkPosition, false);
			return chunk;
		}, threadPoolExecutor);
	}

	public @NotNull CompletableFuture<Chunk> light(@NotNull Chunk chunk, boolean excludeBlocks) {
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
		int bottomY = serverWorld.getBottomY();
		var chunkSectionYPosition = chunkSectionPosition.getSectionY();
		if (chunkSectionYPosition < bottomY || chunkSectionYPosition > serverWorld.getTopY()) {
			return;
		}

		var chunkPosition = chunkSectionPosition.toChunkPos();
		((ServerWorldExtension) serverWorld).noisiumchunkmanager$getChunkAsync(chunkPosition).whenCompleteAsync((worldChunk, throwable) -> {
			var worldChunkExtension = (WorldChunkExtension) worldChunk;
			var skyLightBits = worldChunkExtension.noisiumchunkmanager$getBlockLightBits();
			var blockLightBits = worldChunkExtension.noisiumchunkmanager$getSkyLightBits();
			int chunkSectionYPositionDifference = chunkSectionYPosition - bottomY;

			skyLightBits.clear();
			blockLightBits.clear();
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
		}, threadPoolExecutor);
	}

	private void tick() {
		doLightUpdatesAsync();
	}

	private void doLightUpdatesAsync() {
		CompletableFuture.runAsync(() -> {
			blockLightProvider.doLightUpdates();
			skyLightProvider.doLightUpdates();
		}, threadPoolExecutor);
	}
}