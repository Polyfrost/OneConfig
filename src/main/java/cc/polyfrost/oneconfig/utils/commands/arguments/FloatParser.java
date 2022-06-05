package cc.polyfrost.oneconfig.utils.commands.arguments;

import org.jetbrains.annotations.Nullable;

public class FloatParser extends ArgumentParser<Float> {

    @Override
    public @Nullable Float parse(Arguments arguments) {
        try {
            return Float.parseFloat(arguments.poll());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
