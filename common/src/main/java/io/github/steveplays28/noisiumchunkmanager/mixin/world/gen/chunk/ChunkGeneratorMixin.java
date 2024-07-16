package io.github.steveplays28.noisiumchunkmanager.mixin.world.gen.chunk;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	@Shadow
	@Final
	private Supplier<List<PlacedFeatureIndexer.IndexedFeatures>> indexedFeaturesListSupplier;

	@Shadow
	private static BlockBox getBlockBoxForChunk(Chunk chunk) {
		throw new RuntimeException();
	}

	@Shadow
	@Final
	private Function<RegistryEntry<Biome>, GenerationSettings> generationSettingsGetter;

	@Shadow
	public abstract BiomeSource getBiomeSource();

	/**
	 * Replaces {@link ChunkGenerator#addStructureReferences} with a simpler one, that only checks the center chunk, instead of iterating outwards.
	 * This fixes an infinite loop with {@link io.github.steveplays28.noisiumchunkmanager.experimental.server.world.ServerWorldChunkManager}.
	 */
	@Inject(method = "addStructureReferences", at = @At(value = "HEAD"), cancellable = true)
	public void noisiumchunkmanager$replaceAddStructureReferencesToFixAnInfiniteLoop(StructureWorldAccess world, StructureAccessor structureAccessor, Chunk chunk, CallbackInfo ci) {
		var chunkPos = chunk.getPos();
		int chunkPosStartX = chunkPos.getStartX();
		int chunkPosStartZ = chunkPos.getStartZ();
		var chunkSectionPos = ChunkSectionPos.from(chunk);
		var chunkPosLong = chunkPos.toLong();

		for (StructureStart structureStart : chunk.getStructureStarts().values()) {
			try {
				if (structureStart.hasChildren() && structureStart.getBoundingBox().intersectsXZ(
						chunkPosStartX, chunkPosStartZ, chunkPosStartX + 15, chunkPosStartZ + 15)
				) {
					structureAccessor.addStructureReference(chunkSectionPos, structureStart.getStructure(), chunkPosLong, chunk);
					DebugInfoSender.sendStructureStart(world, structureStart);
				}
			} catch (Exception e) {
				CrashReport crashReport = CrashReport.create(e, "Generating structure reference");
				CrashReportSection crashReportSection = crashReport.addElement("Structure");
				crashReportSection.add(
						"Id",
						() -> world.getRegistryManager().getOptional(RegistryKeys.STRUCTURE).map(
								structureTypeRegistry -> {
									var structureId = structureTypeRegistry.getId(structureStart.getStructure());
									if (structureId == null) {
										return "UNKNOWN";
									}

									return structureId.toString();
								}
						).orElse("UNKNOWN")
				);
				crashReportSection.add(
						"Name",
						() -> {
							var structureTypeId = Registries.STRUCTURE_TYPE.getId(structureStart.getStructure().getType());
							if (structureTypeId == null) {
								return "UNKNOWN";
							}

							return structureTypeId.toString();
						}
				);
				crashReportSection.add("Class", () -> structureStart.getStructure().getClass().getCanonicalName());
				throw new CrashException(crashReport);
			}
		}

		ci.cancel();
	}

	/**
	 * @author Steveplays28
	 * @reason TODO
	 */
	@Overwrite
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
		ChunkPos chunkPos = chunk.getPos();
		if (SharedConstants.isOutsideGenerationArea(chunkPos)) {
			return;
		}

		ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunkPos, world.getBottomSectionCoord());
		BlockPos blockPos = chunkSectionPos.getMinPos();
		Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
		Map<Integer, List<Structure>> map = registry.stream().collect(
				Collectors.groupingBy((structureType) -> structureType.getFeatureGenerationStep().ordinal()));
		List<PlacedFeatureIndexer.IndexedFeatures> indexedFeatures = this.indexedFeaturesListSupplier.get();
		ChunkRandom chunkRandom = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
		long l = chunkRandom.setPopulationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
		Set<RegistryEntry<Biome>> biomeRegistryEntries = new ObjectArraySet<>();
		for (ChunkSection chunkSection : chunk.getSectionArray()) {
			chunkSection.getBiomeContainer().forEachValue(biomeRegistryEntries::add);
		}
		biomeRegistryEntries.retainAll(this.getBiomeSource().getBiomes());
		int indexedFeaturesSize = indexedFeatures.size();

		try {
			Registry<PlacedFeature> placedFeatureRegistry = world.getRegistryManager().get(RegistryKeys.PLACED_FEATURE);
			int j = Math.max(GenerationStep.Feature.values().length, indexedFeaturesSize);

			for (int indexedFeatureIndex = 0; indexedFeatureIndex < j; ++indexedFeatureIndex) {
				int m = 0;
				CrashReportSection crashReportSection;
				if (structureAccessor.shouldGenerateStructures()) {
					List<Structure> list2 = map.getOrDefault(indexedFeatureIndex, Collections.emptyList());

					for (var biomeRegistryEntriesIterator = list2.iterator(); biomeRegistryEntriesIterator.hasNext(); ++m) {
						Structure structure = biomeRegistryEntriesIterator.next();
						chunkRandom.setDecoratorSeed(l, m, indexedFeatureIndex);
						Supplier<String> currentlyGeneratingStructureNameSupplier = () -> {
							var placedFeatureNameOptional = registry.getKey(structure).map(Object::toString);
							Objects.requireNonNull(structure);
							return placedFeatureNameOptional.orElseGet(structure::toString);
						};

						try {
							world.setCurrentlyGeneratingStructureName(currentlyGeneratingStructureNameSupplier);
							//noinspection DataFlowIssue
							structureAccessor.getStructureStarts(chunkSectionPos, structure).forEach((start) -> start.place(
									world, structureAccessor, (ChunkGenerator) (Object) this, chunkRandom,
									getBlockBoxForChunk(chunk), chunkPos
							));
						} catch (Exception e) {
							CrashReport crashReport = CrashReport.create(e, "Feature placement");
							crashReportSection = crashReport.addElement("Feature");
							Objects.requireNonNull(currentlyGeneratingStructureNameSupplier);
							crashReportSection.add("Description", currentlyGeneratingStructureNameSupplier::get);
							throw new CrashException(crashReport);
						}
					}
				}

				if (indexedFeatureIndex < indexedFeaturesSize) {
					IntSet placedFeatureIndexMappings = new IntArraySet();

					for (RegistryEntry<Biome> biomeRegistryEntry : biomeRegistryEntries) {
						List<RegistryEntryList<PlacedFeature>> placedFeatureRegistryEntries = this.generationSettingsGetter.apply(
								biomeRegistryEntry).getFeatures();
						if (indexedFeatureIndex < placedFeatureRegistryEntries.size()) {
							RegistryEntryList<PlacedFeature> registryEntryList = placedFeatureRegistryEntries.get(indexedFeatureIndex);
							PlacedFeatureIndexer.IndexedFeatures indexedFeature = indexedFeatures.get(indexedFeatureIndex);
							registryEntryList.stream().map(RegistryEntry::value).forEach(
									(placedFeature) -> placedFeatureIndexMappings.add(
											indexedFeature.indexMapping().applyAsInt(placedFeature)));
						}
					}

					int placedFeatureIndexMappingsSize = placedFeatureIndexMappings.size();
					int[] placedFeatureIndexMappingsArray = placedFeatureIndexMappings.toIntArray();
					Arrays.sort(placedFeatureIndexMappingsArray);
					PlacedFeatureIndexer.IndexedFeatures indexedFeature = indexedFeatures.get(indexedFeatureIndex);

					for (int o = 0; o < placedFeatureIndexMappingsSize; ++o) {
						int p = placedFeatureIndexMappingsArray[o];
						PlacedFeature placedFeature = indexedFeature.features().get(p);
						Supplier<String> currentlyGeneratingStructureNameSupplier = () -> {
							var placedFeatureNameOptional = placedFeatureRegistry.getKey(placedFeature).map(Object::toString);
							Objects.requireNonNull(placedFeature);
							return placedFeatureNameOptional.orElseGet(placedFeature::toString);
						};
						chunkRandom.setDecoratorSeed(l, p, indexedFeatureIndex);

						try {
							world.setCurrentlyGeneratingStructureName(currentlyGeneratingStructureNameSupplier);
							//noinspection DataFlowIssue
							placedFeature.generate(world, (ChunkGenerator) (Object) this, chunkRandom, blockPos);
						} catch (Exception e) {
							CrashReport crashReport = CrashReport.create(e, "Feature placement");
							crashReportSection = crashReport.addElement("Feature");
							Objects.requireNonNull(currentlyGeneratingStructureNameSupplier);
							crashReportSection.add("Description", currentlyGeneratingStructureNameSupplier::get);
							throw new CrashException(crashReport);
						}
					}
				}
			}

			world.setCurrentlyGeneratingStructureName(null);
		} catch (Exception var31) {
			CrashReport crashReport3 = CrashReport.create(var31, "Biome decoration");
			crashReport3.addElement("Generation").add("CenterX", chunkPos.x).add("CenterZ", chunkPos.z).add("Seed", l);
			throw new CrashException(crashReport3);
		}
	}
}
