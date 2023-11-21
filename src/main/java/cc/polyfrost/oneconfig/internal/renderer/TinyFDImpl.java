package cc.polyfrost.oneconfig.internal.renderer;

import cc.polyfrost.oneconfig.renderer.TinyFD;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;

public class TinyFDImpl implements TinyFD {

    @Override
    public File openSaveSelector(String title, @Nullable String defaultFilePath, String[] filterPatterns, String filterDescription) {
        PointerBuffer p = null;
        if (filterPatterns != null && filterPatterns.length != 0) {
            p = stringsToPointerBuffer(filterPatterns);
        }
        String out = TinyFileDialogs.tinyfd_saveFileDialog(title == null ? "Save" : title, defaultFilePath, p, filterDescription);
        return out == null ? null : new File(out);
    }

    @Override
    public File openFileSelector(String title, @Nullable String defaultFilePath, String[] filterPatterns, String filterDescription) {
        PointerBuffer p = null;
        if (filterPatterns != null && filterPatterns.length != 0) {
            p = stringsToPointerBuffer(filterPatterns);
        }
        String out = TinyFileDialogs.tinyfd_openFileDialog(title == null ? "Open file" : title, defaultFilePath, p, filterDescription, false);
        return out == null ? null : new File(out);
    }

    @Override
    public File[] openMultiFileSelector(String title, @Nullable String defaultFilePath, String[] filterPatterns, String filterDescription) {
        PointerBuffer p = null;
        if (filterPatterns != null && filterPatterns.length != 0) {
            p = stringsToPointerBuffer(filterPatterns);
        }
        String out = TinyFileDialogs.tinyfd_openFileDialog(title == null ? "Open files" : title, defaultFilePath, p, filterDescription, true);
        if (out == null) {
            return null;
        }
        String[] split = out.split("\\|");
        File[] files = new File[split.length];
        for (int i = 0; i < split.length; i++) {
            files[i] = new File(split[i]);
        }
        return files;
    }

    @Override
    public File openFolderSelector(String title, @Nullable String defaultFolderPath) {
        String out = TinyFileDialogs.tinyfd_selectFolderDialog(title == null ? "Select folder" : title, defaultFolderPath);
        return out == null ? null : new File(out);
    }

    @Override
    public boolean showMessageBox(String title, String message, @NotNull String dialog, String icon, boolean defaultState) {
        return TinyFileDialogs.tinyfd_messageBox(title, message, dialog, icon, defaultState);
    }

    @Override
    public int showNotification(String title, String message, String icon) {
        return TinyFileDialogs.tinyfd_notifyPopup(title, message, icon);
    }

    private static PointerBuffer stringsToPointerBuffer(String[] strings) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer p = stack.mallocPointer(strings.length);
            for (int i = 0; i < strings.length; i++) {
                p.put(i, stack.UTF8(strings[i]));
            }
            return p.flip();
        }
    }
}