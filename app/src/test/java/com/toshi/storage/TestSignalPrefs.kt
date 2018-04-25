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

package com.toshi.storage

import com.toshi.util.sharedPrefs.SignalPrefsInterface
import com.toshi.util.sharedPrefs.SignalPrefsInterface.Companion.LOCAL_REGISTRATION_ID
import com.toshi.util.sharedPrefs.SignalPrefsInterface.Companion.PASSWORD
import com.toshi.util.sharedPrefs.SignalPrefsInterface.Companion.REGISTERED_WITH_SERVER
import com.toshi.util.sharedPrefs.SignalPrefsInterface.Companion.SERIALIZED_IDENTITY_KEY_PAIR
import com.toshi.util.sharedPrefs.SignalPrefsInterface.Companion.SERIALIZED_LAST_RESORT_KEY
import com.toshi.util.sharedPrefs.SignalPrefsInterface.Companion.SIGNALING_KEY
import com.toshi.util.sharedPrefs.SignalPrefsInterface.Companion.SIGNED_PRE_KEY_ID

class TestSignalPrefs : SignalPrefsInterface {

    private val prefs by lazy { HashMap<String, Any?>() }

    override fun getRegisteredWithServer(): Boolean = prefs[REGISTERED_WITH_SERVER] as? Boolean? ?: false

    override fun setRegisteredWithServer() {
        prefs[REGISTERED_WITH_SERVER] = true
    }

    override fun getLocalRegistrationId(): Int = prefs[LOCAL_REGISTRATION_ID] as? Int ?: -1

    override fun setLocalRegistrationId(registrationId: Int) {
        prefs[LOCAL_REGISTRATION_ID] = registrationId
    }

    override fun getSignalingKey(): String? = prefs[SignalPrefsInterface.SIGNALING_KEY] as? String?

    override fun setSignalingKey(signalingKey: String) {
        prefs[SIGNALING_KEY] = signalingKey
    }

    override fun getPassword(): String? = prefs[PASSWORD] as? String?

    override fun setPassword(password: String) {
        prefs[PASSWORD] = password
    }

    override fun getSerializedIdentityKeyPair(): ByteArray? {
        return prefs[SERIALIZED_IDENTITY_KEY_PAIR] as? ByteArray?
    }

    override fun setSerializedIdentityKeyPair(serializedIdentityKeyPair: ByteArray) {
        prefs[SERIALIZED_IDENTITY_KEY_PAIR] = serializedIdentityKeyPair
    }

    override fun getSerializedLastResortKey(): ByteArray? {
        return prefs[SERIALIZED_LAST_RESORT_KEY] as? ByteArray?
    }

    override fun setSerializedLastResortKey(serializedLastResortKey: ByteArray) {
        prefs[SERIALIZED_LAST_RESORT_KEY] = serializedLastResortKey
    }

    override fun getSignedPreKeyId(): Int = prefs[SignalPrefsInterface.SIGNED_PRE_KEY_ID] as? Int? ?: -1

    override fun setSignedPreKeyId(signedPreKeyId: Int) {
        prefs[SIGNED_PRE_KEY_ID] = signedPreKeyId
    }

    override fun clear() = prefs.clear()
}