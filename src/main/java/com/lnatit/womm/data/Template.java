package com.lnatit.womm.data;

import com.lnatit.womm.pipeline.LoadContext;
import com.mojang.datafixers.util.Function14;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

public record Template(Identifier identity,
                       boolean abbreviatable,
                       boolean alwaysRecreate,
                       GameType gameType,
                       Difficulty difficulty,
                       boolean hardcore,
                       boolean locked,
                       WorldDataConfiguration dataConfig,
                       ResourceKey<WorldPreset> preset,
                       Optional<ResourceKey<?>> dimensionsUpdater,
                       Optional<Long> seed,
                       boolean generateStructures,
                       boolean generateBonusChest,
                       Optional<GameRuleMap> gameRules) implements ITemplate<Identifier, ResourceKey<WorldPreset>>
{
    private static final DateTimeFormatter LEVEL_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static final StreamCodec<RegistryFriendlyByteBuf, Template> STREAM_CODEC =
            composite(
                    Identifier.STREAM_CODEC,
                    ITemplate::identity,
                    ByteBufCodecs.BOOL,
                    Template::abbreviatable,
                    ByteBufCodecs.BOOL,
                    ITemplate::alwaysRecreate,
                    GameType.STREAM_CODEC,
                    ITemplate::gameType,
                    Difficulty.STREAM_CODEC,
                    ITemplate::difficulty,
                    ByteBufCodecs.BOOL,
                    ITemplate::hardcore,
                    ByteBufCodecs.BOOL,
                    ITemplate::locked,
                    Utils.DATA_CONFIG,
                    ITemplate::dataConfig,
                    Utils.PRESET_KEY,
                    ITemplate::preset,
                    Utils.RAW_RKEY.apply(ByteBufCodecs::optional),
                    ITemplate::dimensionsUpdater,
                    ByteBufCodecs.LONG.apply(ByteBufCodecs::optional),
                    ITemplate::seed,
                    ByteBufCodecs.BOOL,
                    ITemplate::generateStructures,
                    ByteBufCodecs.BOOL,
                    ITemplate::generateBonusChest,
                    Utils.GAME_RULES.apply(ByteBufCodecs::optional),
                    ITemplate::gameRules,
                    Template::new);

    public String getIdStr() {
        return this.abbreviatable ? this.identity.getPath() : this.identity.toString();
    }

    public LoadContext assemble() {
        String levelName = "WOMM-" + this.getIdStr();
        if (this.alwaysRecreate) {
            levelName = levelName + "-" + LocalDateTime.now().format(LEVEL_NAME_TIMESTAMP);
        }
        long seed = this.seed.orElse(WorldOptions.randomSeed());
        Optional<GameRules> gameRules = this.gameRules.map(m -> new GameRules(dataConfig.enabledFeatures(), m));
        return new LoadContext(this.getIdStr(),
                               new LevelSettings(levelName,
                                                 gameType,
                                                 new LevelSettings.DifficultySettings(difficulty, hardcore, locked),
                                                 true,
                                                 dataConfig),
                               this.preset,
                               this.dimensionsUpdater,
                               new WorldOptions(seed, generateStructures, generateBonusChest),
                               gameRules);
    }

    private static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> codec1,
            Function<C, T1> getter1,
            StreamCodec<? super B, T2> codec2,
            Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3,
            Function<C, T3> getter3,
            StreamCodec<? super B, T4> codec4,
            Function<C, T4> getter4,
            StreamCodec<? super B, T5> codec5,
            Function<C, T5> getter5,
            StreamCodec<? super B, T6> codec6,
            Function<C, T6> getter6,
            StreamCodec<? super B, T7> codec7,
            Function<C, T7> getter7,
            StreamCodec<? super B, T8> codec8,
            Function<C, T8> getter8,
            StreamCodec<? super B, T9> codec9,
            Function<C, T9> getter9,
            StreamCodec<? super B, T10> codec10,
            Function<C, T10> getter10,
            StreamCodec<? super B, T11> codec11,
            Function<C, T11> getter11,
            StreamCodec<? super B, T12> codec12,
            Function<C, T12> getter12,
            StreamCodec<? super B, T13> codec13,
            Function<C, T13> getter13,
            StreamCodec<? super B, T14> codec14,
            Function<C, T14> getter14,
            Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, C> constructor
    ) {
        return new StreamCodec<>()
        {
            @Override
            public C decode(B input) {
                T1 v1 = codec1.decode(input);
                T2 v2 = codec2.decode(input);
                T3 v3 = codec3.decode(input);
                T4 v4 = codec4.decode(input);
                T5 v5 = codec5.decode(input);
                T6 v6 = codec6.decode(input);
                T7 v7 = codec7.decode(input);
                T8 v8 = codec8.decode(input);
                T9 v9 = codec9.decode(input);
                T10 v10 = codec10.decode(input);
                T11 v11 = codec11.decode(input);
                T12 v12 = codec12.decode(input);
                T13 v13 = codec13.decode(input);
                T14 v14 = codec14.decode(input);
                return constructor.apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14);
            }

            @Override
            public void encode(B output, C value) {
                codec1.encode(output, getter1.apply(value));
                codec2.encode(output, getter2.apply(value));
                codec3.encode(output, getter3.apply(value));
                codec4.encode(output, getter4.apply(value));
                codec5.encode(output, getter5.apply(value));
                codec6.encode(output, getter6.apply(value));
                codec7.encode(output, getter7.apply(value));
                codec8.encode(output, getter8.apply(value));
                codec9.encode(output, getter9.apply(value));
                codec10.encode(output, getter10.apply(value));
                codec11.encode(output, getter11.apply(value));
                codec12.encode(output, getter12.apply(value));
                codec13.encode(output, getter13.apply(value));
                codec14.encode(output, getter14.apply(value));
            }
        };
    }
}
