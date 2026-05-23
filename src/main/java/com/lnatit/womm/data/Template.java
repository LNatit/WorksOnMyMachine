package com.lnatit.womm.data;

import com.lnatit.womm.pipeline.LoadContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

public record Template(String identity,
                       boolean alwaysRecreate,
                       GameType gameType,
                       Difficulty difficulty,
                       boolean hardcore,
                       boolean locked,
                       WorldDataConfiguration dataConfig,
                       ResourceKey<WorldPreset> preset,
                       Optional<Long> seed,
                       boolean generateStructures,
                       boolean generateBonusChest,
                       Optional<GameRuleMap> gameRules) implements ITemplate<String, ResourceKey<WorldPreset>>
{
    private static final DateTimeFormatter LEVEL_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static final StreamCodec<RegistryFriendlyByteBuf, Template> STREAM_CODEC =
            ITemplate.streamCodec(ByteBufCodecs.STRING_UTF8, StreamCodecs.PRESET_KEY, Template::new);

    public LoadContext assemble() {
        String levelName = "WOMM-" + identity;
        if (this.alwaysRecreate) {
            levelName = levelName + "-" + LocalDateTime.now().format(LEVEL_NAME_TIMESTAMP);
        }
        long seed = this.seed.orElse(WorldOptions.randomSeed());
        Optional<GameRules> gameRules = this.gameRules.map(m -> new GameRules(dataConfig.enabledFeatures(), m));
        return new LoadContext(this.identity,
                               new LevelSettings(levelName,
                                                 gameType,
                                                 new LevelSettings.DifficultySettings(difficulty, hardcore, locked),
                                                 true,
                                                 dataConfig),
                               this.preset,
                               new WorldOptions(seed, generateStructures, generateBonusChest),
                               gameRules);
    }
}
