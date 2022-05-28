package cc.polyfrost.oneconfig.utils.commands.arguments;

import org.jetbrains.annotations.Nullable;

public class BooleanParser extends ArgumentParser<Boolean> {

    @Override
    public @Nullable Boolean parse(Arguments arguments) {
        return Boolean.parseBoolean(arguments.poll());
    }
}
