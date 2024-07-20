package io.github.steveplays28.noisiumchunkmanager.extension.world.chunk;

import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

public interface WorldChunkExtension {
	@NotNull BitSet noisiumchunkmanager$getBlockLightBits();

	@NotNull BitSet noisiumchunkmanager$getSkyLightBits();
}
