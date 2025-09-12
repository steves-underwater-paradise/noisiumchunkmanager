package io.github.steveplays28.noisiumchunkmanager.mixin.block;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.MultifaceSpreader;

@Mixin(MultifaceSpreader.class)
public class LichenGrowerGrowCheckerMixin {
	@Inject(method = "spreadToFace", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelPlaceIfChunkIsUnloaded(@NotNull LevelAccessor world, @NotNull MultifaceSpreader.SpreadPos growPosition, boolean markForPostProcessing, @NotNull CallbackInfoReturnable<Optional<MultifaceSpreader.SpreadPos>> cir) {
		@NotNull final var growBlockPosition = growPosition.pos();
		if (!world.hasChunk(SectionPos.blockToSectionCoord(growBlockPosition.getX()), SectionPos.blockToSectionCoord(growBlockPosition.getZ()))) {
			cir.setReturnValue(Optional.empty());
		}
	}
}
