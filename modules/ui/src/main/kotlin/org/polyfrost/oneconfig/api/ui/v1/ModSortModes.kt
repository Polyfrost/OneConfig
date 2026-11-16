package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.internal.extensionsBridge
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.api.ui.v1.page.translate

@ExperimentalOneConfigUiApi
public class ModSortMode(
    public val id: String,
    public val icon: String,
    title: PageText,
    confirmation: PageText? = null,
    confirmLabel: PageText? = null,
    public val onSelect: () -> Unit,
) {
    public constructor(
        id: String,
        icon: String,
        title: String,
        confirmation: String? = null,
        confirmLabel: String? = null,
        onSelect: () -> Unit,
    ) : this(
        id,
        icon,
        PageText.literal(title),
        confirmation?.let(PageText::literal),
        confirmLabel?.let(PageText::literal),
        onSelect,
    )

    @Deprecated("Use the constructor that takes confirmLabel", level = DeprecationLevel.HIDDEN)
    @JvmOverloads
    public constructor(
        id: String,
        icon: String,
        title: String,
        confirmation: String? = null,
        onSelect: () -> Unit,
    ) : this(id, icon, title, confirmation, null as String?, onSelect)

    public val titleText: PageText = title

    public val confirmationText: PageText? = confirmation

    public val confirmLabelText: PageText? = confirmLabel

    public val title: String get() = titleText.translate()

    public val confirmation: String? get() = confirmationText?.translate()

    public val confirmLabel: String? get() = confirmLabelText?.translate()
}

@ExperimentalOneConfigUiApi
public object ModSortModes {
    public const val CUSTOM: String = "custom"

    public const val ALPHABETICAL: String = "alphabetical"

    private val LOGGER = LogManager.getLogger("OneConfig/ModSortModes")
    private val lock = Any()

    private var entries by mutableStateOf(emptyList<ModSortMode>())

    @JvmStatic
    public fun register(mode: ModSortMode) {
        synchronized(lock) {
            if (entries.any { it.id == mode.id }) {
                LOGGER.debug("Ignoring ModSortMode {} that is already registered", mode.id)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + mode }
        }
    }

    @JvmStatic
    public fun unregister(mode: ModSortMode): Boolean = synchronized(lock) {
        if (mode !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - mode }
        true
    }

    @JvmStatic
    public fun all(): List<ModSortMode> = entries

    @JvmStatic
    public fun currentId(): String? = extensionsBridge()?.modSortModeId()

    @JvmStatic
    public fun select(id: String) {
        extensionsBridge()?.selectModSortMode(id)
    }
}
