package io.github.steveplays28.noisiumchunkmanager.mixin.accessor.world.gen.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseBasedChunkGenerator.class)
public interface NoiseChunkGeneratorAccessor {
	@Accessor
	public Holder<NoiseGeneratorSettings> getSettings();

	@Invoker
	public ChunkAccess invokeDoFill(
		Blender blender,
		StructureManager structureAccessor,
		RandomState noiseConfig,
		ChunkAccess chunk,
		int minimumCellY,
		int cellHeight
	);
}
