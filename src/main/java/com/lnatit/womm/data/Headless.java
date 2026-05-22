package com.lnatit.womm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Optional;

public record Headless(
        Optional<Identifier> identity,
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
        Optional<GameRuleMap> gameRules) implements Template<Optional<Identifier>>
{

    public static final Codec<Headless> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("identity").forGetter(Headless::identity),
            Codec.BOOL.fieldOf("always_recreate").forGetter(Headless::alwaysRecreate),
            GameType.CODEC.fieldOf("game_type").forGetter(Headless::gameType),
            Difficulty.CODEC.fieldOf("difficulty").forGetter(Headless::difficulty),
            Codec.BOOL.fieldOf("hardcore").forGetter(Headless::hardcore),
            Codec.BOOL.fieldOf("locked").forGetter(Headless::locked),
            WorldDataConfiguration.CODEC.fieldOf("data_config").forGetter(Headless::dataConfig),
            WorldPreset.DIRECT_CODEC.fieldOf("preset").forGetter(Headless::preset),
            Codec.LONG.optionalFieldOf("seed").forGetter(Headless::seed),
            Codec.BOOL.fieldOf("generate_structures").forGetter(Headless::generateStructures),
            Codec.BOOL.fieldOf("generate_bonus_chest").forGetter(Headless::generateBonusChest),
            GameRuleMap.CODEC.optionalFieldOf("game_rules").forGetter(Headless::gameRules)
    ).apply(instance, Headless::new));

    public static final StreamCodec<FriendlyByteBuf, Headless> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional),
            Headless::identity,
            ByteBufCodecs.BOOL,
            Headless::alwaysRecreate,
            GameType.STREAM_CODEC,
            Headless::gameType,
            Difficulty.STREAM_CODEC,
            Headless::difficulty,
            ByteBufCodecs.BOOL,
            Headless::hardcore,
            ByteBufCodecs.BOOL,
            Headless::locked,
            StreamCodecs.DATA_CONFIG,
            Headless::dataConfig,
            StreamCodecs.PRESETS,
            Headless::preset,
            ByteBufCodecs.LONG.apply(ByteBufCodecs::optional),
            Headless::seed,
            ByteBufCodecs.BOOL,
            Headless::generateStructures,
            ByteBufCodecs.BOOL,
            Headless::generateBonusChest,
            StreamCodecs.GAME_RULES.apply(ByteBufCodecs::optional),
            Headless::gameRules,
            Headless::new
    );
}
