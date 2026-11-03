package de.odpisz.core.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.odpisz.core.model.DocumentTextReader
import de.odpisz.core.model.ExternalActions
import de.odpisz.core.model.LetterPdfWriter
import de.odpisz.core.model.LetterRepository
import de.odpisz.core.model.ReminderScheduler
import de.odpisz.core.model.SettingsRepository
import de.odpisz.core.network.FakeLetterAnalyzer
import de.odpisz.core.network.FakeReplyGenerator
import de.odpisz.core.network.LetterAnalyzer
import de.odpisz.core.network.ReplyGenerator
import de.odpisz.core.ocr.MlKitDocumentTextReader
import de.odpisz.core.pdf.AndroidLetterPdfWriter
import de.odpisz.core.privacy.PiiMasker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {
    @Binds
    abstract fun letters(impl: LetterRepositoryImpl): LetterRepository

    @Binds
    abstract fun settings(impl: SettingsStore): SettingsRepository

    @Binds
    abstract fun reminders(impl: WorkReminderScheduler): ReminderScheduler

    @Binds
    abstract fun actions(impl: AndroidExternalActions): ExternalActions

    @Binds
    abstract fun reader(impl: MlKitDocumentTextReader): DocumentTextReader

    @Binds
    abstract fun pdf(impl: AndroidLetterPdfWriter): LetterPdfWriter
}

@Module
@InstallIn(SingletonComponent::class)
object AnalyzerModule {
    @Provides
    @Singleton
    fun analyzer(): LetterAnalyzer = FakeLetterAnalyzer()

    @Provides
    @Singleton
    fun replies(): ReplyGenerator = FakeReplyGenerator()

    @Provides
    @Singleton
    fun masker(): PiiMasker = PiiMasker()
}
