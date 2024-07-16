package io.github.steveplays28.noisiumchunkmanager.fabric.mixin.experimental.compat.fabric.api.networking.v1;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(PlayerLookup.class)
public class PlayerLookupMixin {
	@Inject(method = "tracking(Lnet/minecraft/entity/Entity;)Ljava/util/Collection;", at = @At(value = "HEAD"), cancellable = true)
	private static void noisiumchunkmanager$returnAllPlayersInTheEntityWorld(@NotNull Entity entity, @NotNull CallbackInfoReturnable<Collection<ServerPlayerEntity>> cir) {
		if (!(entity.getWorld() instanceof @NotNull final ServerWorld serverWorld)) {
			return;
		}

		cir.setReturnValue(serverWorld.getPlayers());
	}

	@Inject(method = "tracking(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/ChunkPos;)Ljava/util/Collection;", at = @At(value = "HEAD"), cancellable = true)
	private static void noisiumchunkmanager$returnAllPlayersInTheSpecifiedWorld(@NotNull ServerWorld serverWorld, @NotNull ChunkPos chunkPos, @NotNull CallbackInfoReturnable<Collection<ServerPlayerEntity>> cir) {
		cir.setReturnValue(serverWorld.getPlayers());
	}
}
