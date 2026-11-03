package de.odpisz.app

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockSession
    @Inject
    constructor() {
        val unlocked = MutableStateFlow(false)

        fun unlock() {
            unlocked.value = true
        }
    }
