package io.github.steveplays28.noisiumchunkmanager.mixin.accessor.world.gen.chunk;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseChunkGenerator.class)
public interface NoiseChunkGeneratorAccessor {
	@Accessor
	public RegistryEntry<ChunkGeneratorSettings> getSettings();

	@Invoker
	public Chunk invokePopulateNoise(
		Blender blender,
		StructureAccessor structureAccessor,
		NoiseConfig noiseConfig,
		Chunk chunk,
		int minimumCellY,
		int cellHeight
	);
}
