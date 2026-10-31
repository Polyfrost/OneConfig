package org.polyfrost.oneconfig.internal.compat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigEntrypointsTest {
    private static final StackTraceElement LISTENER = frame(ConfigEntrypoints.class, "onConfigSubmitted");
    private static final StackTraceElement SUBMIT = frame(Object.class, "submitForInitialization");
    private static final StackTraceElement ENTRYPOINT = frame(Object.class, "registerConfigs");
    private static final StackTraceElement BOTTOM = frame(Thread.class, "run");
    private static final StackTraceElement HELPER = frame(Object.class, "helper");
    private static final StackTraceElement REGISTER = frame(ConfigEntrypoints.class, "register");

    @Test
    void depthIsTheOutermostConstructorFrame() {
        StackTraceElement[] frames = {LISTENER, SUBMIT, init(Config.class), init(Base.class), ENTRYPOINT, BOTTOM};
        // counted from the bottom, so the frame of Base.<init> sits two above it
        assertEquals(2, ConfigEntrypoints.constructorDepth(Base.class, frames));
    }

    @Test
    void depthFollowsTheWholeConstructorChain() {
        StackTraceElement[] frames = {LISTENER, SUBMIT, init(Config.class), init(Base.class), init(Derived.class), ENTRYPOINT, BOTTOM};
        assertEquals(2, ConfigEntrypoints.constructorDepth(Derived.class, frames));
    }

    @Test
    void delegatingConstructorsCountAsOneConstruction() {
        StackTraceElement[] frames = {LISTENER, SUBMIT, init(Config.class), init(Base.class), init(Base.class), ENTRYPOINT, BOTTOM};
        assertEquals(2, ConfigEntrypoints.constructorDepth(Base.class, frames));
    }

    @Test
    void anInstanceBuiltInsideAnotherConstructorGetsItsOwnDepth() {
        StackTraceElement[] frames = {LISTENER, SUBMIT, init(Config.class), init(Base.class), HELPER, init(Derived.class), ENTRYPOINT, BOTTOM};
        assertEquals(4, ConfigEntrypoints.constructorDepth(Base.class, frames));
    }

    @Test
    void notBeingConstructedHasNoDepth() {
        StackTraceElement[] frames = {LISTENER, SUBMIT, ENTRYPOINT, BOTTOM};
        assertEquals(-1, ConfigEntrypoints.constructorDepth(Base.class, frames));
    }

    @Test
    void anExceptionFromTheConstructorAtThatDepthFailsIt() {
        Throwable t = thrown(HELPER, init(Base.class), ENTRYPOINT, BOTTOM);
        assertTrue(ConfigEntrypoints.constructorFailed(t, Base.class, at(2, ENTRYPOINT)));
    }

    @Test
    void anExceptionAfterTheConstructorReturnedDoesNotFailIt() {
        Throwable t = thrown(HELPER, ENTRYPOINT, BOTTOM);
        assertFalse(ConfigEntrypoints.constructorFailed(t, Base.class, at(2, ENTRYPOINT)));
    }

    @Test
    void anotherInstanceFailingAtADifferentDepthDoesNotFailThisOne() {
        // the failing Base is built from a helper, one frame further from the bottom than the one recorded
        Throwable t = thrown(init(Base.class), HELPER, ENTRYPOINT, BOTTOM);
        assertFalse(ConfigEntrypoints.constructorFailed(t, Base.class, at(1, ENTRYPOINT)));
        assertTrue(ConfigEntrypoints.constructorFailed(t, Base.class, at(3, HELPER)));
    }

    @Test
    void wrappedCausesAreChecked() {
        Throwable cause = thrown(HELPER, init(Base.class), ENTRYPOINT, BOTTOM);
        Throwable wrapper = new ExceptionInInitializerError(cause);
        wrapper.setStackTrace(new StackTraceElement[]{ENTRYPOINT, BOTTOM});
        assertTrue(ConfigEntrypoints.constructorFailed(wrapper, Base.class, at(2, ENTRYPOINT)));
    }

    @Test
    void anEmptyStackTraceDoesNotFailIt() {
        assertFalse(ConfigEntrypoints.constructorFailed(thrown(), Base.class, at(2, ENTRYPOINT)));
    }

    @Test
    void theConstructionRecordsTheCallerOfTheOutermostConstructor() {
        StackTraceElement[] frames = {LISTENER, SUBMIT, init(Config.class), init(Base.class), ENTRYPOINT, BOTTOM};
        assertEquals(at(2, ENTRYPOINT), ConfigEntrypoints.construction(Base.class, frames));
    }

    @Test
    void aConstructionAtTheBottomOfTheStackHasNoCaller() {
        StackTraceElement[] frames = {LISTENER, SUBMIT, init(Config.class), init(Base.class)};
        assertEquals(at(0, null), ConfigEntrypoints.construction(Base.class, frames));
    }

    @Test
    void notBeingConstructedHasNoConstruction() {
        assertNull(ConfigEntrypoints.construction(Base.class, new StackTraceElement[]{LISTENER, SUBMIT, ENTRYPOINT, BOTTOM}));
    }

    @Test
    void anotherInstanceFailingFromAnotherLineAtTheSameDepthDoesNotFailThisOne() {
        // the next instance threw before it reached Config, so this one was never replaced in the map
        Throwable t = thrown(HELPER, init(Base.class), line(ENTRYPOINT, 11), BOTTOM);
        assertFalse(ConfigEntrypoints.constructorFailed(t, Base.class, at(2, line(ENTRYPOINT, 10))));
        assertTrue(ConfigEntrypoints.constructorFailed(t, Base.class, at(2, line(ENTRYPOINT, 11))));
    }

    @Test
    void aTraceMissingItsBottomFramesIsCountedFromRegister() {
        StackTraceElement[] submitted = {LISTENER, SUBMIT, init(Config.class), init(Base.class), ENTRYPOINT, REGISTER, HELPER, BOTTOM};
        ConfigEntrypoints.Construction construction = ConfigEntrypoints.construction(Base.class, submitted);
        assertEquals(new ConfigEntrypoints.Construction(2, ENTRYPOINT, true), construction);
        // the JVM dropped the bottom of this trace, so it ends earlier than the one recorded
        Throwable t = thrown(HELPER, HELPER, init(Base.class), ENTRYPOINT, REGISTER);
        assertTrue(ConfigEntrypoints.constructorFailed(t, Base.class, construction));
    }

    @Test
    void aTraceCutOffAboveRegisterIsNotLinedUp() {
        ConfigEntrypoints.Construction construction = new ConfigEntrypoints.Construction(2, ENTRYPOINT, true);
        // a StackOverflowError keeps only the top of the stack, so register() and everything below is gone
        Throwable t = thrown(init(Base.class), ENTRYPOINT, HELPER);
        assertFalse(ConfigEntrypoints.constructorFailed(t, Base.class, construction));
    }

    private static Throwable thrown(StackTraceElement... frames) {
        Throwable t = new RuntimeException("test");
        t.setStackTrace(frames);
        return t;
    }

    private static ConfigEntrypoints.Construction at(int depth, StackTraceElement caller) {
        return new ConfigEntrypoints.Construction(depth, caller);
    }

    private static StackTraceElement line(StackTraceElement frame, int line) {
        return new StackTraceElement(frame.getClassName(), frame.getMethodName(), "Test.java", line);
    }

    private static StackTraceElement init(Class<?> type) {
        return frame(type, "<init>");
    }

    private static StackTraceElement frame(Class<?> type, String method) {
        return new StackTraceElement(type.getName(), method, null, -1);
    }

    // never constructed, only their class hierarchy is read
    static class Base extends Config {
        Base() {
            super("base", "Base", Category.OTHER);
        }
    }

    static class Derived extends Base {
    }

    private static ModContainer mod(String id) {
        ClassLoader loader = ConfigEntrypointsTest.class.getClassLoader();
        ModMetadata metadata = (ModMetadata) Proxy.newProxyInstance(loader, new Class<?>[]{ModMetadata.class}, (proxy, method, args) -> id);
        return (ModContainer) Proxy.newProxyInstance(loader, new Class<?>[]{ModContainer.class}, (proxy, method, args) -> metadata);
    }

    @Test
    void theSameContainersAreNoChange() {
        ModContainer lib = mod("lib");
        assertFalse(ConfigEntrypoints.modsChanged(List.of(lib), Map.of("lib", lib)));
    }

    @Test
    void anAddedModIsAChange() {
        ModContainer lib = mod("lib");
        assertTrue(ConfigEntrypoints.modsChanged(List.of(lib, mod("added")), Map.of("lib", lib)));
    }

    @Test
    void aContainerReplacedUnderTheSameIdIsAChange() {
        assertTrue(ConfigEntrypoints.modsChanged(List.of(mod("lib")), Map.of("lib", mod("lib"))));
    }
}
