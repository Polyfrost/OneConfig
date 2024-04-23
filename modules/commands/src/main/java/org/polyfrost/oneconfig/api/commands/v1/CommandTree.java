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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandCreationException;
import org.polyfrost.oneconfig.api.commands.v1.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandExecutionException;
import org.polyfrost.oneconfig.api.commands.v1.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Internal representation of a command in OneConfig.
 */
@SuppressWarnings("unused")
public class CommandTree implements Node {
    final String[] names;
    private final Map<String, List<Node>> commands = new HashMap<>();
    public String description;
    private Map<String, List<Node>> dedupedCommands = null;
    private String[] help = null;
    private boolean init = false;

    public CommandTree(@NotNull String[] names, @Nullable String description) {
        this.names = names;
        this.description = description;
    }

    /**
     * Return a copy of in with all elements that are in any of the lists removed.
     * <br>both lists and in are not modified.
     */
    @Contract(pure = true)
    static <T> @NotNull List<T> cullList(@NotNull Iterable<List<T>> lists, @NotNull List<T> in) {
        List<T> out = new ArrayList<>(1);
        for (T obj : in) {
            boolean found = false;
            for (List<T> list : lists) {
                if (list.contains(obj)) {
                    found = true;
                    break;
                }
            }
            if (!found) out.add(obj);
        }
        return out;
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

    public void setDescription(String description) {
        this.description = description;
        if (init) {
            help = null;
            getHelp();
        }
    }

    public String description() {
        return description;
    }

    public boolean isInitialized() {
        return init;
    }

    private void put(String key, Executable node) {
        if (init) throw new CommandCreationException("Cannot add executables after initialization!");
        if (key.equals("main")) {
            key = "";
        }
        if (!commands.containsKey(key)) {
            List<Node> nodes = new ArrayList<>(1);
            nodes.add(node);
            commands.put(key, nodes);
        } else {
            commands.get(key).add(node);
        }
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

    /**
     * Return the command map without duplicate entries.
     */
    public Map<String, List<Node>> getDedupedCommands() {
        if (dedupedCommands == null) {
            if (!init) throw new CommandCreationException("Command not initialized!");
            Map<String, List<Node>> out = new HashMap<>();
            for (Map.Entry<String, List<Node>> entry : commands.entrySet()) {
                List<Node> nodes = cullList(out.values(), entry.getValue());
                if (!nodes.isEmpty()) out.put(entry.getKey(), nodes);
            }
            dedupedCommands = out;
        }
        return dedupedCommands;
    }

    @NotNull
    public String[] getHelp() {
        if (help == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Help for /").append(String.join(", /", names)).append(": ");
            if (description != null) sb.append(description);
            new TreeMap<>(getDedupedCommands()).values().forEach((ls) -> {
                for (Node value : ls) {
                    if (value instanceof Executable) {
                        sb.append("\n  ").append(value.helpString());
                    } else {
                        fullString((CommandTree) value, 1, sb, true);
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
    }

    public String name() {
        return names[0];
    }

    public String[] names() {
        return names;
    }

    @Override
    public String toString() {
        return "CommandTree" + Arrays.toString(names) + (description == null ? "" : ": " + description);
    }

    private void fullString(CommandTree it, int depth, StringBuilder sb, boolean isHelp) {
        sb.append("\n");
        append(sb, "  ", depth);
        sb.append(isHelp ? it.helpString() : it);
        it.getDedupedCommands().values().forEach((ls) -> {
            for (Node value : ls) {
                if (value instanceof Executable) {
                    sb.append("\n");
                    append(sb, "  ", depth + 2);
                    sb.append(isHelp ? value.helpString() : value);
                } else {
                    fullString((CommandTree) value, depth + 1, sb, isHelp);
                }
            }
        });
    }

    public String helpString() {
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
    public Pair<@Nullable List<Node>, @NotNull String[]> getWithArgs(String... path) {
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
        return new Pair<>(ls, rest);
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
        Pair<List<Node>, String[]> pair = getWithArgs(usage);
        if (pair.first == null) throw new CommandExecutionException("Command not found!");
        String[] args = pair.second;
        List<Node> ls = pair.first.stream().filter(node -> node instanceof Executable && (((Executable) node).isGreedy || ((Executable) node).arity == args.length))
                .collect(Collectors.toList());
        if (ls.isEmpty()) throw new CommandExecutionException("Command not found!");
        if (ls.size() == 1) {
            Executable exe = (Executable) ls.get(0);
            return exe.execute(args);
        }
        loop:
        for (Node node : ls) {
            Executable exe = (Executable) node;
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
        Pair<List<Node>, String[]> res = getWithArgs(current);
        if (res == null) {
            String[] last = new String[current.length - 1];
            System.arraycopy(current, 0, last, 0, last.length);
            // move back, check if we have something valid available on the last
            // like /hello gj where gj is not valid, check /hello for if it can match any
            n = getTree(last);
            if (n == null) return null;
        } else {
            n = res.first.get(0);
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
            if (res.second.length == 0) thisArg = "";
            Executable e = (Executable) n;
            // fast path: we already past the end of this command, don't try
            if (res.second.length > e.parameters.length || e.parameters.length == 0) return null;
            Executable.Param param = e.parameters[Math.max(0, res.second.length - 1)];
            List<String> l = param.tryAutoComplete(thisArg);
            if (l == null || l.isEmpty()) return null;
            if (l.contains(thisArg)) {
                // returned same, move onto next arg
                return autocomplete(withEmpty(current));
            } else return l;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandTree that = (CommandTree) o;
        return Arrays.equals(names, that.names) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + Arrays.hashCode(names);
        return result;
    }
}
