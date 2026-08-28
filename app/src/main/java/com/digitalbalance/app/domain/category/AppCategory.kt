package com.digitalbalance.app.domain.category

enum class AppCategory(val storageKey: String) {
    Education("education"),
    Productivity("productivity"),
    Communication("communication"),
    Social("social"),
    Entertainment("entertainment"),
    Gaming("gaming"),
    Utility("utility"),
    Other("other"),
    MixedContextDependent("mixed_context_dependent");

    companion object {
        fun fromStorageKey(value: String): AppCategory? =
            entries.firstOrNull { it.storageKey == value }
    }
}
