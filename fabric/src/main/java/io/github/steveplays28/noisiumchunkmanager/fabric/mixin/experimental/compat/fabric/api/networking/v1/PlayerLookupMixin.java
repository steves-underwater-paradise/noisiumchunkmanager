package io.github.steveplays28.noisiumchunkmanager.fabric.mixin.experimental.compat.fabric.api.networking.v1;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(PlayerLookup.class)
public class PlayerLookupMixin {
	@Inject(method = "tracking(Lnet/minecraft/world/entity/Entity;)Ljava/util/Collection;", at = @At(value = "HEAD"), cancellable = true)
	private static void noisiumchunkmanager$returnAllPlayersInTheEntityWorld(@NotNull Entity entity, @NotNull CallbackInfoReturnable<Collection<ServerPlayer>> cir) {
		if (!(entity.level() instanceof @NotNull final ServerLevel serverLevel)) {
			return;
		}

		cir.setReturnValue(serverLevel.players());
	}

	@Inject(method = "tracking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;)Ljava/util/Collection;", at = @At(value = "HEAD"), cancellable = true)
	private static void noisiumchunkmanager$returnAllPlayersInTheSpecifiedWorld(@NotNull ServerLevel serverLevel, @NotNull ChunkPos chunkPos, @NotNull CallbackInfoReturnable<Collection<ServerPlayer>> cir) {
		cir.setReturnValue(serverLevel.players());
	}
}
