package com.lnatit.womm.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Optional;

public record Cached(
        String identity,
        boolean alwaysRecreate,
        GameType gameType,
        Difficulty difficulty,
        boolean hardcore,
        boolean locked,
        WorldDataConfiguration dataConfig,
        WorldPreset preset,
        Optional<Long> seed,
        boolean generateStructures,
        boolean generateBonusChest,
        Optional<GameRuleMap> gameRules
) implements Template<String>
{
    public static final Codec<Cached> CODEC = Template.codec(Codec.STRING::fieldOf, Cached::new);

    public static final StreamCodec<ByteBuf, Cached> STREAM_CODEC = Template.streamCodec(ByteBufCodecs.STRING_UTF8, Cached::new);
}
