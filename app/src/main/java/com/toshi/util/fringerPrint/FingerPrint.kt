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

package com.toshi.util.fringerPrint

import android.app.KeyguardManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.support.annotation.RequiresApi
import com.toshi.crypto.keyStore.FingerprintKeystore.FingerprintKeystore

@RequiresApi(api = Build.VERSION_CODES.M)
class FingerPrint(
        private val onSuccessListener: () -> Unit,
        private val onErrorListener: () -> Unit,
        private val onFailedListener: () -> Unit
) {

    fun authenticateUser(fingerprintManager: FingerprintManager) {
        val keystore = FingerprintKeystore()
        val cryptoObject = keystore.createCryptoObject()

        fingerprintManager.authenticate(
                cryptoObject,
                CancellationSignal(),
                0,
                object : FingerprintManager.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onErrorListener()
                    }

                    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                        super.onAuthenticationHelp(helpCode, helpString)
                    }

                    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
                        onSuccessListener()
                    }

                    override fun onAuthenticationFailed() {
                        onFailedListener()
                    }
                },
                null
        )
    }

    fun isSensorAvailable(fingerprintManager: FingerprintManager, keyguardManager: KeyguardManager): Boolean {
        return fingerprintManager.isHardwareDetected
                && fingerprintManager.hasEnrolledFingerprints()
                && keyguardManager.isKeyguardSecure
    }
}