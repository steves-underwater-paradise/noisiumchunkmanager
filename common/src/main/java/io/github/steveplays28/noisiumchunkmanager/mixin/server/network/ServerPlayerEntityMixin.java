package io.github.steveplays28.noisiumchunkmanager.mixin.server.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.server.network.PlayerRespawnLogicAccessor;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player {
	@Shadow
	@Final
	public MinecraftServer server;

	@Shadow
	protected abstract int getCoprime(int horizontalSpawnArea);

	public ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;fudgeSpawnLocation(Lnet/minecraft/server/level/ServerLevel;)V"))
	private void noisiumchunkmanager$moveToSpawnWrapGetChunkAsyncWhenComplete(@NotNull ServerPlayer instance, @NotNull ServerLevel serverWorld, @NotNull Operation<Void> original) {
		((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager().getChunkAsync(
				new ChunkPos(serverWorld.getSharedSpawnPos())).whenComplete((worldChunk, throwable) -> original.call(instance, serverWorld));
	}

	/**
	 * @author Steveplays28
	 * @reason Wait for {@link net.minecraft.world.level.chunk.LevelChunk}s from the {@link ServerLevel}'s {@link ServerWorldChunkManager} asynchronously.
	 */
	@Overwrite
	private void fudgeSpawnLocation(@NotNull ServerLevel serverWorld) {
		var worldSpawnBlockPosition = serverWorld.getSharedSpawnPos();
		if (!serverWorld.dimensionType().hasSkyLight() || serverWorld.getServer().getWorldData().getGameType() == GameType.ADVENTURE) {
			this.moveTo(worldSpawnBlockPosition, 0.0F, 0.0F);

			while (!serverWorld.noCollision(this) && this.getY() < (double) (serverWorld.getMaxBuildHeight() - 1)) {
				this.setPos(this.getX(), this.getY() + 1.0, this.getZ());
			}
		}

		int worldSpawnRadius = Math.max(0, this.server.getSpawnRadius(serverWorld));
		int worldSpawnBlockPositionDistanceInsideBorder = Mth.floor(
				serverWorld.getWorldBorder().getDistanceToBorder(worldSpawnBlockPosition.getX(), worldSpawnBlockPosition.getZ()));
		if (worldSpawnBlockPositionDistanceInsideBorder < worldSpawnRadius) {
			worldSpawnRadius = worldSpawnBlockPositionDistanceInsideBorder;
		}
		if (worldSpawnBlockPositionDistanceInsideBorder <= 1) {
			worldSpawnRadius = 1;
		}

		long worldSpawnRadiusExtended = worldSpawnRadius * 2L + 1;
		long worldSpawnRadiusExtendedSquared = worldSpawnRadiusExtended * worldSpawnRadiusExtended;
		int worldSpawnRadiusLimited = worldSpawnRadiusExtendedSquared > 2147483647L ? Integer.MAX_VALUE : (int) worldSpawnRadiusExtendedSquared;
		int worldSpawnOffsetMultiplier = this.getCoprime(worldSpawnRadiusLimited);
		int randomNumberInWorldSpawnRadiusLimited = RandomSource.create().nextInt(worldSpawnRadiusLimited);

		for (int possibleWorldSpawnRadius = 0; possibleWorldSpawnRadius < worldSpawnRadiusLimited; ++possibleWorldSpawnRadius) {
			int q = (randomNumberInWorldSpawnRadiusLimited + worldSpawnOffsetMultiplier * possibleWorldSpawnRadius) % worldSpawnRadiusLimited;
			int r = q % (worldSpawnRadius * 2 + 1);
			int s = q / (worldSpawnRadius * 2 + 1);
			var searchPositionX = worldSpawnBlockPosition.getX() + r - worldSpawnRadius;
			var searchPositionZ = worldSpawnBlockPosition.getZ() + s - worldSpawnRadius;
			((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager().getChunkAsync(
					new ChunkPos(searchPositionX, searchPositionZ)).whenComplete((worldChunk, throwable) -> {
				@Nullable var overworldSpawnBlockPosition = PlayerRespawnLogicAccessor.invokeGetOverworldRespawnPos(
						serverWorld, searchPositionX, searchPositionZ);
				if (overworldSpawnBlockPosition == null) {
					return;
				}

				this.moveTo(overworldSpawnBlockPosition, 0.0F, 0.0F);
			});

			if (serverWorld.noCollision(this)) {
				break;
			}
		}
	}
}
