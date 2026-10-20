package org.polyfrost.oneconfig.internal.ui.shell

object HudEditorRoute

enum class SearchFocus {
    SelectAll,

    /** Focuses the search field and leaves its text and cursor alone */
    Focus,
    Unfocus,
}

object ShellState {
    var searchQuery: String
        get() = ShellController.shared.search.query
        set(value) {
            ShellController.shared.search.update(value)
        }
}
