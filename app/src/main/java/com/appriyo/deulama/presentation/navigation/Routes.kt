package com.appriyo.deulama.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface HangugRoute {

    // ---- Auth graph (no bottom bar) ----
    @Serializable
    data object Login : HangugRoute

    @Serializable
    data object Register : HangugRoute

    // ---- Main graph (bottom bar visible) ----
    @Serializable
    data object Home : HangugRoute

    @Serializable
    data object Discover : HangugRoute

    @Serializable
    data object Recommendations : HangugRoute

    @Serializable
    data object Activity : HangugRoute

    @Serializable
    data object Profile : HangugRoute

    // ---- Pushed routes (no bottom bar / dimmed) ----
    @Serializable
    data class DramaDetails(
        val id: Int,
        /**
         * `true` when the user navigated here from the Recommendations
         * tab. Lets the Details screen render a "Because you liked X"
         * reasoning line. Defaults to `false` everywhere else.
         */
        val fromRecommendations: Boolean = false,
    ) : HangugRoute

    @Serializable
    data object EditProfile : HangugRoute

    /** Genre breakdown — opened from Profile or empty-states. */
    @Serializable
    data object GenreStats : HangugRoute
}

/** The five tabs shown in the bottom NavigationBar, in display order. */
enum class BottomTab(val route: HangugRoute, val label: String) {
    HOME(HangugRoute.Home, "Home"),
    DISCOVER(HangugRoute.Discover, "Discover"),
    RECOMMENDATIONS(HangugRoute.Recommendations, "For You"),
    ACTIVITY(HangugRoute.Activity, "Activity"),
    PROFILE(HangugRoute.Profile, "Profile"),
}