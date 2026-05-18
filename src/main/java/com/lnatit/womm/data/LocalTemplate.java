package com.lnatit.womm.data;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.storage.LevelDataAndDimensions;

public record LocalTemplate(String id, ReloadableServerResources resources, LayeredRegistryAccess<RegistryLayer> layers, LevelDataAndDimensions.WorldDataAndGenSettings settings)
{
}
