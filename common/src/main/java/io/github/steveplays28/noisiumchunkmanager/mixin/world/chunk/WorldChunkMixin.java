package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.BitSet;

@Mixin(WorldChunk.class)
public class WorldChunkMixin implements io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.chunk.WorldChunkExtension {
	@Unique
	private final BitSet noisiumchunkmanager$blockLightBits = new BitSet();
	@Unique
	private final BitSet noisiumchunkmanager$skyLightBits = new BitSet();

	@Override
	public BitSet noisiumchunkmanager$getBlockLightBits() {
		return noisiumchunkmanager$blockLightBits;
	}

	@Override
	public BitSet noisiumchunkmanager$getSkyLightBits() {
		return noisiumchunkmanager$skyLightBits;
	}
}
