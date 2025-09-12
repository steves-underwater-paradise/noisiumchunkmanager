package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public class ThreadedAnvilChunkStorageMixin {
	@Inject(method = "updateChunkTracking", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelSendWatchPackets(ServerPlayer player, ChunkPos pos, MutableObject<ClientboundLevelChunkWithLightPacket> packet, boolean oldWithinViewDistance, boolean newWithinViewDistance, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "hasWork", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelShutdownDelay(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
