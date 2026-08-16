package com.lnatit.womm.data;

import com.mojang.datafixers.util.Function13;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;

import java.util.Optional;
import java.util.function.Function;

public interface ITemplate<I, P> {
    I identity();

    boolean alwaysRecreate();

    GameType gameType();

    Difficulty difficulty();

    boolean hardcore();

    boolean locked();

    WorldDataConfiguration dataConfig();

    P preset();

    Optional<ResourceKey<?>> dimensionsUpdater();

    Optional<Long> seed();

    boolean generateStructures();

    boolean generateBonusChest();

    Optional<GameRuleMap> gameRules();

    static <T extends ITemplate<I, P>, I, P> Codec<T> codec(Function<String, MapCodec<I>> idMapFunc, Function<String, MapCodec<P>> presetMapFunc, Function13<I, Boolean, GameType, Difficulty, Boolean, Boolean, WorldDataConfiguration, P, Optional<ResourceKey<?>>, Optional<Long>, Boolean, Boolean, Optional<GameRuleMap>, T> constructor) {
        return RecordCodecBuilder.create(instance -> instance.group(
                idMapFunc.apply("identity").forGetter(ITemplate::identity),
                Codec.BOOL.fieldOf("always_recreate").forGetter(ITemplate::alwaysRecreate),
                GameType.CODEC.fieldOf("game_type").forGetter(ITemplate::gameType),
                Difficulty.CODEC.fieldOf("difficulty").forGetter(ITemplate::difficulty),
                Codec.BOOL.fieldOf("hardcore").forGetter(ITemplate::hardcore),
                Codec.BOOL.fieldOf("locked").forGetter(ITemplate::locked),
                WorldDataConfiguration.CODEC.lenientOptionalFieldOf("data_config", WorldDataConfiguration.DEFAULT).forGetter(ITemplate::dataConfig),
                presetMapFunc.apply("preset").forGetter(ITemplate::preset),
                Utils.RAW_RESOURCE_KEY.optionalFieldOf("dimensions_updater").forGetter(ITemplate::dimensionsUpdater),
                Codec.LONG.optionalFieldOf("seed").forGetter(ITemplate::seed),
                Codec.BOOL.fieldOf("generate_structures").forGetter(ITemplate::generateStructures),
                Codec.BOOL.fieldOf("generate_bonus_chest").forGetter(ITemplate::generateBonusChest),
                GameRuleMap.CODEC.optionalFieldOf("game_rules").forGetter(ITemplate::gameRules)
        ).apply(instance, constructor));
    }
}
