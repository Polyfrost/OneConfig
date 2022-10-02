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

import cc.polyfrost.oneconfig.Test;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.utils.commands.annotations.Description;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static cc.polyfrost.oneconfig.utils.commands.ClientCommandManager.*;
import static cc.polyfrost.oneconfig.utils.commands.CommandManager.DELIMITER;
import static cc.polyfrost.oneconfig.utils.commands.CommandManager.MAIN_METHOD_NAME;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class PlatformCommandManagerImpl extends PlatformCommandManager {

	private static final FabricClientCommandSource ADAPTER = new FabricClientCommandSource() {
		@Override
		public void sendFeedback(Text message) {
			System.out.println(message.getString());
		}

		@Override
		public void sendError(Text message) {
			System.err.println(message.asString());
		}

		@Override
		public MinecraftClient getClient() {
			return null;
		}

		@Override
		public ClientPlayerEntity getPlayer() {
			return null;
		}

		@Override
		public ClientWorld getWorld() {
			return null;
		}

		@Override
		public Collection<String> getPlayerNames() {
			return null;
		}

		@Override
		public Collection<String> getTeamNames() {
			return null;
		}

		@Override
		public Collection<Identifier> getSoundIds() {
			return null;
		}

		@Override
		public Stream<Identifier> getRecipeIds() {
			return null;
		}

		@Override
		public CompletableFuture<Suggestions> getCompletions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
			return null;
		}

		@Override
		public Set<RegistryKey<World>> getWorldKeys() {
			return null;
		}

		@Override
		public DynamicRegistryManager getRegistryManager() {
			return null;
		}

		@Override
		public boolean hasPermissionLevel(int level) {
			return false;
		}
	};

	static {
		Test.register(DISPATCHER);
	}

	@Override
	void createCommand(CommandManager.OCCommand cmd) {
		try {
			final LiteralArgumentBuilder<FabricClientCommandSource> master = literal(cmd.getMetadata().value());
			if (cmd.mainMethod != null) {
				master.executes(context -> {
					chat(cmd.mainMethod.invoke());
					return 1;
				});
			} else {
				master.executes(context -> {
					chat(cmd.helpCommand);
					return 1;
				});
			}

			final LiteralCommandNode<FabricClientCommandSource> root = DISPATCHER.register(master);
			//root.addChild(createHelpNode(cmd));
			// register aliases
			for (Map.Entry<String[], CommandManager.InternalCommand> entry : cmd.commandsMap.entrySet()) {
				if (entry.getValue().getName().equals(MAIN_METHOD_NAME)) continue;
				for (String path : entry.getKey()) {
					root.addChild(createSubNode(path, entry.getValue()));
				}
			}
			for (String alias : cmd.getMetadata().aliases()) {
				DISPATCHER.register(literal(alias).redirect(root));
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to register command " + cmd.getMetadata().value(), e);
		}
	}

	@Override
	Collection<String> getPlayerNames() {
		if (UMinecraft.getPlayer() != null && UMinecraft.getPlayer().networkHandler != null)
			return UMinecraft.getPlayer().networkHandler.getCommandSource().getPlayerNames();
		return null;
	}

	private static LiteralCommandNode<FabricClientCommandSource> createSubNode(String p, CommandManager.InternalCommand cmd) {
		// Literal paths
		System.out.println("-------------------");
		String[] paths = p.split(DELIMITER);
		LiteralArgumentBuilder<FabricClientCommandSource> lastLiteral = literal(paths[0]);
		LiteralArgumentBuilder<FabricClientCommandSource> rootLiteral = lastLiteral;
		for (int i = 1; i < paths.length; i++) {
			LiteralArgumentBuilder<FabricClientCommandSource> current = literal(paths[i]);

			System.out.println(lastLiteral.getLiteral() + " then " + current.getLiteral());
			lastLiteral.then(current);
			lastLiteral = current;
		}

		// Parameters
		RequiredArgumentBuilder<FabricClientCommandSource, ?> lastParam = null;
		RequiredArgumentBuilder<FabricClientCommandSource, ?> rootParam = null;
		for (Parameter parameter : cmd.getUnderlyingMethod().getParameters()) {
			RequiredArgumentBuilder<FabricClientCommandSource, ?> current = argument(
					getArgName(parameter),
					parameter.isAnnotationPresent(Greedy.class)
							? greedyString()
							: word()
			);
			if (lastParam != null) {
				System.out.println(lastParam.getName() + " then " + current.getName());
				lastParam.then(current);
			} else {
				rootParam = current;
			}
			lastParam = current;
		}

		Command<FabricClientCommandSource> command = context -> {
			if (cmd.getUnderlyingMethod().getParameterCount() == 0) {
				chat(cmd.invoke());
			} else {
				String[] args = new String[cmd.getUnderlyingMethod().getParameterCount()];
				for (int i = 0; i < args.length; i++) {
					Parameter parameter = cmd.getUnderlyingMethod().getParameters()[i];
					args[i] = context.getArgument(getArgName(parameter), String.class);
				}
				chat(cmd.invoke(args));
			}
			return 1;
		};

		System.out.println("First literal : " + rootLiteral.getLiteral());
		System.out.println("Builder (last): " + lastLiteral.getLiteral());
		System.out.println("First param: " + (rootParam != null ? rootParam.getName() : null));
		System.out.println("Last param : " + (lastParam != null ? lastParam.getName() : null));
		if (lastParam != null) {
			System.out.println("executes(command) then(rootParam)");
			lastParam.executes(command);
			lastLiteral.then(rootParam);
		} else {
			System.out.println("executes(command)");
			lastLiteral.executes(command);
		}
		return rootLiteral.build();
	}

	private static String getArgName(Parameter parameter) {
		return parameter.isAnnotationPresent(Description.class)
				? parameter.getAnnotation(Description.class).value()
				: parameter.getType().getSimpleName() + "@" + parameter.getName().hashCode();
	}

	private static LiteralCommandNode<FabricClientCommandSource> createHelpNode(CommandManager.OCCommand cmd) {
		// TODO
		return null;
	}

	private static void chat(String... in) {
		if (in == null) return;
		for (String s : in) {
			if (s != null) {
				UChat.chat(s);
			}
		}
	}
}
