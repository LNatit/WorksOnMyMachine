package com.lnatit.womm;

import com.lnatit.womm.command.ArgumentRegistry;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(WOMM.MODID)
public class WOMM {
    public static final String MODID = "womm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WOMM(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(WOMM::registerPayload);
        ArgumentRegistry.ARGUMENT_TYPES.register(modEventBus);
    }

    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.commonToClient(WOMMPayload.TYPE, WOMMPayload.STREAM_CODEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
