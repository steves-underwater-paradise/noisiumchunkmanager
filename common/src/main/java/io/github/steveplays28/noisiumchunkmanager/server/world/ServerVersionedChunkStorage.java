package io.github.steveplays28.noisiumchunkmanager.experimental.server.world;

import com.mojang.datafixers.DataFixer;
import net.minecraft.world.storage.VersionedChunkStorage;

import java.nio.file.Path;

public class ServerVersionedChunkStorage extends VersionedChunkStorage {
	public ServerVersionedChunkStorage(Path directory, DataFixer dataFixer, boolean dsync) {
		super(directory, dataFixer, dsync);
	}
}
