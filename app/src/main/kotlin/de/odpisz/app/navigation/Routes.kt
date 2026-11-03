package de.odpisz.app.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object HistoryRoute

@Serializable
data object ScanRoute

@Serializable
data class ReviewRoute(
    val letterId: String,
)

@Serializable
data class LetterRoute(
    val letterId: String,
)

@Serializable
data class ReplyRoute(
    val letterId: String,
)

@Serializable
data class RedCardRoute(
    val letterId: String,
)

@Serializable
data object SettingsRoute
