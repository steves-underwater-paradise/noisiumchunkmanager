package io.github.steveplays28.noisiumchunkmanager.mixin.entity;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;broadcast(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/Packet;)V"))
	private void noisiumchunkmanager$sendBlockUpdatePacketToAllPlayersInEntityWorld(@Nullable ChunkMap instance, @NotNull Entity entity, @NotNull Packet<?> packet) {
		@NotNull var world = entity.level();
		if (world.isClientSide()) {
			return;
		}

		((ServerWorldExtension) world).noisiumchunkmanager$sendPacketToNearbyPlayers(packet);
	}
}
