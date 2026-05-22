package com.lnatit.womm.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public interface StreamCodecs
{
    StreamCodec<ByteBuf, WorldDataConfiguration> DATA_CONFIG = ByteBufCodecs.fromCodec(WorldDataConfiguration.CODEC);
    StreamCodec<ByteBuf, WorldPreset> PRESETS = ByteBufCodecs.fromCodec(WorldPreset.DIRECT_CODEC);
    StreamCodec<ByteBuf, GameRuleMap> GAME_RULES = ByteBufCodecs.fromCodec(GameRuleMap.CODEC);
}
