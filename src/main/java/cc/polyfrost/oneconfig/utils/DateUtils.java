package cc.polyfrost.oneconfig.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils extends Date {
    private static final Pattern timeRegex = Pattern.compile("(\\d+)([A-Za-z]+)");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyy-MM-dd @ HH:mm:ss");

    public DateUtils(long milliseconds) {
        super(milliseconds);
    }

    public DateUtils() {
        this(Calendar.getInstance().getTimeInMillis());
    }

    public DateUtils(String date, SimpleDateFormat dateFormat) throws ParseException {
        super(dateFormat.parse(date).getTime());
    }

    public DateUtils(String date) throws ParseException {
        this(date, dateFormat);
    }

    public String format(SimpleDateFormat dateFormat) {
        return dateFormat.format(this);
    }

    public String format() {
        return this.format(dateFormat);
    }

    public DateUtils add(long millis) {
        this.setTime(this.getTime() + millis);
        return this;
    }

    public DateUtils add(Duration duration) {
        return this.add(duration.toMillis());
    }

    public DateUtils add(TimeUnit unit, int time) {
        return this.add(unit.toMillis((long)time));
    }

    public DateUtils add(String time) throws ParseException {
        BooleanUtils.notNull(time, () -> "time cannot be null");

        if (time.length() < 2) {
            throw new ParseException("time is too short", 0);
        } else if (time.startsWith("-")) {
            throw new ParseException("time cannot be negative", 0);
        } else {
            Matcher matcher = timeRegex.matcher(time);

            if (!matcher.matches()) {
                throw new ParseException("unable to parse time", 0);
            } else {
                String timeAmount = matcher.group(1);
                String timeUnit = matcher.group(2);

                int amount = Integer.parseInt(timeAmount);
                byte unit = -1;

                switch(timeUnit.hashCode()) {
                    case 100:
                        if (timeUnit.equals("d")) {
                            unit = 3;
                        }
                        break;
                    case 104:
                        if (timeUnit.equals("h")) {
                            unit = 4;
                        }
                        break;
                    case 109:
                        if (timeUnit.equals("m")) {
                            unit = 1;
                        }
                        break;
                    case 115:
                        if (timeUnit.equals("s")) {
                            unit = 7;
                        }
                        break;
                    case 119:
                        if (timeUnit.equals("w")) {
                            unit = 2;
                        }
                        break;
                    case 121:
                        if (timeUnit.equals("y")) {
                            unit = 0;
                        }
                        break;
                    case 108114:
                        if (timeUnit.equals("min")) {
                            unit = 6;
                        }
                        break;
                    case 3351649:
                        if (timeUnit.equals("mins")) {
                            unit = 5;
                        }
                }

                switch(unit) {
                    case 0:
                        return this.add(TimeUnit.DAYS, amount * 356);
                    case 1:
                        return this.add(TimeUnit.DAYS, amount * 28);
                    case 2:
                        return this.add(TimeUnit.DAYS, amount * 7);
                    case 3:
                        return this.add(TimeUnit.DAYS, amount);
                    case 4:
                        return this.add(TimeUnit.HOURS, amount);
                    case 5:
                    case 6:
                        return this.add(TimeUnit.MINUTES, amount);
                    case 7:
                        return this.add(TimeUnit.SECONDS, amount);
                    default:
                        throw new ParseException("Invalid input: unknown unit " + unit, 0);
                }
            }
        }
    }


}
