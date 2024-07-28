package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.WorldChunkExtension;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.BitSet;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements WorldChunkExtension {
	@Shadow
	public abstract @NotNull World getWorld();

	@Unique
	private final BitSet noisiumchunkmanager$blockLightBits = new BitSet();
	@Unique
	private final BitSet noisiumchunkmanager$skyLightBits = new BitSet();

	@Override
	public @NotNull BitSet noisiumchunkmanager$getBlockLightBits() {
		return noisiumchunkmanager$blockLightBits;
	}

	@Override
	public @NotNull BitSet noisiumchunkmanager$getSkyLightBits() {
		return noisiumchunkmanager$skyLightBits;
	}
}
