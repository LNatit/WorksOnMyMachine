package com.lnatit.womm.pipeline;

import com.lnatit.womm.WOMM;
import com.mojang.serialization.Dynamic;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.ContentValidationException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface Pipeline
{
    static Loader prepareResources(LoadContext template) throws WorldPrepareException {
        Minecraft mc = Minecraft.getInstance();
        String worldName = template.worldName();

        LevelStorageSource.LevelStorageAccess access;
        try {
            access = mc.getLevelSource().validateAndCreateAccess(worldName);
        }
        catch (IOException exception) {
            throw new WorldPrepareException(FailCode.WORLD_ACCESS_IO, exception);
        }
        catch (ContentValidationException exception) {
            throw new WorldPrepareException(FailCode.WORLD_ACCESS_VALIDATION, exception);
        }

        PackRepository packRepository = ServerPacksSource.createPackRepository(access);
        boolean failed = true;

        try {
            Loader result;
            if (access.hasWorldData()) {
                Dynamic<?> levelDataUnfixed;
                try {
                    levelDataUnfixed = access.getUnfixedDataTagWithFallback();
                }
                catch (NbtException | ReportedNbtException | IOException exception) {
                    throw new WorldPrepareException(FailCode.WORLD_DATA_READ_FAILED, exception);
                }

                LevelSummary summary = access.fixAndGetSummaryFromTag(levelDataUnfixed);
                if (summary.requiresManualConversion()) {
                    throw new WorldPrepareException(FailCode.WORLD_REQUIRES_MANUAL_CONVERSION);
                }

                if (!summary.isCompatible()) {
                    throw new WorldPrepareException(FailCode.WORLD_INCOMPATIBLE_VERSION);
                }

                // actually Dymamic<WorldDataConfiguration>
                Dynamic<?> levelDataTag =
                        DataFixers.getFileFixer().fix(access, levelDataUnfixed, new UpgradeProgress());
                WorldLoader.PackConfig packConfig =
                        LevelStorageSource.getPackConfig(levelDataTag, packRepository, false);
                WorldStem worldStem = createStemBlocking(mc, packConfig, context -> {
                    Registry<LevelStem> datapackDimensions =
                            context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM);
                    LevelDataAndDimensions data = LevelStorageSource.getLevelDataAndDimensions(access,
                                                                                               levelDataTag,
                                                                                               context.dataConfiguration(),
                                                                                               datapackDimensions,
                                                                                               context.datapackWorldgen());
                    return new WorldLoader.DataLoadOutput<>(data.worldDataAndGenSettings(),
                                                            data.dimensions().dimensionsRegistryAccess());
                }, WorldStem::new);
                result = createLoader(access, packRepository, worldStem, Optional.empty(), false);
            }
            else {
                LevelSettings levelSettings = template.levelSettings();
                WorldDataConfiguration dataConfiguration = levelSettings.dataConfiguration();
                WorldLoader.PackConfig packConfig =
                        new WorldLoader.PackConfig(packRepository, dataConfiguration, false, false);
                WorldStem worldStem = createStemBlocking(mc, packConfig, context -> {
                    Holder<WorldPreset> presetHolder = context
                            .datapackWorldgen()
                            .lookupOrThrow(Registries.WORLD_PRESET)
                            .get(template.preset())
                            .orElseThrow(() -> new RuntimeException(new WorldPrepareException(FailCode.WORLD_PRESET_NOT_FOUND)));

                    WorldDimensions dimensions = presetHolder.value().createWorldDimensions();
                    WorldDimensions.Complete completeDimensions =
                            dimensions.bake(context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
                    return new WorldLoader.DataLoadOutput<>(new LevelDataAndDimensions.WorldDataAndGenSettings(new PrimaryLevelData(
                            levelSettings,
                            completeDimensions.specialWorldProperty(),
                            completeDimensions.lifecycle()), new WorldGenSettings(template.options(), dimensions)),
                                                            completeDimensions.dimensionsRegistryAccess());
                }, WorldStem::new);
                result = createLoader(access, packRepository, worldStem, template.gameRules(), true);
            }
            failed = false;
            return result;
        }
        catch (WorldPrepareException exception) {
            throw exception;
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WorldPrepareException(FailCode.WORLD_STEM_BUILD_FAILED, exception);
        }
        catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime && runtime.getCause() instanceof WorldPrepareException worldPrepareException) {
                throw worldPrepareException;
            }
            if (cause instanceof WorldPrepareException worldPrepareException) {
                throw worldPrepareException;
            }
            throw new WorldPrepareException(FailCode.WORLD_STEM_BUILD_FAILED, exception);
        }
        catch (Exception exception) {
            throw new WorldPrepareException(FailCode.WORLD_PREPARE_UNKNOWN, exception);
        }
        finally {
            if (failed) {
                access.safeClose();
            }
        }
    }

    //    /**
    //     * @see WorldOpenFlows#loadWorldDataBlocking(WorldLoader.PackConfig, WorldLoader.WorldDataSupplier, WorldLoader.ResultFactory)
    //     */
    static <D> WorldStem createStemBlocking(
            Minecraft mc,
            WorldLoader.PackConfig packConfig,
            WorldLoader.WorldDataSupplier<D> worldDataGetter,
            WorldLoader.ResultFactory<D, WorldStem> worldDataSupplier
    ) throws InterruptedException, ExecutionException {
        long start = Util.getMillis();
        WorldLoader.InitConfig config = new WorldLoader.InitConfig(packConfig,
                                                                   Commands.CommandSelection.INTEGRATED,
                                                                   LevelBasedPermissionSet.GAMEMASTER);
        CompletableFuture<WorldStem> resourceLoad =
                WorldLoader.load(config, worldDataGetter, worldDataSupplier, Util.backgroundExecutor(), mc);
        mc.managedBlock(resourceLoad::isDone);
        long end = Util.getMillis();
        WOMM.LOGGER.debug("World resource load blocked for {} ms", end - start);
        return resourceLoad.get();
    }

    @FunctionalInterface
    interface Loader {
        void loadWorld(Minecraft mc);
    }

    private static Loader createLoader(LevelStorageSource.LevelStorageAccess access,
                                       PackRepository repository,
                                       WorldStem worldStem,
                                       Optional<GameRules> gameRules,
                                       boolean newWorld) {
        return mc -> mc.doWorldLoad(access, repository, worldStem, gameRules, newWorld);
    }
}
