package de.odpisz.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Random SQLCipher passphrase wrapped by an AES key that never leaves
 * the Android Keystore. The wrapped bytes live in a private preference file.
 */
@Singleton
class DatabasePassphrase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun bytes(): ByteArray {
            val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val stored = preferences.getString(KEY, null)
            if (stored != null) {
                return unwrap(Base64.decode(stored, Base64.NO_WRAP))
            }
            val fresh = ByteArray(PASSPHRASE_BYTES).also { java.security.SecureRandom().nextBytes(it) }
            preferences.edit().putString(KEY, Base64.encodeToString(wrap(fresh), Base64.NO_WRAP)).apply()
            return fresh
        }

        private fun wrap(raw: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(raw)
            val iv = cipher.iv
            return byteArrayOf(iv.size.toByte()) + iv + encrypted
        }

        private fun unwrap(payload: ByteArray): ByteArray {
            val ivLength = payload[0].toInt()
            val iv = payload.copyOfRange(1, 1 + ivLength)
            val encrypted = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            return cipher.doFinal(encrypted)
        }

        private fun secretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(ALIAS, null) as? SecretKey
            if (existing != null) return existing
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec =
                KeyGenParameterSpec
                    .Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE)
                    .build()
            generator.init(spec)
            return generator.generateKey()
        }

        private companion object {
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val ALIAS = "odpisz_db_passphrase"
            const val PREFS = "odpisz_db_key"
            const val KEY = "wrapped"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val GCM_TAG_BITS = 128
            const val KEY_SIZE = 256
            const val PASSPHRASE_BYTES = 32
        }
    }
