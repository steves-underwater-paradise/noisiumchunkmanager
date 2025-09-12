package io.github.steveplays28.noisiumchunkmanager.mixin.world.gen.chunk;

import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseChunkGeneratorMixin {
	@ModifyConstant(method = "applyCarvers", constant = @Constant(intValue = 8))
	private int noisiumchunkmanager$modifyCarvingChunkRadiusPositiveToFixAnInfiniteLoop(int chunkRadius) {
		return 0;
	}

	@ModifyConstant(method = "applyCarvers", constant = @Constant(intValue = -8))
	private int noisiumchunkmanager$modifyCarvingChunkRadiusNegativeToFixAnInfiniteLoop(int chunkRadius) {
		return 0;
	}
}
