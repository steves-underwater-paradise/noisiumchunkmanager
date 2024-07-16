package io.github.steveplays28.noisiumchunkmanager;

import io.github.steveplays28.noisiumchunkmanager.server.NoisiumChunkManagerServerInitialiser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoisiumChunkManager {
	public static final String MOD_ID = "noisiumchunkmanager";
	public static final String MOD_NAME = "Noisium Chunk Manager";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static void initialize() {
		LOGGER.info("Loading {}.", MOD_NAME);

		NoisiumChunkManagerServerInitialiser.initialise();
	}
}
