package com.lnatit.womm.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public interface StreamCodecs
{
    StreamCodec<RegistryFriendlyByteBuf, WorldDataConfiguration> DATA_CONFIG = ByteBufCodecs.fromCodecWithRegistries(WorldDataConfiguration.CODEC);
    /** Transmits only the registry key; registry lookup is deferred to assemble(). */
    StreamCodec<ByteBuf, ResourceKey<WorldPreset>> PRESET_KEY = ResourceKey.streamCodec(Registries.WORLD_PRESET);
    StreamCodec<RegistryFriendlyByteBuf, GameRuleMap> GAME_RULES = ByteBufCodecs.fromCodecWithRegistries(GameRuleMap.CODEC);
}
