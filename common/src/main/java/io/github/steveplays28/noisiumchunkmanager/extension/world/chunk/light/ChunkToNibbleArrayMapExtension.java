package io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.light;

import net.minecraft.world.chunk.ChunkNibbleArray;

import java.util.Map;

public interface ChunkToNibbleArrayMapExtension {
	Map<Long, ChunkNibbleArray> noisiumchunkmanager$getArrays();
}
