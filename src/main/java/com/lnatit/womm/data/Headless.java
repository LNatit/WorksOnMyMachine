package com.lnatit.womm.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Optional;

public record Headless(
        Optional<String> identity,
        boolean alwaysRecreate,
        GameType gameType,
        Difficulty difficulty,
        boolean hardcore,
        boolean locked,
        WorldDataConfiguration dataConfig,
        Holder<WorldPreset> preset,
        Optional<Long> seed,
        boolean generateStructures,
        boolean generateBonusChest,
        Optional<GameRuleMap> gameRules) implements ITemplate<Optional<String>, Holder<WorldPreset>>
{
    private static final Codec<Holder<WorldPreset>> PRESET_CODEC = WorldPreset.DIRECT_CODEC.xmap(Holder::direct, Holder::value).withAlternative(WorldPreset.CODEC);
    public static final Codec<Headless> CODEC = ITemplate.codec(Codec.STRING::optionalFieldOf, PRESET_CODEC::fieldOf, Headless::new);

    public Template withId(Identifier identity, boolean abbreviatable) {
        ResourceKey<WorldPreset> key = this.preset.unwrapKey()
                .orElseThrow(() -> new IllegalStateException(
                        "Direct (inline) WorldPreset holders cannot be used as a Template preset; use a registry reference instead"));
        return new Template(
                identity,
                abbreviatable ,
                this.alwaysRecreate,
                this.gameType,
                this.difficulty,
                this.hardcore,
                this.locked,
                this.dataConfig,
                key,
                this.seed,
                this.generateStructures,
                this.generateBonusChest,
                this.gameRules
        );
    }
}
