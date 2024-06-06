/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.commands.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.commands.v1.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandCreationException;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandExecutionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Internal representation of a command in OneConfig.
 */
@SuppressWarnings("unused")
// todo: change the main method so that the CommandTree itself has a List<Executable> attached to it instead
public class CommandTree extends Node {
    private final Map<String, List<Node>> commands = new HashMap<>();
    private final Map<String, List<Node>> commandsNoDupe = new HashMap<>();
    private String[] help = null;
    private boolean init = false;

    public CommandTree(@NotNull String[] names, @Nullable String description) {
        super(names, description);
    }

    public static void append(StringBuilder sb, String toAppend, int amount) {
        for (int i = 0; i < amount; i++) {
            sb.append(toAppend);
        }
    }

    static <T> boolean contains(T[] array, T item) {
        for (T x : array) {
            if (x.equals(item)) return true;
        }
        return false;
    }

    static String[] withEmpty(String[] current) {
        String[] out = new String[current.length + 1];
        System.arraycopy(current, 0, out, 0, current.length);
        out[current.length] = "";
        return out;
    }

    private static void _getHelp(CommandTree it, int depth, StringBuilder sb) {
        sb.append('\n');
        append(sb, "  ", depth);
        sb.append(it);
        it.commandsNoDupe.values().forEach((ls) -> {
            for (Node value : ls) {
                if (value instanceof Executable) {
                    sb.append('\n');
                    append(sb, "  ", depth + 2);
                    sb.append(value);
                } else {
                    _getHelp((CommandTree) value, depth + 1, sb);
                }
            }
        });
    }

    @Override
    public void setDescription(String description) {
        super.setDescription(description);
        if (init) {
            help = null;
            getHelp();
        }
    }

    public Map<String, List<Node>> getDedupedCommands() {
        if (!init) throw new CommandCreationException("Cannot get deduped commands before initialization!");
        return commandsNoDupe;
    }

    public boolean isInitialized() {
        return init;
    }

    private void put(String key, Executable node) {
        if (init) throw new CommandCreationException("Cannot add executables after initialization!");
        if (key.equals("main")) {
            key = "";
        }
        commands.computeIfAbsent(key, k -> new ArrayList<>(1)).add(node);
    }

    private void put(String key, CommandTree tree) {
        if (init) throw new CommandCreationException("Cannot add trees after initialization!");
        if (commands.put(key, Collections.singletonList(tree)) != null)
            throw new CommandCreationException("Invalid command: " + this + ": see https://docs.polyfrost.org/oneconfig/commands/overloaded-subcommand");
    }

    public void put(Executable executable) {
        for (String name : executable.names) {
            put(name, executable);
        }
        commandsNoDupe.computeIfAbsent(executable.name(), k -> new ArrayList<>(1)).add(executable);
    }

    public void init() {
        // todo optimize: replace arraylists with singleton lists where applicable?
        if (init) return;
        init = true;
        List<Executable> executables = new ArrayList<>(5);
        for (Map.Entry<String, List<Node>> entry : commands.entrySet()) {
            for (Node node : entry.getValue()) {
                if (node instanceof CommandTree) {
                    ((CommandTree) node).init();
                } else {
                    executables.add((Executable) node);
                }
            }
            if (executables.size() > 1) {
                for (int i = 0; i < executables.size(); i++) {
                    for (int j = i + 1; j < executables.size(); j++) {
                        Executable a = executables.get(i);
                        Executable b = executables.get(j);
                        if (a.parameters.length != b.parameters.length) continue;
                        ArgumentParser<?>[] aParsers = Arrays.stream(a.parameters).map(p -> p.parser).toArray(ArgumentParser[]::new);
                        ArgumentParser<?>[] bParsers = Arrays.stream(b.parameters).map(p -> p.parser).toArray(ArgumentParser[]::new);

                        boolean same = true;
                        for (int k = 0; k < aParsers.length; k++) {
                            if (aParsers[k] != bParsers[k]) {
                                same = false;
                                break;
                            }
                        }
                        if (same)
                            throw new CommandCreationException("Ambiguous command overload: " + a.names[0] + ": see https://docs.polyfrost.org/oneconfig/commands/ambiguous-overload");
                    }
                }
            }
            executables.clear();
        }
    }

    @NotNull
    public String[] getHelp() {
        if (help == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Help for /").append(String.join(", /", names)).append(": ");
            if (description != null) sb.append(description);
            new TreeMap<>(commandsNoDupe).values().forEach((ls) -> {
                for (Node value : ls) {
                    if (value instanceof Executable) {
                        sb.append("\n  ").append(value);
                    } else {
                        _getHelp((CommandTree) value, 1, sb);
                    }
                }
            });
            String s = sb.toString();
            help = s.split("\n");
        }
        return help;
    }

    public void put(CommandTree sub) {
        for (String name : sub.names) {
            put(name, sub);
        }
        commandsNoDupe.put(sub.name(), Collections.singletonList(sub));
    }

    @Override
    public String toString() {
        return String.join(", ", names) + (description == null ? "" : ": " + description);
    }

    public List<Node> get(String path) {
        return commands.get(path);
    }

    /**
     * Get a list of all matching nodes.
     */
    public @Nullable List<Node> get(String... path) {
        CommandTree self = this;
        List<Node> ls = null;
        for (String s : path) {
            List<Node> res = self.get(s);
            if (res != null) {
                ls = res;
            } else return null;
            for (Node node : ls) {
                if (node instanceof CommandTree) {
                    self = (CommandTree) node;
                    break;
                }
            }
        }
        return ls;
    }

