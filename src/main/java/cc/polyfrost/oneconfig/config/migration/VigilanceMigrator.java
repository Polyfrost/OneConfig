package cc.polyfrost.oneconfig.config.migration;

import cc.polyfrost.oneconfig.config.compatibility.vigilance.VigilanceName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VigilanceMigrator implements Migrator {
    private static final Pattern categoryPattern = Pattern.compile("\\[\"?(?<category>[^.\\[\\]\"]+)\"?\\.\"?(?<subcategory>[^.\\[\\]\"]+)\"?]");
    private static final Pattern booleanPattern = Pattern.compile("\"?(?<name>[^\\s\"]+)\"? = (?<value>true|false)");
    private static final Pattern numberPattern = Pattern.compile("\"?(?<name>[^\\s\"]+)\"? = (?<value>[\\d.]+)");
    private static final Pattern stringPattern = Pattern.compile("\"?(?<name>[^\\s\"]+)\"? = \"(?<value>.+)\"");
    protected final String filePath;
    protected HashMap<String, HashMap<String, HashMap<String, Object>>> values = null;
    protected final boolean fileExists;

    public VigilanceMigrator(String filePath) {
        this.filePath = filePath;
        this.fileExists = new File(filePath).exists();
    }

    @Override
    public Object getValue(Field field, String name, String category, String subcategory) {
        if (!fileExists) return null;
        if (values == null) getOptions();
        if (field.isAnnotationPresent(VigilanceName.class)) {
            VigilanceName annotation = field.getAnnotation(VigilanceName.class);
            name = annotation.name();
            category = annotation.category();
            subcategory = annotation.subcategory();
        }
        name = parse(name);
        category = parse(category);
        subcategory = parse(subcategory);
        if (values.containsKey(category) && values.get(category).containsKey(subcategory) && values.get(category).get(subcategory).containsKey(name))
            return values.get(category).get(subcategory).get(name);
        return null;
    }

    protected String parse(String value) {
        return value.toLowerCase().replace(" ", "_");
    }

    protected void getOptions() {
        if (values == null) values = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentCategory = null;
            String currentSubcategory = null;
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher categoryMatcher = categoryPattern.matcher(line);
                if (categoryMatcher.find()) {
                    currentCategory = categoryMatcher.group("category");
                    currentSubcategory = categoryMatcher.group("subcategory");
                    if (!values.containsKey(currentCategory)) values.put(currentCategory, new HashMap<>());
                    if (!values.get(currentCategory).containsKey(currentSubcategory))
                        values.get(currentCategory).put(currentSubcategory, new HashMap<>());
                    continue;
                }
                if (currentCategory == null) continue;
                HashMap<String, Object> options = values.get(currentCategory).get(currentSubcategory);
                Matcher booleanMatcher = booleanPattern.matcher(line);
                if (booleanMatcher.find()) {
                    options.put(booleanMatcher.group("name"), Boolean.parseBoolean(booleanMatcher.group("value")));
                    continue;
                }
                Matcher numberMatcher = numberPattern.matcher(line);
                if (numberMatcher.find()) {
                    String value = numberMatcher.group("value");
                    if (value.contains(".")) options.put(numberMatcher.group("name"), Float.parseFloat(value));
                    else options.put(numberMatcher.group("name"), Integer.parseInt(value));
                    continue;
                }
                Matcher stringMatcher = stringPattern.matcher(line);
                if (stringMatcher.find()) {
                    options.put(stringMatcher.group("name"), stringMatcher.group("value"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
