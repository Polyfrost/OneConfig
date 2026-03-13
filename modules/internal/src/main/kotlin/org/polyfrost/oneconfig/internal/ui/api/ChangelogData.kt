package org.polyfrost.oneconfig.internal.ui.api

interface ChangelogData {
    val sections: List<ChangelogSection>
}

interface ChangelogSection {
    val headerImageUrl: String?

    val title: String
    val content: String
    val author: String
    val date: String
}