package com.lnatit.womm.reference;

import com.lnatit.womm.WOMM;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

public interface PackedPipeline
{
    Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
    Component DEFAULT_WORLD_NAME = Component.translatable("selectWorld.newWorld");

    /**
     * @param worldName
     * @return isSuccess
     * @see CreateWorldScreen#createWorldAndCleanup(LayeredRegistryAccess, LevelDataAndDimensions.WorldDataAndGenSettings, Optional)
     */
    static boolean createNewWorld(String worldName) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreenAndShow(new GenericMessageScreen(PREPARING_WORLD_DATA));
        long start = Util.getMillis();

        /**
         * @see CreateWorldScreen#openFresh(Minecraft, Runnable, CreateWorldCallback)
         */
        WorldCreationContextMapper worldCreationContext = (managers, registries, cookie) -> new WorldCreationContext(
                cookie.worldGenSettings(),
                registries,
                managers,
                cookie.dataConfiguration());
        Function<WorldLoader.DataLoadContext, WorldGenSettings> settingsFunction = context -> new WorldGenSettings(
                WorldOptions.defaultWithRandomSeed(),
                WorldPresets.createNormalWorldDimensions(context.datapackWorldgen()));

        /**
         * @see CreateWorldScreen#openCreateWorldScreen
         */
        PackRepository vanillaOnlyPackRepository = new PackRepository(new ServerPacksSource(mc.directoryValidator()));
        net.neoforged.neoforge.resource.ResourcePackLoader.populatePackRepository(vanillaOnlyPackRepository,
                                                                                  net.minecraft.server.packs.PackType.SERVER_DATA,
                                                                                  false);
        WorldDataConfiguration dataConfig = SharedConstants.IS_RUNNING_IN_IDE
                                            ? new WorldDataConfiguration(new DataPackConfig(List.of("vanilla",
                                                                                                    "tests"),
                                                                                            List.of()),
                                                                         FeatureFlags.DEFAULT_FLAGS)
                                            : WorldDataConfiguration.DEFAULT;
        WorldLoader.PackConfig packConfig =
                new WorldLoader.PackConfig(vanillaOnlyPackRepository, dataConfig, false, true);
        WorldLoader.InitConfig loadConfig = new WorldLoader.InitConfig(packConfig,
                                                                       Commands.CommandSelection.INTEGRATED,
                                                                       LevelBasedPermissionSet.GAMEMASTER);
        CompletableFuture<WorldCreationContext> loadResult = WorldLoader.load(loadConfig,
                                                                              context -> new WorldLoader.DataLoadOutput<>(
                                                                                      new DataPackReloadCookie(
                                                                                              settingsFunction.apply(
                                                                                                      context),
                                                                                              context.dataConfiguration()),
                                                                                      context.datapackDimensions()),
                                                                              (resources, managers, registries, cookie) -> {
                                                                                  resources.close();
                                                                                  return worldCreationContext.apply(
                                                                                          managers,
                                                                                          registries,
                                                                                          cookie);
                                                                              },
                                                                              Util.backgroundExecutor(),
                                                                              mc);
        mc.managedBlock(loadResult::isDone);
        long end = Util.getMillis();
        WOMM.LOGGER.debug("Resource load for world creation blocked for {} ms", end - start);

        /**
         * @see CreateWorldScreen#createNewWorld(LayeredRegistryAccess, LevelDataAndDimensions.WorldDataAndGenSettings, Optional)
         */
        String worldFolder;
        String trimmedName = worldName.trim();
        Path savesFolder = Minecraft.getInstance().getLevelSource().getBaseDir();
        try {
            worldFolder = FileUtil.findAvailableName(savesFolder,
                                                     !trimmedName.isEmpty()
                                                     ? trimmedName
                                                     : DEFAULT_WORLD_NAME.getString(),
                                                     "");
        }
        catch (Exception exception) {
            try {
                worldFolder = FileUtil.findAvailableName(savesFolder, "World", "");
            }
            catch (IOException ioException) {
                throw new RuntimeException("Could not create save folder", ioException);
            }
        }
        WorldCreationContext context = loadResult.join();
        Path tempDataPackDir = getOrCreateTempDataPackDir(mc, worldName);
        Optional<LevelStorageSource.LevelStorageAccess> newWorldAccess =
                createNewWorldDirectory(mc, worldFolder, tempDataPackDir);
        if (newWorldAccess.isEmpty()) {
            SystemToast.onPackCopyFailure(mc, worldFolder);
            cleanOnFail(mc, tempDataPackDir);
            return false;
        }
        else {
            /**
             * @see CreateWorldScreen#onCreate()
             */
            WorldDimensions worldDimensions = context.selectedDimensions();
            WorldDimensions.Complete finalDimensions = worldDimensions.bake(context.datapackDimensions());
            LayeredRegistryAccess<RegistryLayer> finalLayers = context.worldgenRegistries()
                                                                      .replaceFrom(RegistryLayer.DIMENSIONS,
                                                                                   finalDimensions.dimensionsRegistryAccess());
            FeatureFlagSet enabledFeatures = context.dataConfiguration().enabledFeatures();
            Lifecycle lifecycleFromFeatures =
                    FeatureFlags.isExperimental(enabledFeatures) ? Lifecycle.experimental() : Lifecycle.stable();
            Lifecycle lifecycleFromRegistries = finalLayers.compositeAccess().allRegistriesLifecycle();
            Lifecycle lifecycle = lifecycleFromRegistries.add(lifecycleFromFeatures);
            boolean skipWarning = lifecycleFromRegistries == Lifecycle.stable();
            boolean isDebug = finalDimensions.specialWorldProperty() == PrimaryLevelData.SpecialWorldProperty.DEBUG;
            String name = DEFAULT_WORLD_NAME.getString().trim();
            LevelSettings levelSettings = isDebug
                                          ? new LevelSettings(name,
                                                              GameType.SPECTATOR,
                                                              new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,
                                                                                                   false,
                                                                                                   false),
                                                              true,
                                                              WorldDataConfiguration.DEFAULT)
                                          : new LevelSettings(name,
                                                              context.initialWorldCreationOptions()
                                                                     .selectedGameMode().gameType,
                                                  // TODO customize
                                                              new LevelSettings.DifficultySettings(Difficulty.NORMAL,
                                                                                                   false,
                                                                                                   false),
                                                              true,
                                                              context.dataConfiguration());
            GameRules gameRules;
            if (isDebug) {
                gameRules = MinecraftServer.DEFAULT_GAME_RULES.get();
                gameRules.set(GameRules.ADVANCE_TIME, false, null);
            }
            else {
                gameRules = new GameRules(enabledFeatures);
            }

