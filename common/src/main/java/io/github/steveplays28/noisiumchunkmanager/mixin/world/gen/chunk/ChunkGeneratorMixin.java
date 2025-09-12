package io.github.steveplays28.noisiumchunkmanager.mixin.world.gen.chunk;

import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
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
	private Supplier<List<FeatureSorter.StepFeatureData>> featuresPerStep;

	@Shadow
	private static BoundingBox getWritableArea(ChunkAccess chunk) {
		throw new RuntimeException();
	}

	@Shadow
	@Final
	private Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter;

	@Shadow
	public abstract BiomeSource getBiomeSource();

	/**
	 * Replaces {@link ChunkGenerator#createReferences} with a simpler one, that only checks the center chunk, instead of iterating outwards.
	 * This fixes an infinite loop with {@link ServerWorldChunkManager}.
	 */
	@Inject(method = "createReferences", at = @At(value = "HEAD"), cancellable = true)
	public void noisiumchunkmanager$replaceAddStructureReferencesToFixAnInfiniteLoop(WorldGenLevel world, StructureManager structureAccessor, ChunkAccess chunk, CallbackInfo ci) {
		var chunkPos = chunk.getPos();
		int chunkPosStartX = chunkPos.getMinBlockX();
		int chunkPosStartZ = chunkPos.getMinBlockZ();
		var chunkSectionPos = SectionPos.bottomOf(chunk);
		var chunkPosLong = chunkPos.toLong();

		for (StructureStart structureStart : chunk.getAllStarts().values()) {
			try {
				if (structureStart.isValid() && structureStart.getBoundingBox().intersects(
						chunkPosStartX, chunkPosStartZ, chunkPosStartX + 15, chunkPosStartZ + 15)
				) {
					structureAccessor.addReferenceForStructure(chunkSectionPos, structureStart.getStructure(), chunkPosLong, chunk);
					DebugPackets.sendStructurePacket(world, structureStart);
				}
			} catch (Exception e) {
				CrashReport crashReport = CrashReport.forThrowable(e, "Generating structure reference");
				CrashReportCategory crashReportSection = crashReport.addCategory("Structure");
				crashReportSection.setDetail(
						"Id",
						() -> world.registryAccess().registry(Registries.STRUCTURE).map(
								structureTypeRegistry -> {
									var structureId = structureTypeRegistry.getKey(structureStart.getStructure());
									if (structureId == null) {
										return "UNKNOWN";
									}

									return structureId.toString();
								}
						).orElse("UNKNOWN")
				);
				crashReportSection.setDetail(
						"Name",
						() -> {
							var structureTypeId = BuiltInRegistries.STRUCTURE_TYPE.getKey(structureStart.getStructure().type());
							if (structureTypeId == null) {
								return "UNKNOWN";
							}

							return structureTypeId.toString();
						}
				);
				crashReportSection.setDetail("Class", () -> structureStart.getStructure().getClass().getCanonicalName());
				throw new ReportedException(crashReport);
			}
		}

		ci.cancel();
	}

	/**
	 * @author Steveplays28
	 * @reason TODO
	 */
	@Overwrite
	public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {
		ChunkPos chunkPos = chunk.getPos();
		if (SharedConstants.debugVoidTerrain(chunkPos)) {
			return;
		}

		SectionPos chunkSectionPos = SectionPos.of(chunkPos, world.getMinSection());
		BlockPos blockPos = chunkSectionPos.origin();
		Registry<Structure> registry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
		Map<Integer, List<Structure>> map = registry.stream().collect(
				Collectors.groupingBy((structureType) -> structureType.step().ordinal()));
		List<FeatureSorter.StepFeatureData> indexedFeatures = this.featuresPerStep.get();
		WorldgenRandom chunkRandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
		long l = chunkRandom.setDecorationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
		Set<Holder<Biome>> biomeRegistryEntries = new ObjectArraySet<>();
		for (LevelChunkSection chunkSection : chunk.getSections()) {
			chunkSection.getBiomes().getAll(biomeRegistryEntries::add);
		}
		biomeRegistryEntries.retainAll(this.getBiomeSource().possibleBiomes());
		int indexedFeaturesSize = indexedFeatures.size();

		try {
			Registry<PlacedFeature> placedFeatureRegistry = world.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
			int j = Math.max(GenerationStep.Decoration.values().length, indexedFeaturesSize);

			for (int indexedFeatureIndex = 0; indexedFeatureIndex < j; ++indexedFeatureIndex) {
				int m = 0;
				CrashReportCategory crashReportSection;
				if (structureAccessor.shouldGenerateStructures()) {
					List<Structure> list2 = map.getOrDefault(indexedFeatureIndex, Collections.emptyList());

					for (var biomeRegistryEntriesIterator = list2.iterator(); biomeRegistryEntriesIterator.hasNext(); ++m) {
						Structure structure = biomeRegistryEntriesIterator.next();
						chunkRandom.setFeatureSeed(l, m, indexedFeatureIndex);
						Supplier<String> currentlyGeneratingStructureNameSupplier = () -> {
							var placedFeatureNameOptional = registry.getResourceKey(structure).map(Object::toString);
							Objects.requireNonNull(structure);
							return placedFeatureNameOptional.orElseGet(structure::toString);
						};

						try {
							world.setCurrentlyGenerating(currentlyGeneratingStructureNameSupplier);
							//noinspection DataFlowIssue
							structureAccessor.startsForStructure(chunkSectionPos, structure).forEach((start) -> start.placeInChunk(
									world, structureAccessor, (ChunkGenerator) (Object) this, chunkRandom,
									getWritableArea(chunk), chunkPos
							));
						} catch (Exception e) {
							CrashReport crashReport = CrashReport.forThrowable(e, "Feature placement");
							crashReportSection = crashReport.addCategory("Feature");
							Objects.requireNonNull(currentlyGeneratingStructureNameSupplier);
							crashReportSection.setDetail("Description", currentlyGeneratingStructureNameSupplier::get);
							throw new ReportedException(crashReport);
						}
					}
				}

				if (indexedFeatureIndex < indexedFeaturesSize) {
					IntSet placedFeatureIndexMappings = new IntArraySet();

					for (Holder<Biome> biomeRegistryEntry : biomeRegistryEntries) {
						List<HolderSet<PlacedFeature>> placedFeatureRegistryEntries = this.generationSettingsGetter.apply(
								biomeRegistryEntry).features();
						if (indexedFeatureIndex < placedFeatureRegistryEntries.size()) {
							HolderSet<PlacedFeature> registryEntryList = placedFeatureRegistryEntries.get(indexedFeatureIndex);
							FeatureSorter.StepFeatureData indexedFeature = indexedFeatures.get(indexedFeatureIndex);
							registryEntryList.stream().map(Holder::value).forEach(
									(placedFeature) -> placedFeatureIndexMappings.add(
											indexedFeature.indexMapping().applyAsInt(placedFeature)));
						}
					}

					int placedFeatureIndexMappingsSize = placedFeatureIndexMappings.size();
					int[] placedFeatureIndexMappingsArray = placedFeatureIndexMappings.toIntArray();
					Arrays.sort(placedFeatureIndexMappingsArray);
					FeatureSorter.StepFeatureData indexedFeature = indexedFeatures.get(indexedFeatureIndex);

					for (int o = 0; o < placedFeatureIndexMappingsSize; ++o) {
						int p = placedFeatureIndexMappingsArray[o];
						PlacedFeature placedFeature = indexedFeature.features().get(p);
						Supplier<String> currentlyGeneratingStructureNameSupplier = () -> {
							var placedFeatureNameOptional = placedFeatureRegistry.getResourceKey(placedFeature).map(Object::toString);
							Objects.requireNonNull(placedFeature);
							return placedFeatureNameOptional.orElseGet(placedFeature::toString);
						};
						chunkRandom.setFeatureSeed(l, p, indexedFeatureIndex);

						try {
							world.setCurrentlyGenerating(currentlyGeneratingStructureNameSupplier);
							//noinspection DataFlowIssue
							placedFeature.placeWithBiomeCheck(world, (ChunkGenerator) (Object) this, chunkRandom, blockPos);
						} catch (Exception e) {
							CrashReport crashReport = CrashReport.forThrowable(e, "Feature placement");
							crashReportSection = crashReport.addCategory("Feature");
							Objects.requireNonNull(currentlyGeneratingStructureNameSupplier);
							crashReportSection.setDetail("Description", currentlyGeneratingStructureNameSupplier::get);
							throw new ReportedException(crashReport);
						}
					}
				}
			}

			world.setCurrentlyGenerating(null);
		} catch (Exception var31) {
			CrashReport crashReport3 = CrashReport.forThrowable(var31, "Biome decoration");
			crashReport3.addCategory("Generation").setDetail("CenterX", chunkPos.x).setDetail("CenterZ", chunkPos.z).setDetail("Seed", l);
			throw new ReportedException(crashReport3);
		}
	}
}
