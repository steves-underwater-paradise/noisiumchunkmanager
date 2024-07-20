package io.github.steveplays28.noisiumchunkmanager.mixin.server.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.server.network.SpawnLocatingAccessor;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
	@Shadow
	@Final
	public MinecraftServer server;

	@Shadow
	protected abstract int calculateSpawnOffsetMultiplier(int horizontalSpawnArea);

	public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;moveToSpawn(Lnet/minecraft/server/world/ServerWorld;)V"))
	private void noisiumchunkmanager$moveToSpawnWrapGetChunkAsyncWhenComplete(@NotNull ServerPlayerEntity instance, @NotNull ServerWorld serverWorld, @NotNull Operation<Void> original) {
		((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager().getChunkAsync(
				new ChunkPos(serverWorld.getSpawnPos())).whenComplete((worldChunk, throwable) -> original.call(instance, serverWorld));
	}

	/**
	 * @author Steveplays28
	 * @reason Wait for {@link net.minecraft.world.chunk.WorldChunk}s from the {@link ServerWorld}'s {@link ServerWorldChunkManager} asynchronously.
	 */
	@Overwrite
	private void moveToSpawn(@NotNull ServerWorld serverWorld) {
		var worldSpawnBlockPosition = serverWorld.getSpawnPos();
		if (!serverWorld.getDimension().hasSkyLight() || serverWorld.getServer().getSaveProperties().getGameMode() == GameMode.ADVENTURE) {
			this.refreshPositionAndAngles(worldSpawnBlockPosition, 0.0F, 0.0F);

			while (!serverWorld.isSpaceEmpty(this) && this.getY() < (double) (serverWorld.getTopY() - 1)) {
				this.setPosition(this.getX(), this.getY() + 1.0, this.getZ());
			}
		}

		int worldSpawnRadius = Math.max(0, this.server.getSpawnRadius(serverWorld));
		int worldSpawnBlockPositionDistanceInsideBorder = MathHelper.floor(
				serverWorld.getWorldBorder().getDistanceInsideBorder(worldSpawnBlockPosition.getX(), worldSpawnBlockPosition.getZ()));
		if (worldSpawnBlockPositionDistanceInsideBorder < worldSpawnRadius) {
			worldSpawnRadius = worldSpawnBlockPositionDistanceInsideBorder;
		}
		if (worldSpawnBlockPositionDistanceInsideBorder <= 1) {
			worldSpawnRadius = 1;
		}

		long worldSpawnRadiusExtended = worldSpawnRadius * 2L + 1;
		long worldSpawnRadiusExtendedSquared = worldSpawnRadiusExtended * worldSpawnRadiusExtended;
		int worldSpawnRadiusLimited = worldSpawnRadiusExtendedSquared > 2147483647L ? Integer.MAX_VALUE : (int) worldSpawnRadiusExtendedSquared;
		int worldSpawnOffsetMultiplier = this.calculateSpawnOffsetMultiplier(worldSpawnRadiusLimited);
		int randomNumberInWorldSpawnRadiusLimited = Random.create().nextInt(worldSpawnRadiusLimited);

		for (int possibleWorldSpawnRadius = 0; possibleWorldSpawnRadius < worldSpawnRadiusLimited; ++possibleWorldSpawnRadius) {
			int q = (randomNumberInWorldSpawnRadiusLimited + worldSpawnOffsetMultiplier * possibleWorldSpawnRadius) % worldSpawnRadiusLimited;
			int r = q % (worldSpawnRadius * 2 + 1);
			int s = q / (worldSpawnRadius * 2 + 1);
			var searchPositionX = worldSpawnBlockPosition.getX() + r - worldSpawnRadius;
			var searchPositionZ = worldSpawnBlockPosition.getZ() + s - worldSpawnRadius;
			((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager().getChunkAsync(
					new ChunkPos(searchPositionX, searchPositionZ)).whenComplete((worldChunk, throwable) -> {
				@Nullable var overworldSpawnBlockPosition = SpawnLocatingAccessor.invokeFindOverworldSpawn(
						serverWorld, searchPositionX, searchPositionZ);
				if (overworldSpawnBlockPosition == null) {
					return;
				}

				this.refreshPositionAndAngles(overworldSpawnBlockPosition, 0.0F, 0.0F);
			});

			if (serverWorld.isSpaceEmpty(this)) {
				break;
			}
		}
	}
}
