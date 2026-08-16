package com.lnatit.womm.pipeline;

import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import javax.annotation.Nullable;
import java.util.Optional;

public record LoadContext(String identity,
                          LevelSettings levelSettings,
                          ResourceKey<WorldPreset> preset,
                          Optional<ResourceKey<?>> dimensionsUpdater,
                          WorldOptions options,
                          Optional<GameRules> gameRules)
{
    String worldName() {
        return levelSettings.levelName();
    }

    @Nullable
    WorldCreationContext.DimensionsUpdater unwrapUpdater(HolderLookup.Provider provider) {
        if (dimensionsUpdater.isPresent()) {
            var holder = provider.get(dimensionsUpdater.get());
            if (holder.isPresent()) {
                if (holder.get().value() instanceof FlatLevelGeneratorPreset) {
                    return PresetEditor.flatWorldConfigurator(((FlatLevelGeneratorPreset) holder.get().value()).settings());
                }
                else if (holder.get().value() instanceof Biome) {
                    return PresetEditor.fixedBiomeConfigurator((Holder<Biome>) holder.get());
                }
            }
        }
        return null;
    }
}
