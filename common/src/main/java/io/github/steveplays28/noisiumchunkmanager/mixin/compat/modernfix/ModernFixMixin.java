package io.github.steveplays28.noisiumchunkmanager.mixin.compat.modernfix;

import net.minecraft.server.MinecraftServer;
import org.embeddedt.modernfix.ModernFix;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModernFix.class)
public class ModernFixMixin {
	/**
	 * Prevents ModernFix from trying to access {@link net.minecraft.server.world.ThreadedAnvilChunkStorage}.
	 *
	 * @param server The Minecraft server.
	 * @param ci     The {@link CallbackInfo} for this mixin injection.
	 */
	@Inject(method = "onServerDead", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelThreadedAnvilChunkStorageClear(@NotNull MinecraftServer server, @NotNull CallbackInfo ci) {
		ci.cancel();
	}
}
