package cc.polyfrost.oneconfig.utils.commands.arguments;

import org.jetbrains.annotations.Nullable;

public class FloatParser extends ArgumentParser<Float> {

    @Override
    public @Nullable Float parse(Arguments arguments) {
        return Float.parseFloat(arguments.poll());
    }
}
