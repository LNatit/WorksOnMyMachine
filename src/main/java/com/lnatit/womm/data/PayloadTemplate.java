package com.lnatit.womm.data;

import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record PayloadTemplate(
        String identity,
        Gameplay gameplay,
        Optional<GameRules> gameRules,
        Optional<ContentConfig> contentConfig,
        Optional<WorldGen> worldGen
) {
    private static final Pattern IDENTITY_PATTERN = Pattern.compile("^[a-z0-9._-]+$");

    public PayloadTemplate {
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException("identity must not be blank");
        }
        if (!IDENTITY_PATTERN.matcher(identity).matches()) {
            throw new IllegalArgumentException("identity must be lower-case and match [a-z0-9._-]+");
        }
        gameRules = Objects.requireNonNullElse(gameRules, Optional.empty());
        contentConfig = Objects.requireNonNullElse(contentConfig, Optional.empty());
        worldGen = Objects.requireNonNullElse(worldGen, Optional.empty());
    }

    public record Gameplay(
            GameType gameMode,
            LevelSettings.DifficultySettings difficultySettings
    ) {
    }

    public record ContentConfig(
            List<String> enabledDataPacks,
            List<String> disabledDataPacks,
            List<String> featureFlags
    ) {
        public ContentConfig {
            enabledDataPacks = enabledDataPacks == null ? List.of() : List.copyOf(enabledDataPacks);
            disabledDataPacks = disabledDataPacks == null ? List.of() : List.copyOf(disabledDataPacks);
            featureFlags = featureFlags == null ? List.of() : List.copyOf(featureFlags);
        }
    }

    public record WorldGen(WorldOptions worldOptions, Dimensions dimensions) {
        public WorldGen {
            if (worldOptions == null) {
                throw new IllegalArgumentException("worldOptions must not be null when worldGen is present");
            }
            if (dimensions == null) {
                throw new IllegalArgumentException("dimensions must not be null when worldGen is present");
            }
        }
    }

    public record WorldOptions(long seed, boolean generateStructures, boolean bonusChest) {
    }

    public record Dimensions(String mode, String presetId, Optional<String> templateWorld) {
        public Dimensions {
            if (mode == null || mode.isBlank()) {
                throw new IllegalArgumentException("dimensions.mode must not be blank");
            }
            presetId = presetId == null ? "minecraft:normal" : presetId;
            templateWorld = Objects.requireNonNullElse(templateWorld, Optional.empty());
        }
    }
}
