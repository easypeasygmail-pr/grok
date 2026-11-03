package de.odpisz.feature.onboarding

data class OnboardingUiState(
    val page: Int = 0,
    val consentChecked: Boolean = false,
) {
    val pageCount: Int = PAGE_COUNT
    val canFinish: Boolean get() = page == pageCount - 1 && consentChecked

    private companion object {
        const val PAGE_COUNT = 3
    }
}

sealed interface OnboardingUiEvent {
    data object Next : OnboardingUiEvent

    data object Back : OnboardingUiEvent

    data class ConsentChanged(
        val checked: Boolean,
    ) : OnboardingUiEvent

    data object Finish : OnboardingUiEvent
}
