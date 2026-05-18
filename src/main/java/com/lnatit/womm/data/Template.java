package com.lnatit.womm.data;

import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;

public record Template(
        GameType gameMode,
        LevelSettings.DifficultySettings difficultySettings,

)
{
}
