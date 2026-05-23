package com.lnatit.womm;

import com.lnatit.womm.data.Cached;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

public record WOMMPayload(String identity, Optional<Cached> template) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<WOMMPayload> TYPE = new CustomPacketPayload.Type<>(WOMM.id("world_template"));

    public static final StreamCodec<FriendlyByteBuf, WOMMPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WOMMPayload::identity,
            Cached.STREAM_CODEC.apply(ByteBufCodecs::optional),
            WOMMPayload::template,
            WOMMPayload::new
    );

    public WOMMPayload(String identity) {
        this(identity, Optional.empty());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