            PrimaryLevelData worldData =
                    new PrimaryLevelData(levelSettings, finalDimensions.specialWorldProperty(), lifecycle);
            WorldOptions options = context.options();
            WorldGenSettings worldGenSettings = new WorldGenSettings(options, worldDimensions);
            LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings =
                    new LevelDataAndDimensions.WorldDataAndGenSettings(worldData, worldGenSettings);


            if (worldDataAndGenSettings.data().worldGenSettingsLifecycle() != Lifecycle.stable()) {
                // Neo: set experimental settings confirmation flag so user is not shown warning on next open
                ((PrimaryLevelData) worldDataAndGenSettings.data()).withConfirmedWarning(true);
            }
            LevelStorageSource.LevelStorageAccess levelSourceAccess = newWorldAccess.get();
            PackRepository packRepository = ServerPacksSource.createPackRepository(levelSourceAccess);
            CloseableResourceManager resourceManager = new WorldLoader.PackConfig(packRepository,
                                                                                  worldDataAndGenSettings.data()
                                                                                                         .getDataConfiguration(),
                                                                                  false,
                                                                                  false).createResourceManager()
                                                                                        .getSecond();
            mc.doWorldLoad(levelSourceAccess,
                           packRepository,
                           new WorldStem(resourceManager,
                                         context.dataPackResources(),
                                         finalLayers,
                                         worldDataAndGenSettings),
                           Optional.of(gameRules),
                           true);
            removeTempDataPackDir(tempDataPackDir);
            return true;
        }
    }

    /**
     * @param minecraft
     * @param worldFolder
     * @return
     * @see CreateWorldScreen#createNewWorldDirectory(Minecraft, String, Path)
     */
    static Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory(
            Minecraft minecraft,
            String worldFolder,
            Path tempDataPackDir
    ) {
        try {
            LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(worldFolder);
            if (tempDataPackDir == null) {
                return Optional.of(access);
            }

            try {
                Optional<LevelStorageSource.LevelStorageAccess> result;
                try (Stream<Path> files = Files.walk(tempDataPackDir)) {
                    Path targetDir = access.getLevelPath(LevelResource.DATAPACK_DIR);
                    FileUtil.createDirectoriesSafe(targetDir);
                    files.filter(f -> !f.equals(tempDataPackDir))
                         .forEach(source -> copyBetweenDirs(tempDataPackDir, targetDir, source));
                    result = Optional.of(access);
                }

                return result;
            }
            catch (UncheckedIOException | IOException var9) {
                WOMM.LOGGER.warn("Failed to copy datapacks to world {}", worldFolder, var9);
                access.close();
            }
        }
        catch (UncheckedIOException | IOException var10) {
            WOMM.LOGGER.warn("Failed to create access for {}", worldFolder, var10);
        }

        return Optional.empty();
    }

    /**
     * @param sourceDir
     * @param targetDir
     * @param sourcePath
     * @see CreateWorldScreen#copyBetweenDirs(Path, Path, Path)
     */
    static void copyBetweenDirs(Path sourceDir, Path targetDir, Path sourcePath) {
        try {
            Util.copyBetweenDirs(sourceDir, targetDir, sourcePath);
        }
        catch (IOException exception) {
            WOMM.LOGGER.warn("Failed to copy datapack file from {} to {}", sourcePath, targetDir);
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * @param minecraft
     * @param worldFolder
     * @return
     * @see CreateWorldScreen#getOrCreateTempDataPackDir()
     */
    static @Nullable Path getOrCreateTempDataPackDir(Minecraft minecraft, String worldFolder) {
        Path path = null;
        try {
            path = Files.createTempDirectory("mcworld-");
            return path;
        }
        catch (IOException exception) {
            WOMM.LOGGER.warn("Failed to create temporary dir", exception);
            SystemToast.onPackCopyFailure(minecraft, worldFolder);
            cleanOnFail(minecraft, path);
        }
        return null;
    }

    static void cleanOnFail(Minecraft minecraft, Path path) {
        minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
        removeTempDataPackDir(path);
    }

    /**
     * @see CreateWorldScreen#removeTempDataPackDir()
     */
    static void removeTempDataPackDir(Path tempDataPackDir) {
        if (tempDataPackDir != null && Files.exists(tempDataPackDir)) {
            try (Stream<Path> files = Files.walk(tempDataPackDir)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    }
                    catch (IOException exception) {
                        WOMM.LOGGER.warn("Failed to remove temporary file {}", path, exception);
                    }
                });
            }
            catch (IOException exception) {
                WOMM.LOGGER.warn("Failed to list temporary dir {}", tempDataPackDir);
            }
        }
    }

}
