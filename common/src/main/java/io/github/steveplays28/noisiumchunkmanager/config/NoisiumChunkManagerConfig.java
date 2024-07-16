package io.github.steveplays28.noisiumchunkmanager.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.IntField;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.steveplays28.noisiumchunkmanager.util.ModLoaderUtil;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import static io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager.MOD_ID;

public class NoisiumChunkManagerConfig {
	public static final @NotNull String JSON_5_FILE_EXTENSION = "json5";
	public static final @NotNull ConfigClassHandler<NoisiumChunkManagerConfig> HANDLER = ConfigClassHandler.createBuilder(
			NoisiumChunkManagerConfig.class).id(new Identifier(MOD_ID, "config")).serializer(
			config -> GsonConfigSerializerBuilder.create(config).setPath(
					ModLoaderUtil.getConfigDir().resolve(String.format("%s/config.%s", MOD_ID, JSON_5_FILE_EXTENSION))).setJson5(
					true).build()).build();

	private static final @NotNull String SERVER_CATEGORY = "server";
	private static final @NotNull String SERVER_WORLD_CHUNK_MANAGER_GROUP = "serverWorldChunkManager";

	@AutoGen(category = SERVER_CATEGORY, group = SERVER_WORLD_CHUNK_MANAGER_GROUP)
	@SerialEntry(comment = "The amount of threads used by a server world's chunk manager. Every world has its own chunk manager, and thus its own threads. After changing this option you MUST restart the server.")
	@IntField(min = 1, format = "%i threads")
	public int serverWorldChunkManagerThreads = 2;
	@AutoGen(category = SERVER_CATEGORY, group = SERVER_WORLD_CHUNK_MANAGER_GROUP)
	@SerialEntry(comment = "The amount of threads used by a server world's chunk manager lighting populator. Every world has its own chunk manager, and thus its own threads. After changing this option you MUST restart the server.")
	@IntField(min = 1, format = "%i threads")
	public int serverWorldChunkManagerLightingThreads = 2;
}
