package cc.polyfrost.oneconfig.utils.holder;

public abstract class Holder<T> {
    protected T holder;
    public Holder(T holder) {
        this.holder = holder;
    }


    public abstract T getHolder() throws UnsupportedOperationException;

    public abstract void setHolder() throws UnsupportedOperationException;
}
