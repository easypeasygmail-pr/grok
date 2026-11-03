package de.odpisz.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
        passphrase: DatabasePassphrase,
    ): OdpiszDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(passphrase.bytes())
        return Room
            .databaseBuilder(context, OdpiszDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .build()
    }

    private const val DB_NAME = "odpisz.db"
}
