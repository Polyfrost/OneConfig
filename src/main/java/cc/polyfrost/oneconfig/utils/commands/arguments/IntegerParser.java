package cc.polyfrost.oneconfig.utils.commands.arguments;

public class IntegerParser extends ArgumentParser<Integer> {
    @Override
    public Integer parse(Arguments arguments) {
        try {
            return Integer.parseInt(arguments.poll());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
