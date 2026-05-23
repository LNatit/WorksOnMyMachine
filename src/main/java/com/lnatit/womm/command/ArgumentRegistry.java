package com.lnatit.womm.command;

import com.lnatit.womm.WOMM;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface ArgumentRegistry
{
    DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, WOMM.MODID);
    DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<IdentityArgument, ?>> IDENTITY =
            ARGUMENT_TYPES.register("identity", () -> ArgumentTypeInfos.registerByClass(IdentityArgument.class, new IdentityArgument.Info()));
}