    /**
     * Get a list of all matching nodes and the remaining arguments.<br>
     * This method has additional checks so that it prefers to return the default command if it is available.<br>
     * The returned list will be null if no matching nodes were found. The returned array will be empty if there are no remaining arguments.
     *
     * @see #get(String...)
     */
    public Result getWithArgs(String... path) {
        CommandTree self = this;
        List<Node> ls = null;
        int i = 1;
        for (String s : path) {
            List<Node> res = self.get(s);
            if (res != null) {
                ls = res;
            } else {
                // asm: attempt to grab the default command on this tree, if we have not found any yet
                if (self.get("") != null && ls == null) {
                    ls = self.get("");
                    i--;
                } else {
                    break;
                }
            }
            for (Node node : ls) {
                if (node instanceof CommandTree) {
                    self = (CommandTree) node;
                    i++;
                    break;
                }
            }
        }
        // asm: if this is trying to return a tree, check if there is a main method on it, and return that instead.
        if (ls != null && ls.size() == 1) {
            Node n = ls.get(0);
            if (n instanceof CommandTree) {
                List<Node> nodes = ((CommandTree) n).get("");
                if (nodes != null) {
                    ls = nodes;
                    i--;
                }
            }
        }
        String[] rest = new String[Math.max(0, path.length - i)];
        if (rest.length != 0) System.arraycopy(path, i, rest, 0, rest.length);
        return new Result(ls, rest);
    }

    /**
     * Perform a function on all {@link Executable}s matching the given path.
     * <br>
     * Due to how trees function, it is more expensive to get a list of executables, so this is provided as an alternative.
     */
    public void onExecs(Consumer<Executable> func, String... path) {
        List<Node> ls = get(path);
        if (ls == null) return;
        for (Node node : ls) {
            if (node instanceof Executable) {
                func.accept((Executable) node);
            }
        }
    }

    /**
     * Execute the given command. This method will dispatch to the correct {@link Executable} based on the given arguments.
     *
     * @param usage the full path and arguments to the command
     * @return the result of the command
     * @throws CommandExecutionException if the command could not be found or if the arguments were invalid, or if the command threw an exception
     */
    public Object execute(String... usage) {
        if (usage.length == 0) usage = new String[]{""};
        Result res = getWithArgs(usage);
        if (res.nodes == null || res.nodes.isEmpty()) throw new CommandExecutionException("Command not found!");
        List<Node> nodes = res.nodes;
        String[] args = res.args;
        loop:
        for (Node node : nodes) {
            if (!(node instanceof Executable)) continue;
            Executable exe = (Executable) node;
            if (!exe.isGreedy && args.length != exe.arity) continue;
            if (exe.arity == 0 && args.length == 0) {
                return exe.execute();
            }
            int offset = 0;
            for (int i = 0; i < exe.parameters.length; i++) {
                Executable.Param p = exe.parameters[i];
                if (p.parsedOrNull(offset, args) == null) continue loop;
                offset += p.arity;
            }
            return exe.execute(args);
        }
        throw new CommandExecutionException("Command not found!");
    }

    public CommandTree getTree(String... path) {
        List<Node> ls = get(path);
        if (ls == null) return null;
        for (Node node : ls) {
            if (node instanceof CommandTree) {
                return (CommandTree) node;
            }
        }
        return null;
    }

    /**
     * Return a list of valid autocompletion options for a given path, with arguments.
     *
     * @return the list of args, or null
     * @see #getWithArgs(String...)
     * @see #execute(String...)
     */
    public List<String> autocomplete(String... current) {
        if (current.length == 0) return null;
        Node n;
        String thisArg = current[current.length - 1];
        Result res = getWithArgs(current);
        if (res.nodes == null) {
            String[] last = new String[current.length - 1];
            System.arraycopy(current, 0, last, 0, last.length);
            // move back, check if we have something valid available on the last
            // like /hello gj where gj is not valid, check /hello for if it can match any
            n = getTree(last);
            if (n == null) return null;
        } else {
            n = res.nodes.get(0);
        }
        if (n instanceof CommandTree) {
            CommandTree c = (CommandTree) n;
            Set<String> cmds = c.commands.keySet();
            // same, move onto next arg
            if (contains(c.names, thisArg)) thisArg = "";
            List<String> ls = new ArrayList<>(cmds.size());
            for (String s : cmds) {
                if (s.startsWith(thisArg)) ls.add(s);
            }
            return ls.isEmpty() ? null : ls;
        } else {
            if (res.args.length == 0) thisArg = "";
            Executable e = (Executable) n;
            // fast path: we already past the end of this command, don't try
            if (res.args.length > e.parameters.length || e.parameters.length == 0) return null;
            Executable.Param param = e.parameters[Math.max(0, res.args.length - 1)];
            List<String> l = param.tryAutoComplete(thisArg);
            if (l == null || l.isEmpty()) return null;
            if (l.contains(thisArg)) {
                // returned same, move onto next arg
                return autocomplete(withEmpty(current));
            } else return l;
        }
    }

    /**
     * Represents a result from a {@link #getWithArgs(String...)} call. This class is a wrapper around a set of possible candidates for the given path, and their arguments.
     */
    public static final class Result {
        @Nullable
        public final List<@NotNull Node> nodes;
        @NotNull
        public final String[] args;

        Result(@Nullable List<@NotNull Node> nodes, @NotNull String[] args) {
            this.nodes = nodes;
            this.args = args;
        }
    }
}
