package com.lnatit.womm.data;

import com.mojang.datafixers.util.Function12;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Optional;
import java.util.function.Function;

public interface Template<I> {
    I identity();

    boolean alwaysRecreate();

    GameType gameType();

    Difficulty difficulty();

    boolean hardcore();

    boolean locked();

    WorldDataConfiguration dataConfig();

    WorldPreset preset();

    Optional<Long> seed();

    boolean generateStructures();

    boolean generateBonusChest();

    Optional<GameRuleMap> gameRules();

    static <T extends Template<I>, I> Codec<T> codec(Function<String, MapCodec<I>> mapFunc, Function12<I, Boolean, GameType, Difficulty, Boolean, Boolean, WorldDataConfiguration, WorldPreset, Optional<Long>, Boolean, Boolean, Optional<GameRuleMap>, T> constructor) {
        return RecordCodecBuilder.create(instance -> instance.group(
                mapFunc.apply("identity").forGetter(Template::identity),
                Codec.BOOL.fieldOf("always_recreate").forGetter(Template::alwaysRecreate),
                GameType.CODEC.fieldOf("game_type").forGetter(Template::gameType),
                Difficulty.CODEC.fieldOf("difficulty").forGetter(Template::difficulty),
                Codec.BOOL.fieldOf("hardcore").forGetter(Template::hardcore),
                Codec.BOOL.fieldOf("locked").forGetter(Template::locked),
                WorldDataConfiguration.CODEC.fieldOf("data_config").forGetter(Template::dataConfig),
                WorldPreset.DIRECT_CODEC.fieldOf("preset").forGetter(Template::preset),
                Codec.LONG.optionalFieldOf("seed").forGetter(Template::seed),
                Codec.BOOL.fieldOf("generate_structures").forGetter(Template::generateStructures),
                Codec.BOOL.fieldOf("generate_bonus_chest").forGetter(Template::generateBonusChest),
                GameRuleMap.CODEC.optionalFieldOf("game_rules").forGetter(Template::gameRules)
        ).apply(instance, constructor));
    }

    static <T extends Template<I>, I> StreamCodec<ByteBuf, T> streamCodec(StreamCodec<ByteBuf, I> idStreamCodec, Function12<I, Boolean, GameType, Difficulty, Boolean, Boolean, WorldDataConfiguration, WorldPreset, Optional<Long>, Boolean, Boolean, Optional<GameRuleMap>, T> constructor) {
        return StreamCodec.composite(
                idStreamCodec,
                Template::identity,
                ByteBufCodecs.BOOL,
                Template::alwaysRecreate,
                GameType.STREAM_CODEC,
                Template::gameType,
                Difficulty.STREAM_CODEC,
                Template::difficulty,
                ByteBufCodecs.BOOL,
                Template::hardcore,
                ByteBufCodecs.BOOL,
                Template::locked,
                StreamCodecs.DATA_CONFIG,
                Template::dataConfig,
                StreamCodecs.PRESETS,
                Template::preset,
                ByteBufCodecs.LONG.apply(ByteBufCodecs::optional),
                Template::seed,
                ByteBufCodecs.BOOL,
                Template::generateStructures,
                ByteBufCodecs.BOOL,
                Template::generateBonusChest,
                StreamCodecs.GAME_RULES.apply(ByteBufCodecs::optional),
                Template::gameRules,
                constructor
        );
    }
}
