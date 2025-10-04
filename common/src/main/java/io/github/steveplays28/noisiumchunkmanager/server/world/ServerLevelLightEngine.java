package io.github.steveplays28.noisiumchunkmanager.server.world;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ServerLevelLightEngine extends LevelLightEngine {
	private final @NotNull ServerLevel serverLevel;

	private @Nullable ConcurrentMap<ChunkPos, BitSet> worldChunkBlockLightData;
	private @Nullable ConcurrentMap<ChunkPos, BitSet> worldChunkSkyLightData;
	private @Nullable Executor blockLightEngineThreadPoolExecutor;
	private @Nullable Executor skyLightEngineThreadPoolExecutor;
	private @Nullable Executor lightUpdateHandlerThreadPoolExecutor;

	public ServerLevelLightEngine(@NotNull ServerLevel serverLevel, @NotNull LightChunkGetter lightChunkGetter, boolean enableBlockLightEngine, boolean enableSkyLightEngine) {
		super(lightChunkGetter, enableBlockLightEngine, enableSkyLightEngine);
		this.serverLevel = serverLevel;
		if (enableBlockLightEngine) {
			this.worldChunkBlockLightData = new ConcurrentHashMap<ChunkPos, BitSet>();
			this.blockLightEngineThreadPoolExecutor = Executors.newFixedThreadPool(1,
					new ThreadFactoryBuilder().setNameFormat("Noisium Server World Chunk Manager Block Light Engine " + serverLevel.dimensionType().effectsLocation() + " %d").build());
		}
		if (enableSkyLightEngine) {
			this.worldChunkSkyLightData = new ConcurrentHashMap<ChunkPos, BitSet>();
			this.skyLightEngineThreadPoolExecutor = Executors.newFixedThreadPool(1,
					new ThreadFactoryBuilder().setNameFormat("Noisium Server World Chunk Manager Sky Light Engine " + serverLevel.dimensionType().effectsLocation() + " %d").build());
		}
		this.lightUpdateHandlerThreadPoolExecutor = Executors.newFixedThreadPool(1,
				new ThreadFactoryBuilder().setNameFormat("Noisium Server World Chunk Manager Light Update Handler " + serverLevel.dimensionType().effectsLocation() + " %d").build());

		ServerChunkEvent.LIGHT_UPDATE.register(this::onLightUpdateAsync);
	}

	@Override
	public void checkBlock(BlockPos blockPosition) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.checkBlock(blockPosition), blockLightEngineThreadPoolExecutor).join();
		}
		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.checkBlock(blockPosition), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public boolean hasLightWork() {
		return this.skyEngine != null && CompletableFuture.supplyAsync(() -> this.skyEngine.hasLightWork(), skyLightEngineThreadPoolExecutor).join() ? true
				: this.blockEngine != null && CompletableFuture.supplyAsync(() -> this.blockEngine.hasLightWork(), blockLightEngineThreadPoolExecutor).join();
	}

	@Override
	public int runLightUpdates() {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> {
				if (!this.blockEngine.hasLightWork()) {
					return;
				}

				this.blockEngine.runLightUpdates();
			}, blockLightEngineThreadPoolExecutor);
		}
		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> {
				if (!this.skyEngine.hasLightWork()) {
					return;
				}

				this.skyEngine.runLightUpdates();
			}, skyLightEngineThreadPoolExecutor);
		}
		return 0;
	}

	@Override
	public void updateSectionStatus(SectionPos sectionPosition, boolean bl) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.updateSectionStatus(sectionPosition, bl), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.updateSectionStatus(sectionPosition, bl), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public void setLightEnabled(ChunkPos chunkPosition, boolean lightEnabled) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.setLightEnabled(chunkPosition, lightEnabled), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.setLightEnabled(chunkPosition, lightEnabled), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public void propagateLightSources(ChunkPos chunkPosition) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.propagateLightSources(chunkPosition), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.propagateLightSources(chunkPosition), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public String getDebugData(LightLayer lightLayer, SectionPos sectionPosition) {
		if (lightLayer == LightLayer.BLOCK) {
			if (this.blockEngine != null) {
				return CompletableFuture.supplyAsync(() -> this.blockEngine.getDebugData(sectionPosition.asLong()), blockLightEngineThreadPoolExecutor).join();
			}
		} else if (this.skyEngine != null) {
			return CompletableFuture.supplyAsync(() -> this.skyEngine.getDebugData(sectionPosition.asLong()), skyLightEngineThreadPoolExecutor).join();
		}

		return "n/a";
	}

	@Override
	public LayerLightSectionStorage.SectionType getDebugSectionType(LightLayer lightLayer, SectionPos sectionPosition) {
		if (lightLayer == LightLayer.BLOCK) {
			if (this.blockEngine != null) {
				return CompletableFuture.supplyAsync(() -> this.blockEngine.getDebugSectionType(sectionPosition.asLong()), blockLightEngineThreadPoolExecutor).join();
			}
		} else if (this.skyEngine != null) {
			return CompletableFuture.supplyAsync(() -> this.skyEngine.getDebugSectionType(sectionPosition.asLong()), skyLightEngineThreadPoolExecutor).join();
		}

		return LayerLightSectionStorage.SectionType.EMPTY;
	}

	@Override
	public void queueSectionData(LightLayer lightLayer, SectionPos sectionPosition, @Nullable DataLayer dataLayer) {
		if (lightLayer == LightLayer.BLOCK) {
			if (this.blockEngine != null) {
				CompletableFuture.runAsync(() -> this.blockEngine.queueSectionData(sectionPosition.asLong(), dataLayer), blockLightEngineThreadPoolExecutor).join();
			}
		} else if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.queueSectionData(sectionPosition.asLong(), dataLayer), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public void retainData(ChunkPos chunkPosition, boolean retainData) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.retainData(chunkPosition, retainData), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.retainData(chunkPosition, retainData), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public int getRawBrightness(BlockPos blockPosition, int i) {
		int j = this.skyEngine == null ? 0 : CompletableFuture.supplyAsync(() -> this.skyEngine.getLightValue(blockPosition), skyLightEngineThreadPoolExecutor).join() - i;
		int k = this.blockEngine == null ? 0 : CompletableFuture.supplyAsync(() -> this.blockEngine.getLightValue(blockPosition), blockLightEngineThreadPoolExecutor).join();
		return Math.max(k, j);
	}

	@Override
	public boolean lightOnInSection(SectionPos sectionPosition) {
		long sectionPositionAsLong = sectionPosition.asLong();
		return this.blockEngine == null || CompletableFuture.supplyAsync(() -> this.blockEngine.storage.lightOnInSection(sectionPositionAsLong), blockLightEngineThreadPoolExecutor).join()
				&& (this.skyEngine == null || CompletableFuture.supplyAsync(() -> this.skyEngine.storage.lightOnInSection(sectionPositionAsLong), skyLightEngineThreadPoolExecutor).join());
	}

	public @Nullable BitSet getWorldChunkSkyLightBits(@NotNull ChunkPos chunkPosition) {
		return CompletableFuture.supplyAsync(() -> this.worldChunkSkyLightData.get(chunkPosition), lightUpdateHandlerThreadPoolExecutor).join();
	}

	public @Nullable BitSet getWorldChunkBlockLightBits(@NotNull ChunkPos chunkPosition) {
		return CompletableFuture.supplyAsync(() -> this.worldChunkBlockLightData.get(chunkPosition), lightUpdateHandlerThreadPoolExecutor).join();
	}

	/**
	 * Updates the chunk's lighting at the specified {@link SectionPos}. This method is ran asynchronously.
	 *
	 * @param lightType The {@link LightLayer} that should be updated for this {@link LevelChunk}.
	 * @param chunkSectionPosition The {@link SectionPos} of the {@link LevelChunk}.
	 */
	private void onLightUpdateAsync(@NotNull ServerLevel serverLevel, @NotNull LightLayer lightType, @NotNull SectionPos chunkSectionPosition) {
		if (!serverLevel.equals(this.serverLevel)) {
			return;
		}

		CompletableFuture.runAsync(() -> {
			var chunkSectionYPosition = chunkSectionPosition.y();
			var bottomY = this.getMinLightSection();
			if (chunkSectionYPosition < bottomY || chunkSectionYPosition > this.getMaxLightSection()) {
				return;
			}

			@NotNull var chunkPosition = chunkSectionPosition.chunk();
			NoisiumChunkManager.LOGGER.info("Light update at {}", chunkPosition);
			var chunkSectionYPositionDifference = chunkSectionYPosition - bottomY;
			if (lightType == LightLayer.SKY) {
				worldChunkSkyLightData.compute(chunkPosition, (@NotNull ChunkPos chunkPosition1, @Nullable BitSet skyLightBits) -> {
					if (skyLightBits == null) {
						skyLightBits = new BitSet();
					}

					skyLightBits.set(chunkSectionYPositionDifference);
					return skyLightBits;
				});
			} else {
				worldChunkBlockLightData.compute(chunkPosition, (@NotNull ChunkPos chunkPosition1, @Nullable BitSet blockLightBits) -> {
					if (blockLightBits == null) {
						blockLightBits = new BitSet();
					}

					blockLightBits.set(chunkSectionYPositionDifference);
					return blockLightBits;
				});
			}
			ChunkUtil.sendLightUpdateToPlayers(this.serverLevel.players(), this, chunkPosition, worldChunkSkyLightData.get(chunkPosition), worldChunkBlockLightData.get(chunkPosition));
			worldChunkSkyLightData.remove(chunkPosition);
			worldChunkBlockLightData.remove(chunkPosition);
		}, lightUpdateHandlerThreadPoolExecutor);
	}
}
