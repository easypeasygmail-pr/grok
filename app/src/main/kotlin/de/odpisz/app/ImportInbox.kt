package de.odpisz.app

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportInbox
    @Inject
    constructor() {
        val pendingUri = MutableStateFlow<String?>(null)
    }
