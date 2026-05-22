package com.lnatit.womm;

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
        WOMM.LOGGER.debug("Received payload: {}", payload.identity());



        disconnect();

    }

    private static void disconnect() {
        Minecraft mc = Minecraft.getInstance();
        mc.getReportingContext()
          .draftReportHandled(mc, mc.screen, () -> mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), false);
    }
}
