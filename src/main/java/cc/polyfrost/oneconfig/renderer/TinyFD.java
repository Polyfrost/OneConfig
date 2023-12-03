package cc.polyfrost.oneconfig.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * API for TinyFD, a cross-platform file selection dialog.
 * <p>
 * On Linux, TinyFD "allows shell metacharacters in titles, messages, and other input data," meaning that it is vulnerable to command injection.
 * **Treat all user input as untrusted and sanitize it before passing it to TinyFD.**
 */
@SuppressWarnings("unused")
public interface TinyFD {
    String QUESTION_ICON = "question";
    String ERROR_ICON = "error";
    String WARNING_ICON = "warning";
    String INFO_ICON = "info";

    String OK_DIALOG = "ok";
    String OK_CANCEL_DIALOG = "okcancel";
    String YES_NO_DIALOG = "yesno";
    String YES_NO_CANCEL_DIALOG = "yesnocancel";

    TinyFD INSTANCE = LwjglManager.INSTANCE.getTinyFD();

    /**
     * Open a save file selection prompt.
     * Same as {@link #openFileSelector(String, String, String[], String)} but says save instead of open.
     */
    File openSaveSelector(@Nullable String title, @Nullable String defaultFilePath, @Nullable String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a file selection prompt.
     *
     * @param title             the title of the prompt
     * @param defaultFilePath   the path to the default file to select
     * @param filterPatterns    the file extensions to filter by. e.g. new String[]{"*.png", "*.jpg"}
     * @param filterDescription the description for said filter. e.g. "Images"
     * @return the selected file, or null if the user cancelled.
     */
    @Nullable
    File openFileSelector(@Nullable String title, @Nullable String defaultFilePath, @Nullable String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a multi file selection prompt.
     * Same as {@link #openFileSelector(String, String, String[], String)} but allows the user to select multiple files.
     */
    File[] openMultiFileSelector(@Nullable String title, @Nullable String defaultFilePath, @Nullable String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a folder selection prompt.
     * Same as {@link #openFileSelector(String, String, String[], String)} but allows the user to select a folder.
     */
    File openFolderSelector(@Nullable String title, @Nullable String defaultFolderPath);

    /**
     * Shows a message box.
     *
     * @param message      the message. may contain \n and \t
     * @param dialog       the type of message box to show. <br>One of: {@link #OK_DIALOG}, {@link #OK_CANCEL_DIALOG}, {@link #YES_NO_DIALOG}, {@link #YES_NO_CANCEL_DIALOG}
     * @param icon         the icon to use. <br>One of: {@link #QUESTION_ICON}, {@link #ERROR_ICON}, {@link #WARNING_ICON}, {@link #INFO_ICON}
     * @param defaultValue the default value to return if the user closes the dialog without clicking a button
     * @return true if the user clicked the "ok" or "yes" button, false for "cancel" or "no"
     */
    boolean showMessageBox(String title, String message, @NotNull String dialog, String icon, boolean defaultValue);

    /**
     * Shows a notification.
     *
     * @param icon the icon to use. One of: {@link #QUESTION_ICON}, {@link #ERROR_ICON}, {@link #WARNING_ICON}, {@link #INFO_ICON}
     * @return 0 if the user clicked the "ok" button, 1 for "cancel"
     */
    int showNotification(String title, String message, String icon);
}