package com.lnatit.womm;

import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

@Mod(value = WOMM.MODID, dist = Dist.CLIENT)
public class WOMMClient {
    public static final Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
    public static final Component DEFAULT_WORLD_NAME = Component.translatable("selectWorld.newWorld");


    public WOMMClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(WOMMClient::registerPayloadHandler);
    }

    public static void registerPayloadHandler(RegisterClientPayloadHandlersEvent event) {
        event.register(WOMMPayload.TYPE, WOMMClient::handlePayload);
    }

    public static void handlePayload(WOMMPayload payload, IPayloadContext context) {
        WOMM.LOGGER.debug("Received payload with message: {}", payload.msg());
        disconnect();
        createNewWorld(payload.msg());
    }

    private static void disconnect() {
        Minecraft mc = Minecraft.getInstance();
        mc.getReportingContext().draftReportHandled(mc, mc.screen, () -> mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), false);
    }

    /**
     * @param worldName
     * @return isSuccess
     * @see CreateWorldScreen#createWorldAndCleanup(LayeredRegistryAccess, LevelDataAndDimensions.WorldDataAndGenSettings, Optional)
     */
    private static boolean createNewWorld(String worldName) {
        Minecraft mc = Minecraft.getInstance();
        queueLoadScreen(mc, PREPARING_WORLD_DATA);
        long start = Util.getMillis();

        /**
         * @see CreateWorldScreen#openFresh(Minecraft, Runnable, CreateWorldCallback)
         */
        WorldCreationContextMapper worldCreationContext = (managers, registries, cookie) -> new WorldCreationContext(cookie.worldGenSettings(), registries, managers, cookie.dataConfiguration());
        Function<WorldLoader.DataLoadContext, WorldGenSettings> settingsFunction = context -> new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(context.datapackWorldgen()));

        /**
         * @see CreateWorldScreen#openCreateWorldScreen
         */
        PackRepository vanillaOnlyPackRepository = new PackRepository(new ServerPacksSource(mc.directoryValidator()));
        net.neoforged.neoforge.resource.ResourcePackLoader.populatePackRepository(vanillaOnlyPackRepository, net.minecraft.server.packs.PackType.SERVER_DATA, false);
        WorldDataConfiguration dataConfig = SharedConstants.IS_RUNNING_IN_IDE ? new WorldDataConfiguration(new DataPackConfig(List.of("vanilla", "tests"), List.of()), FeatureFlags.DEFAULT_FLAGS) : WorldDataConfiguration.DEFAULT;
        WorldLoader.InitConfig loadConfig = createDefaultLoadConfig(vanillaOnlyPackRepository, dataConfig);
        CompletableFuture<WorldCreationContext> loadResult = WorldLoader.load(loadConfig, context -> new WorldLoader.DataLoadOutput<>(new DataPackReloadCookie(settingsFunction.apply(context), context.dataConfiguration()), context.datapackDimensions()), (resources, managers, registries, cookie) -> {
            resources.close();
            return worldCreationContext.apply(managers, registries, cookie);
        }, Util.backgroundExecutor(), mc);
        mc.managedBlock(loadResult::isDone);
        long end = Util.getMillis();
        WOMM.LOGGER.debug("Resource load for world creation blocked for {} ms", end - start);

        /**
         * @see CreateWorldScreen#createNewWorld(LayeredRegistryAccess, LevelDataAndDimensions.WorldDataAndGenSettings, Optional)
         */
        String worldFolder = getTargetFolder(worldName);
        WorldCreationContext context = loadResult.join();
        Path tempDataPackDir = getOrCreateTempDataPackDir(mc, worldName);
        Optional<LevelStorageSource.LevelStorageAccess> newWorldAccess = createNewWorldDirectory(mc, worldFolder, tempDataPackDir);
        if (newWorldAccess.isEmpty()) {
            SystemToast.onPackCopyFailure(mc, worldFolder);
            cleanOnFail(mc, tempDataPackDir);
            return false;
        } else {
            /**
             * @see CreateWorldScreen#onCreate()
             */
            WorldDimensions worldDimensions = context.selectedDimensions();
            WorldDimensions.Complete finalDimensions = worldDimensions.bake(context.datapackDimensions());
            LayeredRegistryAccess<RegistryLayer> finalLayers = context.worldgenRegistries()
                    .replaceFrom(RegistryLayer.DIMENSIONS, finalDimensions.dimensionsRegistryAccess());
            FeatureFlagSet enabledFeatures = context.dataConfiguration().enabledFeatures();
            Lifecycle lifecycleFromFeatures = FeatureFlags.isExperimental(enabledFeatures) ? Lifecycle.experimental() : Lifecycle.stable();
            Lifecycle lifecycleFromRegistries = finalLayers.compositeAccess().allRegistriesLifecycle();
            Lifecycle lifecycle = lifecycleFromRegistries.add(lifecycleFromFeatures);
            boolean skipWarning = lifecycleFromRegistries == Lifecycle.stable();
            boolean isDebug = finalDimensions.specialWorldProperty() == PrimaryLevelData.SpecialWorldProperty.DEBUG;
            LevelSettings levelSettings = createLevelSettings(context, isDebug);
            GameRules gameRules;
            if (isDebug) {
                gameRules = MinecraftServer.DEFAULT_GAME_RULES.get();
                gameRules.set(GameRules.ADVANCE_TIME, false, null);
            } else {
                gameRules = new GameRules(context.dataConfiguration().enabledFeatures()).copy(enabledFeatures);
            }

            PrimaryLevelData worldData = new PrimaryLevelData(levelSettings, finalDimensions.specialWorldProperty(), lifecycle);
            WorldOptions options = context.options();
            WorldGenSettings worldGenSettings = new WorldGenSettings(options, worldDimensions);
            LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings = new LevelDataAndDimensions.WorldDataAndGenSettings(worldData, worldGenSettings);


            if (worldDataAndGenSettings.data().worldGenSettingsLifecycle() != Lifecycle.stable()) {
                // Neo: set experimental settings confirmation flag so user is not shown warning on next open
                ((PrimaryLevelData) worldDataAndGenSettings.data()).withConfirmedWarning(true);
            }
            mc.createWorldOpenFlows().createLevelFromExistingSettings(newWorldAccess.get(), context.dataPackResources(), finalLayers, worldDataAndGenSettings, Optional.of(gameRules));
            removeTempDataPackDir(tempDataPackDir);
            return true;
        }
    }

    private static String getTargetFolder(String worldName) {
        String trimmedName = worldName.trim();
        Path savesFolder = Minecraft.getInstance().getLevelSource().getBaseDir();
        try {
            return FileUtil.findAvailableName(savesFolder, !trimmedName.isEmpty() ? trimmedName : DEFAULT_WORLD_NAME.getString(), "");
        } catch (Exception var5) {
            try {
                return FileUtil.findAvailableName(savesFolder, "World", "");
            } catch (IOException var4) {
                throw new RuntimeException("Could not create save folder", var4);
            }
        }
    }

    private static void queueLoadScreen(Minecraft minecraft, Component message) {
        minecraft.setScreenAndShow(new GenericMessageScreen(message));
    }

    private static WorldLoader.InitConfig createDefaultLoadConfig(PackRepository packRepository, WorldDataConfiguration config) {
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, config, false, true);
        return new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER);
    }

    /**
     * @see CreateWorldScreen#createNewWorldDirectory(Minecraft, String, Path)
     * @param minecraft
     * @param worldFolder
     * @return
     */
    private static Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory(
            Minecraft minecraft, String worldFolder, Path tempDataPackDir
    ) {
        try {
            LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(worldFolder);
            if (tempDataPackDir == null) {
                return Optional.of(access);
            }

            try {
                Optional var6;
                try (Stream<Path> files = Files.walk(tempDataPackDir)) {
                    Path targetDir = access.getLevelPath(LevelResource.DATAPACK_DIR);
                    FileUtil.createDirectoriesSafe(targetDir);
                    files.filter(f -> !f.equals(tempDataPackDir)).forEach(source -> copyBetweenDirs(tempDataPackDir, targetDir, source));
                    var6 = Optional.of(access);
                }

                return var6;
            } catch (UncheckedIOException | IOException var9) {
                WOMM.LOGGER.warn("Failed to copy datapacks to world {}", worldFolder, var9);
                access.close();
            }
        } catch (UncheckedIOException | IOException var10) {
            WOMM.LOGGER.warn("Failed to create access for {}", worldFolder, var10);
        }

        return Optional.empty();
    }

    /**
     * @see CreateWorldScreen#copyBetweenDirs(Path, Path, Path)
     * @param sourceDir
     * @param targetDir
     * @param sourcePath
     */
    private static void copyBetweenDirs(Path sourceDir, Path targetDir, Path sourcePath) {
        try {
            Util.copyBetweenDirs(sourceDir, targetDir, sourcePath);
        } catch (IOException var4) {
            WOMM.LOGGER.warn("Failed to copy datapack file from {} to {}", sourcePath, targetDir);
            throw new UncheckedIOException(var4);
        }
    }

    /**
     * @see CreateWorldScreen#getOrCreateTempDataPackDir()
     * @param minecraft
     * @param worldFolder
     * @return
     */
    private static @Nullable Path getOrCreateTempDataPackDir(Minecraft minecraft, String worldFolder) {
        Path path = null;
        try {
            path = Files.createTempDirectory("mcworld-");
            return path;
        } catch (IOException var2) {
            WOMM.LOGGER.warn("Failed to create temporary dir", var2);
            SystemToast.onPackCopyFailure(minecraft, worldFolder);
            cleanOnFail(minecraft, path);
        }
        return null;
    }

    private static void cleanOnFail(Minecraft minecraft, Path path) {
        minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
        removeTempDataPackDir(path);
    }

    /**
     * @see CreateWorldScreen#removeTempDataPackDir()
     */
    private static void removeTempDataPackDir(Path tempDataPackDir) {
        if (tempDataPackDir != null && Files.exists(tempDataPackDir)) {
            try (Stream<Path> files = Files.walk(tempDataPackDir)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException var2) {
                        WOMM.LOGGER.warn("Failed to remove temporary file {}", path, var2);
                    }
                });
            } catch (IOException var6) {
                WOMM.LOGGER.warn("Failed to list temporary dir {}", tempDataPackDir);
            }
        }
    }

    /**
     * @param isDebug
     * @return
     * @see CreateWorldScreen#createLevelSettings(boolean)
     */
    private static LevelSettings createLevelSettings(WorldCreationContext context, boolean isDebug) {
        String name = DEFAULT_WORLD_NAME.getString().trim();
        return isDebug
                ? new LevelSettings(
                name, GameType.SPECTATOR, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT
        )
                : new LevelSettings(
                name,
                context.initialWorldCreationOptions().selectedGameMode().gameType,
                // TODO customize
                new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false),
                true,
                context.dataConfiguration()
        );
    }
}
