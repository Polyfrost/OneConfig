package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Locale;

public class BooleanParser extends ArgumentParser<Boolean> {

    private static final List<String> VALUES = Lists.newArrayList("true", "false");

    @Override
    public @Nullable Boolean parse(Arguments arguments) {
        String next = arguments.poll();
        if (next.equalsIgnoreCase("true")) {
            return true;
        } else if (next.equalsIgnoreCase("false")) {
            return false;
        } else {
            return null;
        }
    }

    @Override
    public @Nullable List<String> complete(Arguments arguments, Parameter parameter) {
        String value = arguments.poll();
        if (value != null && !value.trim().isEmpty()) {
            for (String v : VALUES) {
                if (v.startsWith(value.toLowerCase(Locale.ENGLISH))) {
                    return Lists.newArrayList(v);
                }
            }
        }
        return VALUES;
    }
}
