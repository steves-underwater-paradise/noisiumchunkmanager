package io.github.steveplays28.noisiumchunkmanager.fabric;

import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import net.fabricmc.api.ModInitializer;

public class NoisiumFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NoisiumChunkManager.initialize();
    }
}
