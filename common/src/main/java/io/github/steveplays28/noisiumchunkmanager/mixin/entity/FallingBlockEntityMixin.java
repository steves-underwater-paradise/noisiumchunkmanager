package io.github.steveplays28.noisiumchunkmanager.mixin.entity;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;sendToOtherNearbyPlayers(Lnet/minecraft/entity/Entity;Lnet/minecraft/network/packet/Packet;)V"))
	private void noisiumchunkmanager$sendBlockUpdatePacketToAllPlayersInEntityWorld(@Nullable ThreadedAnvilChunkStorage instance, @NotNull Entity entity, @NotNull Packet<?> packet) {
		@NotNull var world = entity.getWorld();
		if (world.isClient()) {
			return;
		}

		((ServerWorldExtension) world).noisiumchunkmanager$sendPacketToNearbyPlayers(packet);
	}
}
