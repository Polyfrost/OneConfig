package cc.polyfrost.oneconfig.api.commands.arguments;

public class Arguments {
    public final String[] args;
    public final boolean greedy;
    private int position = 0;

    public Arguments(String[] args, boolean greedy) {
        this.args = args;
        this.greedy = greedy;
    }

    public String poll() {
        ++position;
        return args[position - 1];
    }

    public String peek() {
        if (hasNext()) {
            return args[position];
        } else {
            return null;
        }
    }

    public boolean hasNext() {
        return position < args.length;
    }

    public int getPosition() {
        return position;
    }
}
