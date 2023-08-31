import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.commands.internal.CommandTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder.Arg.*;
import static org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder.command;
import static org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder.runs;

public class BuilderTest {
    @Test
    void test() {
        CommandTree t = command("test")
                .then(
                        runs("chicken").with(org.polyfrost.oneconfig.api.commands.factories.builder.BuilderUtils.intArg(), org.polyfrost.oneconfig.api.commands.factories.builder.BuilderUtils.intArg()).does(args -> {
                            int res = getInt(args[0]) + getInt(args[1]);
                            System.out.println(res);
                            return res;
                        })
                ).then(
                        runs("bob").with(org.polyfrost.oneconfig.api.commands.factories.builder.BuilderUtils.shortArg()).does(args -> {
                            System.out.println(getShort(args[0]));
                        })
                ).subcommand("something")
                .then(
                        runs("a").with(org.polyfrost.oneconfig.api.commands.factories.builder.BuilderUtils.stringArg()).does(args -> {
                            System.out.println(getString(args[0]));
                        })
                ).then(
                        runs("a").with(org.polyfrost.oneconfig.api.commands.factories.builder.BuilderUtils.stringArg(), org.polyfrost.oneconfig.api.commands.factories.builder.BuilderUtils.stringArg()).does(args -> {
                            System.out.println(getString(args[0]) + getString(args[1]));
                            return 0;
                        })
                ).registerTree();

        assertEquals(3, t.execute("chicken", "1", "2"));
        assertNull(t.execute("bob", "42"));
        assertNull(t.execute("something", "a", "HEY"));
        assertEquals(0, t.execute("something", "a", "HEY", "THERE"));
    }
}
