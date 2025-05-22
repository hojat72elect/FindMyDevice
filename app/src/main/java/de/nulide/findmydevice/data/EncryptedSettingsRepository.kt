package de.nulide.findmydevice.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.nulide.findmydevice.utils.SingletonHolder


/**
 * Storage for sensitive values that benefit from an additional layer of encryption.
 *
 * Because encryption is device-bound, these settings cannot (and should not) be backed up.
 */
class EncryptedSettingsRepository private constructor(context: Context) {

    companion object :
        SingletonHolder<EncryptedSettingsRepository, Context>(::EncryptedSettingsRepository) {

        val TAG = EncryptedSettingsRepository::class.simpleName

        // This file should be EXCLUDED from backups
        private const val FILENAME = "fmd_encrypted_settings"

        private const val KEY_SERVER_CACHED_ACCESS_TOKEN = "KEY_SERVER_CACHED_ACCESS_TOKEN"
        private const val KEY_FMD_PIN = "KEY_FMD_PIN"
    }

    val sharedPrefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPrefs = EncryptedSharedPreferences.create(
            context,
            FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getCachedAccessToken(): String {
        return sharedPrefs.getString(KEY_SERVER_CACHED_ACCESS_TOKEN, "") ?: ""
    }

    fun setCachedAccessToken(newToken: String) {
        sharedPrefs.edit().putString(KEY_SERVER_CACHED_ACCESS_TOKEN, newToken).apply()
    }

    fun getFmdPin(): String? {
        return sharedPrefs.getString(KEY_FMD_PIN, null)
    }

    fun setFmdPin(new: String?) {
        if (new == null) {
            sharedPrefs.edit().remove(KEY_FMD_PIN).apply()
        } else {
            sharedPrefs.edit().putString(KEY_FMD_PIN, new).apply()
        }
    }
}
