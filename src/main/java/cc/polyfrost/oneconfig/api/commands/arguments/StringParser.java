package cc.polyfrost.oneconfig.api.commands.arguments;

public class StringParser extends ArgumentParser<String> {

    @Override
    public String parse(Arguments arguments) {
        if (arguments.greedy) {
            StringBuilder builder = new StringBuilder();
            while (arguments.hasNext()) {
                String arg = arguments.poll();
                builder.append(arg).append(" ");
            }
            return builder.toString().trim();
        } else {
            return arguments.poll();
        }
    }
}
