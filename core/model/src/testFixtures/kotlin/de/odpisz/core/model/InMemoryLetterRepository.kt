package de.odpisz.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class InMemoryLetterRepository(
    initial: List<Letter> = emptyList(),
) : LetterRepository {
    private val letters = MutableStateFlow(initial)
    private val profile = MutableStateFlow(UserProfile())
    var failReads: Boolean = false
    val generated = mutableListOf<ReplyScenario>()

    override fun observeLetters(): Flow<List<LetterSummary>> =
        letters.map { rows ->
            if (failReads) error("db")
            rows.map { it.toSummary() }
        }

    override suspend fun getLetter(id: String): Letter? {
        if (failReads) error("db")
        return letters.value.find { it.id == id }
    }

    override suspend fun importText(
        text: String,
        sourceLabel: String?,
    ): String = error("unused")

    override suspend fun importUri(uri: String): String = error("unused")

    override suspend fun analyze(letterId: String): Letter = error("unused")

    override suspend fun confirmDeadline(
        letterId: String,
        index: Int,
    ) {
        update(letterId) { letter ->
            val analysis = requireNotNull(letter.analysis)
            val deadlines = analysis.deadlines.toMutableList()
            deadlines[index] = deadlines[index].copy(confirmed = true)
            letter.copy(analysis = analysis.copy(deadlines = deadlines))
        }
    }

    override suspend fun correctDeadline(
        letterId: String,
        index: Int,
        date: LocalDate,
    ) {
        update(letterId) { letter ->
            val analysis = requireNotNull(letter.analysis)
            val deadlines = analysis.deadlines.toMutableList()
            deadlines[index] = deadlines[index].copy(date = date, confirmed = true)
            letter.copy(analysis = analysis.copy(deadlines = deadlines))
        }
    }

    override suspend fun addDeadline(
        letterId: String,
        deadline: Deadline,
    ) = Unit

    override suspend fun generateReply(
        letterId: String,
        scenario: ReplyScenario,
        polishFacts: String,
    ): Letter {
        generated += scenario
        return update(letterId) { it.copy(status = LetterStatus.ReplyReady) }
    }

    override fun observeProfile(): Flow<UserProfile> = profile

    override suspend fun saveProfile(profile: UserProfile) {
        this.profile.value = profile
    }

    override suspend fun deleteAll() {
        letters.value = emptyList()
    }

    private fun update(
        id: String,
        transform: (Letter) -> Letter,
    ): Letter {
        val current = letters.value
        val updated = current.map { if (it.id == id) transform(it) else it }
        letters.value = updated
        return updated.first { it.id == id }
    }
}
