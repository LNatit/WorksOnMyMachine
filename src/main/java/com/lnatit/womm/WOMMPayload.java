package com.lnatit.womm;

import com.lnatit.womm.data.Template;
import com.lnatit.womm.data.TemplateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

public record WOMMPayload(String identity, Optional<Template> template) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<WOMMPayload> TYPE = new CustomPacketPayload.Type<>(WOMM.id("world_template"));

    private static final StreamCodec<RegistryFriendlyByteBuf, WOMMPayload> REGISTRY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WOMMPayload::identity,
            Template.STREAM_CODEC.apply(ByteBufCodecs::optional),
            WOMMPayload::template,
            WOMMPayload::new
    );

    /**
     * Declared as FriendlyByteBuf for PayloadRegistrar compatibility.
     * NeoForge always provides a RegistryFriendlyByteBuf at runtime, so the cast is safe.
     */
    @SuppressWarnings("unchecked")
    public static final StreamCodec<FriendlyByteBuf, WOMMPayload> STREAM_CODEC =
            (StreamCodec<FriendlyByteBuf, WOMMPayload>) (Object) REGISTRY_STREAM_CODEC;

    public WOMMPayload(String identity) {
        this(identity, TemplateManager.INSTANCE.getTemplate(identity));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
