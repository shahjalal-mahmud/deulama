package com.appriyo.deulama.domain.model

/**
 * Domain wrapper for the `/api/recommendations` response.
 *
 * The wire response carries two boolean flags (`is_personalized`,
 * `fallback`) and a list of up to 10 [Drama]s. The UI renders this
 * set directly — internal recommendation scores are NOT exposed by
 * the API, so the domain model intentionally has no numeric "match
 * score" field.
 */
data class RecommendationSet(
    val items: List<Drama>,
    val isPersonalized: Boolean,
    val fallback: Boolean,
) {
    /** Convenience for `LazyColumn` / `LazyVerticalGrid` adapters. */
    val isEmpty: Boolean get() = items.isEmpty()
    val count: Int get() = items.size
}
