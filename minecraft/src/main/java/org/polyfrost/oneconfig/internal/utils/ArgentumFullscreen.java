package org.polyfrost.oneconfig.internal.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ArgentumFullscreen {
    private static final MethodHandle IS_EXCLUSIVE;
    private static final MethodHandle SET_EXCLUSIVE;

    static {
        MethodHandle get = null, set = null;
        try {
            Class<?> api = Class.forName("dev.rdh.argentum.api.config.FullscreenSettings");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            get = lookup.findStatic(api, "isExclusive", MethodType.methodType(boolean.class));
            set = lookup.findStatic(api, "setExclusive", MethodType.methodType(void.class, boolean.class));
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        IS_EXCLUSIVE = get;
        SET_EXCLUSIVE = set;
    }

    private ArgentumFullscreen() {}

    public static Boolean isExclusive() {
        if (IS_EXCLUSIVE == null) return null;
        try {
            return (boolean) IS_EXCLUSIVE.invokeExact();
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setExclusive(boolean exclusive) {
        if (SET_EXCLUSIVE == null) return;
        try {
            SET_EXCLUSIVE.invokeExact(exclusive);
        } catch (Throwable ignored) {
        }
    }
}
