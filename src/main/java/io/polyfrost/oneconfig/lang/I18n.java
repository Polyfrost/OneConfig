package io.polyfrost.oneconfig.lang;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The private I18n class for internal use.
 */
public final class I18n {

    /**
     * The main key for OneConfig localization.
     */
    private final static String ONECONFIG_KEY = "oneconfig";

    /**
     * Blank private void.
     */
    private I18n() {

    }

    /**
     * The ResourceBundle used to store the messages.
     */
    private static ResourceBundle bundle;

    /**
     * Returns the default Locale when called.
     * @return java.util.Locale
     */
    public static Locale getLocale() {
        return Locale.getDefault();
    }

    /**
     * Checks if a Locale exists / is supported.
     * @param l The Locale to validate
     * @return All Locales that are supported
     */
    public static boolean isSupported(Locale l) {
        Locale[] availableLocales = Locale.getAvailableLocales();
        return Arrays.asList(availableLocales).contains(l);
    }

    /**
     * Sets the default Locale.
     * @param l The Locale to set the default locale to
     */
    public static void setLocale(Locale l) {
        Locale.setDefault(l);
    }

    /**
     * Gets a message from the selected Locale.
     * @param key The key of the message you would like to get
     * @return The string of the message if it exists
     */
    public static String getMessage(String key) {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(ONECONFIG_KEY);
        }
        return bundle.getString(key);
    }

    /**
     * Gets a formatted message with arguments.
     * @param key The key of the message you would like to get
     * @param arguments The arguments for the MessageFormat.
     * @return The formatted message in a String format
     */
    public static String getMessage(String key, Object ...arguments) {
        return MessageFormat.format(getMessage(key), arguments);
    }
}
