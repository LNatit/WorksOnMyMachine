package com.lnatit.womm;

import com.lnatit.womm.data.Template;
import com.lnatit.womm.data.TemplateManager;
import com.lnatit.womm.pipeline.LoadContext;
import com.lnatit.womm.pipeline.Pipeline;
import com.lnatit.womm.pipeline.WorldPrepareException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

@Mod(value = WOMM.MODID, dist = Dist.CLIENT)
public class WOMMClient
{
    //    public static boolean isInWOMMWorld = false;
    public static Runnable returnCallback = null;

    public WOMMClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(WOMMClient::registerPayloadHandler);
    }

    public static void registerPayloadHandler(RegisterClientPayloadHandlersEvent event) {
        event.register(WOMMPayload.TYPE, WOMMClient::handlePayload);
    }

    public static void handlePayload(WOMMPayload payload, IPayloadContext context) {
        String identity = payload.identity();
        WOMM.LOGGER.debug("Received payload: {}", identity);

        Template template;
        if (payload.template().isPresent()) {
            template = payload.template().get();
        }
        else {
            var op = TemplateManager.INSTANCE.getTemplate(identity);
            if (op.isPresent()) {
                template = op.get();
            }
            else {
                WOMM.LOGGER.info("Received payload with unknown template identity '{}', ignored!", identity);
                return;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        IntegratedServer singleplayer = mc.getSingleplayerServer();
        if (singleplayer != null && singleplayer.isPublished()) {
            WOMM.LOGGER.info("Received payload while in a published singleplayer world, ignored!");
            return;
        }

        LoadContext loadContext = template.assemble();

        returnCallback = snapshotAndDisconnect(mc, singleplayer);

        Pipeline.Loader loader;
        try {
            loader = Pipeline.prepareResources(loadContext);
            loader.loadWorld(Minecraft.getInstance());
        }
        catch (WorldPrepareException e) {
            WOMM.LOGGER.error("Failed to prepare world resources for '{}': {}", identity, e.getMessage());
        }
    }


    /**
     * @see WorldSelectionList.WorldListEntry#joinWorld()
     */
    private static Runnable snapshotAndDisconnect(Minecraft mc, @Nullable IntegratedServer singleplayer) {
        Runnable callback = null;

        if (singleplayer != null) {
            callback = () -> mc.createWorldOpenFlows()
                                   .openWorld(singleplayer.storageSource.getLevelId(),
                                              () -> mc.setScreen(new TitleScreen()));
        }
        else {
            ServerData server = mc.getCurrentServer();
            if (server != null) {
                callback = () -> ConnectScreen.startConnecting(new TitleScreen(),
                                                                   mc,
                                                                   ServerAddress.parseString(server.ip),
                                                                   server,
                                                                   false,
                                                                   null);
            }
            WOMM.LOGGER.warn("Trying to snapshot an impossible world!");
        }

        mc.getReportingContext()
          .draftReportHandled(mc, mc.screen, () -> mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), false);

        return callback;
    }

    public static boolean isCallbackEmpty() {
        return returnCallback == null;
    }

    public static void returnToWorld(final Button button) {
        if (isCallbackEmpty()) {
            return;
        }
        returnCallback.run();
        returnCallback = null;
        WOMM.LOGGER.info("Returning to previous world...");
    }
}
