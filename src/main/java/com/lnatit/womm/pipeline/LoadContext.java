package com.lnatit.womm.pipeline;

import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Optional;

public record LoadContext(String identity,
                          LevelSettings levelSettings,
                          WorldPreset preset,
                          WorldOptions options,
                          Optional<GameRules> gameRules)
{
    String worldName() {
        return identity;
    }
}
