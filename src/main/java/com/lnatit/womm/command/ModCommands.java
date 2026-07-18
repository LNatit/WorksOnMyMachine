package com.lnatit.womm.command;

import com.lnatit.womm.WOMM;
import com.lnatit.womm.WOMMPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WOMM.MODID)
public interface ModCommands
{
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("womm")
                                    .then(Commands.argument("identity", IdentityArgument.INSTANCE).executes(ModCommands::runId)));
    }

    static int runId(CommandContext<CommandSourceStack> context) {
        WOMM.LOGGER.debug("Test command executed!");
        CommandSourceStack source = context.getSource();
        if (source.isPlayer()) {
            assert source.getPlayer() != null;
            PacketDistributor.sendToPlayer(source.getPlayer(), new WOMMPayload(StringArgumentType.getString(context, "identity")));
            WOMM.LOGGER.debug("Payload sent!");
        }
        return 0;
    }
}
