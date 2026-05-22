package com.lnatit.womm.legacy;

import com.lnatit.womm.WOMM;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WOMMPayload(String msg) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WOMMPayload> TYPE = new CustomPacketPayload.Type<>(WOMM.id("payload"));

    public static final StreamCodec<ByteBuf, WOMMPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, WOMMPayload::msg, WOMMPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
