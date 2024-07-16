package io.github.steveplays28.noisiumchunkmanager.forge;

import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import net.minecraftforge.fml.common.Mod;

@Mod(NoisiumChunkManager.MOD_ID)
public class NoisiumForge {
    public NoisiumForge() {
        NoisiumChunkManager.initialize();
    }
}
