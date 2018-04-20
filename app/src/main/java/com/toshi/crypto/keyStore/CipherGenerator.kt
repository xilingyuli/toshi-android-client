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

package com.toshi.crypto.keyStore

import android.os.Build
import android.support.annotation.RequiresApi
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(Build.VERSION_CODES.KITKAT)
class CipherGenerator {

    companion object {
        private const val encryptionIv = "fixed_enc_iv"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    @Throws(InvalidKeyException::class, InvalidAlgorithmParameterException::class,
            NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    fun generateEncryptionCipher(secretKey: SecretKey): Cipher {
        return generateCipher(Cipher.ENCRYPT_MODE, secretKey)
    }

    @Throws(InvalidKeyException::class, InvalidAlgorithmParameterException::class,
            NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    fun generateDecryptionCipher(secretKey: SecretKey): Cipher {
        return generateCipher(Cipher.DECRYPT_MODE, secretKey)
    }

    private fun generateCipher(mode: Int, secretKey: SecretKey): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, encryptionIv.toByteArray())
        cipher.init(mode, secretKey, spec)
        return cipher
    }
}