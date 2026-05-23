package com.lnatit.womm.command;

import com.google.gson.JsonObject;
import com.lnatit.womm.data.TemplateManager;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.concurrent.CompletableFuture;

public class IdentityArgument implements ArgumentType<String>
{
    public static final IdentityArgument INSTANCE = new IdentityArgument();

    private static final StringArgumentType wrapped = StringArgumentType.greedyString();

    private IdentityArgument() {}

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return wrapped.parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (TemplateManager.INSTANCE.isEmpty()) {
            return Suggestions.empty();
        }
        for (String identity : TemplateManager.INSTANCE.getIdentities()) {
            if (identity.startsWith(builder.getRemaining())) {
                builder.suggest(identity);
            }
        }
        return builder.buildFuture();
    }

    public static class Info implements ArgumentTypeInfo<IdentityArgument, IdentityArgument.Info.Template>
    {
        public void serializeToNetwork(IdentityArgument.Info.Template template, FriendlyByteBuf out) {
        }

        public IdentityArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf in) {
            return new IdentityArgument.Info.Template();
        }

        public void serializeToJson(IdentityArgument.Info.Template template, JsonObject out) {
        }

        public IdentityArgument.Info.Template unpack(IdentityArgument argument) {
            return new IdentityArgument.Info.Template();
        }

        public final class Template implements ArgumentTypeInfo.Template<IdentityArgument>
        {
            public IdentityArgument instantiate(CommandBuildContext context) {
                return IdentityArgument.INSTANCE;
            }

            @Override
            public ArgumentTypeInfo<IdentityArgument, ?> type() {
                return IdentityArgument.Info.this;
            }
        }
    }
}
