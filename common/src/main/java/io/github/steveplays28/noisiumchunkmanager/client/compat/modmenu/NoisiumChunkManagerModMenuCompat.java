package io.github.steveplays28.noisiumchunkmanager.client.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.steveplays28.noisiumchunkmanager.config.NoisiumChunkManagerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class NoisiumChunkManagerModMenuCompat implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> NoisiumChunkManagerConfig.HANDLER.generateGui().generateScreen(parent);
	}
}
