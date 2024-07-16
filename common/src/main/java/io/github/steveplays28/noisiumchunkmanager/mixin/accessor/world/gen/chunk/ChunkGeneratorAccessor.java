package io.github.steveplays28.noisiumchunkmanager.mixin.accessor.world.gen.chunk;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccessor {
	@SuppressWarnings("UnusedReturnValue")
	@Invoker
	CompletableFuture<Chunk> invokePopulateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk);
}
