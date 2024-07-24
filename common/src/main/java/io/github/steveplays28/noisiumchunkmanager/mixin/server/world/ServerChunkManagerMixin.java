package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import com.mojang.datafixers.DataFixer;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.ticket.ServerWorldTicketEvent;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.ServerTickEvent;
import io.github.steveplays28.noisiumchunkmanager.util.networking.packet.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkStatusChangeListener;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.storage.NbtScannable;
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

/**
 * {@link Mixin} into {@link ServerChunkManager}.
 * This {@link Mixin} redirects all method calls from the {@link ServerWorld}'s {@link ServerChunkManager} to the {@link ServerWorld}'s {@link ServerWorldChunkManager}.
 */
@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin {
	@Shadow
	@Final
	@Mutable
	public @Nullable ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

	@Shadow
	@Final
	@NotNull ServerWorld world;
	@Shadow
	@Final
	@Mutable
	@Nullable ServerLightingProvider lightingProvider;

	@Shadow
	public abstract World getWorld();

	@Shadow
	public abstract @NotNull ChunkGenerator getChunkGenerator();

	@Shadow
	public abstract @NotNull NoiseConfig getNoiseConfig();

	@Unique
	private ChunkGenerator noisiumchunkmanager$chunkGenerator;
	@Unique
	private StructurePlacementCalculator noisiumchunkmanager$structurePlacementCalculator;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$constructorInject(ServerWorld world, LevelStorage.Session session, DataFixer dataFixer, StructureTemplateManager structureTemplateManager, Executor workerExecutor, @NotNull ChunkGenerator chunkGenerator, int viewDistance, int simulationDistance, boolean dsync, WorldGenerationProgressListener worldGenerationProgressListener, ChunkStatusChangeListener chunkStatusChangeListener, Supplier<PersistentStateManager> persistentStateManagerFactory, CallbackInfo ci) {
		noisiumchunkmanager$chunkGenerator = chunkGenerator;
		noisiumchunkmanager$structurePlacementCalculator = this.getChunkGenerator().createStructurePlacementCalculator(
				this.getWorld().getRegistryManager().getWrapperOrThrow(RegistryKeys.STRUCTURE_SET), this.getNoiseConfig(),
				((ServerWorld) this.getWorld()).getSeed()
		);
		this.lightingProvider = null;
		this.threadedAnvilChunkStorage = null;
	}

	@Inject(method = "executeQueuedTasks", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$stopServerChunkManagerFromRunningTasks(@NotNull CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}

	@Inject(method = "tick(Ljava/util/function/BooleanSupplier;Z)V", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$stopServerChunkManagerFromTicking(@NotNull BooleanSupplier shouldKeepTicking, boolean tickChunks, @NotNull CallbackInfo ci) {
		ServerTickEvent.SERVER_ENTITY_MOVEMENT_TICK.invoker().onServerEntityMovementTick();
		ci.cancel();
	}

	@Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;close()V", shift = At.Shift.BEFORE), cancellable = true)
	private void noisiumchunkmanager$cancelRemoveThreadedAnvilChunkStorageClose(@NotNull CallbackInfo ci) {
		ci.cancel();
	}

	// TODO: Fix infinite loop
	@Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerWorldChunkManager(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<Chunk> cir) {
		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this.getWorld()).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(chunkX, chunkZ);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition));
	}

	@Inject(method = "getChunk(II)Lnet/minecraft/world/chunk/light/LightSourceView;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerWorldChunkManager(int chunkX, int chunkZ, CallbackInfoReturnable<WorldChunk> cir) {
		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this.getWorld()).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(chunkX, chunkZ);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition));
	}

	@Inject(method = "getWorldChunk", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getWorldChunkFromNoisiumServerWorldChunkManager(int chunkX, int chunkZ, CallbackInfoReturnable<WorldChunk> cir) {
		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this.getWorld()).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(chunkX, chunkZ);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition));
	}

	// TODO: Don't send this packet to players out of range, to save on bandwidth
	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Inject(method = "sendToNearbyPlayers", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$sendToNearbyPlayersViaNoisiumServerWorldChunkManager(Entity entity, Packet<?> packet, CallbackInfo ci) {
		var server = entity.getServer();
		if (server == null) {
			return;
		}

		var players = server.getPlayerManager().getPlayerList();
		for (int i = 0; i < players.size(); i++) {
			players.get(i).networkHandler.sendPacket(packet);
		}

		ci.cancel();
	}

	// TODO: Don't send this packet to players out of range, to save on bandwidth
	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Inject(method = "sendToOtherNearbyPlayers", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$sendToOtherNearbyPlayersViaNoisiumServerWorldChunkManager(Entity entity, Packet<?> packet, CallbackInfo ci) {
		var server = entity.getServer();
		if (server == null) {
			return;
		}

		var players = server.getPlayerManager().getPlayerList();
		for (int i = 0; i < players.size(); i++) {
			var player = players.get(i);
			if (player.equals(entity)) {
				continue;
			}

			player.networkHandler.sendPacket(packet);
		}

		ci.cancel();
	}

	@Inject(method = {"loadEntity", "unloadEntity"}, at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelEntityLoadingAndUnloading(Entity entity, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "updatePosition", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelPlayerPositionUpdating(ServerPlayerEntity player, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "markForUpdate", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$markForUpdateViaNoisiumServerWorldChunkManager(BlockPos blockPos, CallbackInfo ci) {
		// TODO: Optimise using a pending update queue and ChunkDeltaUpdateS2CPacket
		// TODO: Implement block entity update packet sending
		var serverWorld = (ServerWorld) this.getWorld();
		PacketUtil.sendPacketToPlayers(serverWorld.getPlayers(), new BlockUpdateS2CPacket(blockPos, serverWorld.getBlockState(blockPos)));
		ci.cancel();
	}

	@Inject(method = "onLightUpdate", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$updateLightingViaNoisiumServerWorldChunkManager(LightType lightType, ChunkSectionPos chunkSectionPos, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "save", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelSave(boolean flush, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "isChunkLoaded", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$isChunkLoadedInNoisiumServerWorldChunkManager(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(((ServerWorldExtension) this.getWorld()).noisiumchunkmanager$getServerWorldChunkManager().isChunkLoaded(
				new ChunkPos(chunkX, chunkZ)));
	}

	@Inject(method = "getStructurePlacementCalculator", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getStructurePlacementCalculatorFromServerChunkManager(CallbackInfoReturnable<StructurePlacementCalculator> cir) {
		cir.setReturnValue(noisiumchunkmanager$structurePlacementCalculator);
	}

	@Inject(method = "getChunkGenerator", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkGeneratorFromServerChunkManager(@NotNull CallbackInfoReturnable<ChunkGenerator> cir) {
		cir.setReturnValue(noisiumchunkmanager$chunkGenerator);
	}

	@Inject(method = "getNoiseConfig", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getNoiseConfigFromServerChunkManager(@NotNull CallbackInfoReturnable<NoiseConfig> cir) {
		cir.setReturnValue(((ServerWorldExtension) this.getWorld()).noisiumchunkmanager$getNoiseConfig());
	}

	@Inject(method = "getChunkIoWorker", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkIoWorkerFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<NbtScannable> cir) {
		cir.setReturnValue(((ServerWorldExtension) this.getWorld()).noisiumchunkmanager$getServerWorldChunkManager().getChunkIoWorker());
	}

	@Inject(method = {"getLoadedChunkCount", "getTotalChunksLoadedCount"}, at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getTotalChunksLoadedCountFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<Integer> cir) {
		// TODO: Remove the method call for 441 start chunks, replace the 2 method calls for client/server chunk count debugging with an event listener and remove this mixin injection
		cir.setReturnValue(0);
	}

	@Inject(method = {"applyViewDistance", "applySimulationDistance"}, at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelApplyViewAndSimulationDistance(int distance, @NotNull CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "addTicket", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$invokeTicketCreatedEvent(@NotNull ChunkTicketType<?> ticketType, @NotNull ChunkPos chunkPosition, int radius, Object argument, @NotNull CallbackInfo ci) {
		ServerWorldTicketEvent.TICKET_CREATED.invoker().onTicketCreated(this.world, ticketType, chunkPosition, radius);
		ci.cancel();
	}

	@Inject(method = "removeTicket", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$invokeTicketRemovedEvent(@NotNull ChunkTicketType<?> ticketType, @NotNull ChunkPos chunkPosition, int radius, Object argument, @NotNull CallbackInfo ci) {
		ServerWorldTicketEvent.TICKET_REMOVED.invoker().onTicketRemoved(this.world, chunkPosition);
		ci.cancel();
	}
}
