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
import android.util.Base64
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

@RequiresApi(Build.VERSION_CODES.KITKAT)
class Encryptor(
        private val cipherGenerator: CipherGenerator = CipherGenerator()
) {
    @Throws(InvalidKeyException::class, InvalidAlgorithmParameterException::class,
            NoSuchAlgorithmException::class, NoSuchPaddingException::class,
            IllegalBlockSizeException::class, BadPaddingException::class, UnsupportedEncodingException::class)
    fun encrypt(secretKey: SecretKey, textToEncrypt: String): String? {
        val cipher = cipherGenerator.generateEncryptionCipher(secretKey)
        val encryptedData = cipher.doFinal(textToEncrypt.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }
}