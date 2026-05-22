package com.lnatit.womm.data;

import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Optional;

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
}
