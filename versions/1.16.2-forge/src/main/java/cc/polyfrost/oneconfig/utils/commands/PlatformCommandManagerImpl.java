/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.arguments.ArgumentParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlatformCommandManagerImpl extends PlatformCommandManager {

    final HashMap<Class<?>, Pair<ArgumentType<Object>, ArgumentType<Object>>> parsers = new HashMap<>(); // non-greedy, greedy

    @Override
    void createCommand(CommandManager.OCCommand root) {

        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(root.getMetadata().value());
        builder.executes((context ->
        {
            try {
                String[] result = root.doCommand(context.getInput().split(" "));
                if (result.length != 0 && result[0] != null) {
                    for (String s : result) {
                        UChat.chat(s);
                    }
                }
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                UChat.chat(CommandManager.METHOD_RUN_ERROR.replace("@ROOT_COMMAND@", root.getMetadata().value()));
                return -1;
            }
        }));
    }

    @Override
    Collection<String> getPlayerNames() {
        if (Minecraft.getInstance() != null && Minecraft.getInstance().getCurrentServerData() != null) {
            return Minecraft.getInstance().getCurrentServerData().playerList.stream().map(ITextComponent::getString).collect(Collectors.toList());
        } else return null;
    }


    // TODO wyvest can you check this please thanks because i genuinely have no clue how brigadier works
    @Override
    public void handleNewParser(ArgumentParser<?> parser, Class<?> clazz) {
        parsers.put(clazz, new ImmutablePair<ArgumentType<Object>, ArgumentType<Object>>(new ArgumentType() {

            @Override
            public Object parse(StringReader reader) {
                final String text = reader.getRemaining();
                reader.setCursor(reader.getTotalLength());
                try {
                    return parser.parse(text);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
                return ArgumentType.super.listSuggestions(context, builder);
            }
        }, new ArgumentType() {

            @Override
            public Object parse(StringReader reader) {
                final String text = reader.getRemaining();
                reader.setCursor(reader.getTotalLength());
                try {
                    return parser.parse(text);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
                return ArgumentType.super.listSuggestions(context, builder);
            }
        }));
    }
}
