package cc.polyfrost.oneconfig.utils.commands.arguments;

import org.jetbrains.annotations.Nullable;

public class DoubleParser extends ArgumentParser<Double> {
    @Override
    public @Nullable Double parse(Arguments arguments) {
        try {
            return Double.parseDouble(arguments.poll());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
