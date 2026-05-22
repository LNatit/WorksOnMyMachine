package com.lnatit.womm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Optional;

public record Templatel(
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
    public static final Codec<Templatel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("identity").forGetter(Templatel::identity),
            Codec.BOOL.fieldOf("always_recreate").forGetter(Templatel::alwaysRecreate),
            GameType.CODEC.fieldOf("game_type").forGetter(Templatel::gameType),
            Difficulty.CODEC.fieldOf("difficulty").forGetter(Templatel::difficulty),
            Codec.BOOL.fieldOf("hardcore").forGetter(Templatel::hardcore),
            Codec.BOOL.fieldOf("locked").forGetter(Templatel::locked),
            WorldDataConfiguration.CODEC.fieldOf("data_config").forGetter(Templatel::dataConfig),
            WorldPreset.DIRECT_CODEC.fieldOf("preset").forGetter(Templatel::preset),
            Codec.LONG.optionalFieldOf("seed").forGetter(Templatel::seed),
            Codec.BOOL.fieldOf("generate_structures").forGetter(Templatel::generateStructures),
            Codec.BOOL.fieldOf("generate_bonus_chest").forGetter(Templatel::generateBonusChest),
            GameRuleMap.CODEC.optionalFieldOf("game_rules").forGetter(Templatel::gameRules)
    ).apply(instance, Templatel::new));

    public static final StreamCodec<FriendlyByteBuf, Templatel> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            Templatel::identity,
            ByteBufCodecs.BOOL,
            Templatel::alwaysRecreate,
            GameType.STREAM_CODEC,
            Templatel::gameType,
            Difficulty.STREAM_CODEC,
            Templatel::difficulty,
            ByteBufCodecs.BOOL,
            Templatel::hardcore,
            ByteBufCodecs.BOOL,
            Templatel::locked,
            StreamCodecs.DATA_CONFIG,
            Templatel::dataConfig,
            StreamCodecs.PRESETS,
            Templatel::preset,
            ByteBufCodecs.LONG.apply(ByteBufCodecs::optional),
            Templatel::seed,
            ByteBufCodecs.BOOL,
            Templatel::generateStructures,
            ByteBufCodecs.BOOL,
            Templatel::generateBonusChest,
            StreamCodecs.GAME_RULES.apply(ByteBufCodecs::optional),
            Templatel::gameRules,
            Templatel::new
    );

}
