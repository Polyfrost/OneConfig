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

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.commands.v1.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.AnnotationCommandFactory;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.annotations.Command;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.annotations.Parameter;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationTest {
    @Test
    void test() {
        CommandTree tree = new AnnotationCommandFactory().create(ArgumentParser.defaultParsers, new TestCommand());
        assertNotNull(tree);
        tree.init();

        tree.execute("test");
        tree.execute("test3", "1", "2", "3.0", "4.0", "bobFISH");
        tree.execute("test4", "test7", "test9", "1", "2", "3", "4", "5.5", "6.25", "chicken");

        assertEquals(3, tree.execute("test2", "1", "2"));
        assertEquals(0, tree.execute("test2", "aaaaa", "2", "hey", "my", "name", "is", "jeff"));
        assertEquals(60L, tree.execute("test60", "10", "30", "20"));
        assertNull(tree.execute("test"));

        assertThrows(Exception.class, () -> tree.execute("test2", "1", "4idahui"));
        assertThrows(Exception.class, () -> tree.execute("test", "aaaaa"));

        tree.execute();
        tree.execute("32", "53");
        tree.execute("test4");
        for (String s : tree.getHelp()) {
            System.out.println(s);
        }
        System.out.println("OK");
    }


    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    @Command("testing")
    public static class TestCommand {
        @Command(value = {"test", "t"}, description = "Test command")
        public static void test() {
            System.out.println("hey");
        }

        @Command("")
        public void perform() {
            System.out.println("main method a");
        }

        @Command("")
        public void perform(int a, int b) {
            System.out.println("main method with args " + (a + b));
        }

        @Command(value = {"test2", "t2"}, description = "Test command 2")
        int test2(int a, int b) {
            System.out.println("hey2");
            return a + b;
        }

        @Command
        long test60(@Parameter(arity = 3) long[] a) {
            System.out.println("hey60");
            return a[0] + a[1] + a[2];
        }

        @Command(greedy = true)
        int test2(String a, int b, String[] c) {
            System.out.println(a);
            System.out.println(String.join(" ", c));
            return 0;
        }

        @Command(value = {"test3", "t3"}, description = "Test command 3")
        private void test3(@Parameter(value = "an integer", description = "the first thing to add") int a, Integer b, Double c, double d, String e) {
            System.out.println(a + b);
            System.out.println(c + d);
            System.out.println(e);
        }

        @Command(value = {"test4", "t4"}, description = "Test command 4")
        private static class TestClass {
            @Command(value = {"test6", "t6"}, description = "Test command 6")
            public static void test6() {
                System.out.println("hey6");
            }

            @Command("")
            public void test23() {
                System.out.println("main method");
            }

            @Command(value = {"test5", "t5"}, description = "Test command 5")
            public void test5() {
                System.out.println("hey5");
            }

            @Command(value = {"test7", "t7"}, description = "Test command 7")
            private class TestInnerClass {
                @Command(value = {"test8", "t8"}, description = "Test command 8")
                public void test8() {
                    System.out.println("hey8");
                }

                @Command(value = {"test9", "t9"}, description = "Test command 9")
                public void test9(Long a, long b, Integer c, int d, Double e, double f, String g) {
                    System.out.println(a + b);
                    System.out.println(c + d);
                    System.out.println(e + f);
                    System.out.println(g);
                }
            }
        }
    }
}
