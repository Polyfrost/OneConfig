package cc.polyfrost.oneconfig.internal.hook;

public interface FramebufferHook {
    boolean isStencilEnabled();
    void enableStencil();
}
