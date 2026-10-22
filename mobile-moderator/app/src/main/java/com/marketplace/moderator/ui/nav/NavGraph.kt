// Shared route names and builders for navigating between moderator screens.
package com.marketplace.moderator.ui.nav

/** Central route names and detail-route builders. IDs in verification routes identify verification requests, not listing records. */

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Verifications : Screen("verifications")
    object VerificationDetail : Screen("verifications/{id}") {
        fun build(id: Int) = "verifications/$id"
    }
    object Disputes : Screen("disputes")
    object DisputeDetail : Screen("disputes/{id}") {
        fun build(id: Int) = "disputes/$id"
    }
    object Chat : Screen("chat")
    object Profile : Screen("profile")
}
