package com.lnatit.womm.reference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public class A {
    Connection c;
    CreateWorldScreen cws;
    WorldGenSettings wgs;
    Minecraft mc;
    PauseScreen ps;
    MinecraftServer ms;
    WorldCreationContext wcc;
    GameRules grs;
}
