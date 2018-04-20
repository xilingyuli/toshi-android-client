/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 *  This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.crypto.keyStore.FingerprintKeystore

import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.support.annotation.RequiresApi
import com.toshi.crypto.keyStore.CipherGenerator
import com.toshi.crypto.keyStore.KeyGenerator
import com.toshi.crypto.keyStore.KeystoreSecretKey
import com.toshi.exception.KeyStoreException
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException

@RequiresApi(Build.VERSION_CODES.M)
class FingerprintKeystore(
        private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
) {

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ALIAS = "FingerprintAlias"
    }

    init {
        keyStore.load(null)
        createNewKeysIfNeeded()
    }

    @Throws(KeyStoreException::class)
    private fun createNewKeysIfNeeded() {
        try {
            if (keyStore.containsAlias(ALIAS)) return
            KeyGenerator(ALIAS).generateKey()
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException -> throw KeyStoreException(Throwable(e.message))
                is NoSuchProviderException -> throw KeyStoreException(Throwable(e.message))
                is InvalidAlgorithmParameterException -> throw KeyStoreException(Throwable(e.message))
                is java.security.KeyStoreException -> throw KeyStoreException(Throwable(e.message))
            }
        }
    }

    @Throws(KeyStoreException::class)
    fun deleteKey() {
        try {
            keyStore.deleteEntry(ALIAS)
        } catch (e: java.security.KeyStoreException) {
            throw KeyStoreException(Throwable(e.message))
        }
    }

    fun createCryptoObject(): FingerprintManager.CryptoObject {
        val secretKey = KeystoreSecretKey(keyStore, ALIAS).get()
        val cipher = CipherGenerator().generateEncryptionCipher(secretKey)
        return FingerprintManager.CryptoObject(cipher)
    }
}