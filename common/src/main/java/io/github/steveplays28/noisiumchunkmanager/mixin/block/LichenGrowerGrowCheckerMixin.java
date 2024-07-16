package io.github.steveplays28.noisiumchunkmanager.mixin.block;

import net.minecraft.block.LichenGrower;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LichenGrower.class)
public class LichenGrowerGrowCheckerMixin {
	@Inject(method = "place", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelPlaceIfChunkIsUnloaded(@NotNull WorldAccess world, @NotNull LichenGrower.GrowPos growPosition, boolean markForPostProcessing, @NotNull CallbackInfoReturnable<Optional<LichenGrower.GrowPos>> cir) {
		@NotNull final var growBlockPosition = growPosition.pos();
		if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(growBlockPosition.getX()), ChunkSectionPos.getSectionCoord(growBlockPosition.getZ()))) {
			cir.setReturnValue(Optional.empty());
		}
	}
}
