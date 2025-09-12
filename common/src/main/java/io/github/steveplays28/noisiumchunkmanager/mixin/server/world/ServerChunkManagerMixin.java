package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import com.mojang.datafixers.DataFixer;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.ticket.ServerWorldTicketEvent;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.ServerTickEvent;
import io.github.steveplays28.noisiumchunkmanager.util.networking.packet.PacketUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;

/**
 * {@link Mixin} into {@link ServerChunkCache}.
 * This {@link Mixin} redirects all method calls from the {@link ServerLevel}'s {@link ServerChunkCache} to the {@link ServerLevel}'s {@link ServerWorldChunkManager}.
 */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkManagerMixin {
	@Shadow
	public abstract Level getLevel();

	@Mutable
	@Shadow
	@Final
	public @Nullable ChunkMap chunkMap;

	@Shadow
	public abstract @NotNull ChunkGenerator getGenerator();

	@Shadow
	public abstract @NotNull RandomState randomState();

	@Shadow
	@Final
	@NotNull ServerLevel level;

	@Unique
	private ChunkGenerator noisiumchunkmanager$chunkGenerator;
	@Unique
	private ChunkGeneratorStructureState noisiumchunkmanager$structurePlacementCalculator;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$constructorInject(ServerLevel world, LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer, StructureTemplateManager structureTemplateManager, Executor workerExecutor, @NotNull ChunkGenerator chunkGenerator, int viewDistance, int simulationDistance, boolean dsync, ChunkProgressListener worldGenerationProgressListener, ChunkStatusUpdateListener chunkStatusChangeListener, Supplier<DimensionDataStorage> persistentStateManagerFactory, CallbackInfo ci) {
		noisiumchunkmanager$chunkGenerator = chunkGenerator;
		noisiumchunkmanager$structurePlacementCalculator = this.getGenerator().createState(
				this.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET), this.randomState(),
				((ServerLevel) this.getLevel()).getSeed()
		);
		chunkMap = null;
	}

	@Inject(method = "pollTask", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$stopServerChunkManagerFromRunningTasks(@NotNull CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}

	@Inject(method = "tick(Ljava/util/function/BooleanSupplier;Z)V", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$stopServerChunkManagerFromTicking(@NotNull BooleanSupplier shouldKeepTicking, boolean tickChunks, @NotNull CallbackInfo ci) {
		ServerTickEvent.SERVER_ENTITY_MOVEMENT_TICK.invoker().onServerEntityMovementTick();
		ci.cancel();
	}

	@Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;close()V", shift = At.Shift.BEFORE), cancellable = true)
	private void noisiumchunkmanager$cancelRemoveThreadedAnvilChunkStorageClose(@NotNull CallbackInfo ci) {
		ci.cancel();
	}

	// TODO: Fix infinite loop
	@Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerWorldChunkManager(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this.getLevel()).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(chunkX, chunkZ);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition));
	}

	@Inject(method = "getChunkForLighting(II)Lnet/minecraft/world/level/chunk/LightChunk;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerWorldChunkManager(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this.getLevel()).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(chunkX, chunkZ);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition));
	}

	@Inject(method = "getChunkNow", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getWorldChunkFromNoisiumServerWorldChunkManager(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this.getLevel()).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(chunkX, chunkZ);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition));
	}

	// TODO: Don't send this packet to players out of range, to save on bandwidth
	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Inject(method = "broadcastAndSend", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$sendToNearbyPlayersViaNoisiumServerWorldChunkManager(Entity entity, Packet<?> packet, CallbackInfo ci) {
		var server = entity.getServer();
		if (server == null) {
			return;
		}

		var players = server.getPlayerList().getPlayers();
		for (int i = 0; i < players.size(); i++) {
			players.get(i).connection.send(packet);
		}

		ci.cancel();
	}

	// TODO: Don't send this packet to players out of range, to save on bandwidth
	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Inject(method = "broadcast", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$sendToOtherNearbyPlayersViaNoisiumServerWorldChunkManager(Entity entity, Packet<?> packet, CallbackInfo ci) {
		var server = entity.getServer();
		if (server == null) {
			return;
		}

		var players = server.getPlayerList().getPlayers();
		for (int i = 0; i < players.size(); i++) {
			var player = players.get(i);
			if (player.equals(entity)) {
				continue;
			}

			player.connection.send(packet);
		}

		ci.cancel();
	}

	@Inject(method = {"addEntity", "removeEntity"}, at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelEntityLoadingAndUnloading(Entity entity, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "move", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelPlayerPositionUpdating(ServerPlayer player, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "blockChanged", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$markForUpdateViaNoisiumServerWorldChunkManager(BlockPos blockPos, CallbackInfo ci) {
		// TODO: Optimise using a pending update queue and ChunkDeltaUpdateS2CPacket
		// TODO: Implement block entity update packet sending
		var serverWorld = (ServerLevel) this.getLevel();
		PacketUtil.sendPacketToPlayers(serverWorld.players(), new ClientboundBlockUpdatePacket(blockPos, serverWorld.getBlockState(blockPos)));
		ci.cancel();
	}

	@Inject(method = "onLightUpdate", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$updateLightingViaNoisiumServerWorldChunkManager(LightLayer lightType, SectionPos chunkSectionPos, CallbackInfo ci) {
		ServerChunkEvent.LIGHT_UPDATE.invoker().onLightUpdate(lightType, chunkSectionPos);
		ci.cancel();
	}

	@Inject(method = "save", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelSave(boolean flush, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "hasChunk", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$isChunkLoadedInNoisiumServerWorldChunkManager(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(((ServerWorldExtension) this.getLevel()).noisiumchunkmanager$getServerWorldChunkManager().isChunkLoaded(
				new ChunkPos(chunkX, chunkZ)));
	}

	@Inject(method = "getGeneratorState", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getStructurePlacementCalculatorFromServerChunkManager(CallbackInfoReturnable<ChunkGeneratorStructureState> cir) {
		cir.setReturnValue(noisiumchunkmanager$structurePlacementCalculator);
	}

	@Inject(method = "getGenerator", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkGeneratorFromServerChunkManager(@NotNull CallbackInfoReturnable<ChunkGenerator> cir) {
		cir.setReturnValue(noisiumchunkmanager$chunkGenerator);
	}

	@Inject(method = "randomState", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getNoiseConfigFromServerChunkManager(@NotNull CallbackInfoReturnable<RandomState> cir) {
		cir.setReturnValue(((ServerWorldExtension) this.getLevel()).noisiumchunkmanager$getNoiseConfig());
	}

	@Inject(method = "chunkScanner", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkIoWorkerFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<ChunkScanAccess> cir) {
		cir.setReturnValue(((ServerWorldExtension) this.getLevel()).noisiumchunkmanager$getServerWorldChunkManager().getChunkIoWorker());
	}

	@Inject(method = {"getLoadedChunksCount", "getTickingGenerated"}, at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getTotalChunksLoadedCountFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<Integer> cir) {
		// TODO: Remove the method call for 441 start chunks, replace the 2 method calls for client/server chunk count debugging with an event listener and remove this mixin injection
		cir.setReturnValue(0);
	}

	@Inject(method = {"setViewDistance", "setSimulationDistance"}, at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelApplyViewAndSimulationDistance(int distance, @NotNull CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "addRegionTicket", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$invokeTicketCreatedEvent(@NotNull TicketType<?> ticketType, @NotNull ChunkPos chunkPosition, int radius, Object argument, @NotNull CallbackInfo ci) {
		ServerWorldTicketEvent.TICKET_CREATED.invoker().onTicketCreated(this.level, ticketType, chunkPosition, radius);
		ci.cancel();
	}

	@Inject(method = "removeRegionTicket", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$invokeTicketRemovedEvent(@NotNull TicketType<?> ticketType, @NotNull ChunkPos chunkPosition, int radius, Object argument, @NotNull CallbackInfo ci) {
		ServerWorldTicketEvent.TICKET_REMOVED.invoker().onTicketRemoved(this.level, chunkPosition);
		ci.cancel();
	}
}
