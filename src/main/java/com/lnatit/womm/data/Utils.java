package com.lnatit.womm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public interface Utils
{
    Codec<ResourceKey<?>> RAW_RESOURCE_KEY = RecordCodecBuilder.create((instance) -> instance.group(
            Identifier.CODEC.fieldOf("registry").forGetter(ResourceKey::registry),
            Identifier.CODEC.fieldOf("identifier").forGetter(ResourceKey::identifier)
    ).apply(instance, ResourceKey::create));

    StreamCodec<RegistryFriendlyByteBuf, WorldDataConfiguration> DATA_CONFIG = ByteBufCodecs.fromCodecWithRegistries(WorldDataConfiguration.CODEC);
    /** Transmits only the registry key; registry lookup is deferred to assemble(). */
    StreamCodec<ByteBuf, ResourceKey<WorldPreset>> PRESET_KEY = ResourceKey.streamCodec(Registries.WORLD_PRESET);
    StreamCodec<ByteBuf, ResourceKey<?>> RAW_RKEY = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            ResourceKey::registry,
            Identifier.STREAM_CODEC,
            ResourceKey::identifier,
            ResourceKey::create
    );
    StreamCodec<RegistryFriendlyByteBuf, GameRuleMap> GAME_RULES = ByteBufCodecs.fromCodecWithRegistries(GameRuleMap.CODEC);
}
