package com.lnatit.womm;

import com.lnatit.womm.data.Template;
import com.lnatit.womm.data.TemplateManager;
import com.lnatit.womm.pipeline.LoadContext;
import com.lnatit.womm.pipeline.Pipeline;
import com.lnatit.womm.pipeline.WorldPrepareException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@Mod(value = WOMM.MODID, dist = Dist.CLIENT)
public class WOMMClient
{
    public static boolean isInWOMMWorld = false;

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
        } else {
            var op = TemplateManager.INSTANCE.getTemplate(identity);
            if (op.isPresent()) {
                template = op.get();
            } else {
                WOMM.LOGGER.info("Received payload with unknown template identity '{}', ignored!", identity);
                return;
            }
        }

        LoadContext loadContext = template.assemble();

        disconnect();

        Pipeline.Loader loader;
        try {
            loader = Pipeline.prepareResources(loadContext);
            loader.loadWorld(Minecraft.getInstance());
        }
        catch (WorldPrepareException e) {
            WOMM.LOGGER.error("Failed to prepare world resources for '{}': {}", identity, e.getMessage());
        }
    }

    private static void disconnect() {
        Minecraft mc = Minecraft.getInstance();
        mc.getReportingContext()
          .draftReportHandled(mc, mc.screen, () -> mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), false);
    }
}
