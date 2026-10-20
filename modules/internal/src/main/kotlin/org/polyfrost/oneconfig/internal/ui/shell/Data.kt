package org.polyfrost.oneconfig.internal.ui.shell

object HudEditorRoute

enum class SearchFocus {
    SelectAll,

    /** Focuses the search field and leaves its text and cursor alone */
    Focus,
    Unfocus,
}

@Deprecated("Read the search query through LocalShell instead. Removed in OneConfig 1.6.0")
object ShellState {
    var searchQuery: String
        get() = ShellController.shared.search.query
        set(value) {
            ShellController.shared.search.update(value)
        }
}
