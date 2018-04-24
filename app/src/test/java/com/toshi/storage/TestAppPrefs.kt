/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
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

import com.toshi.exception.CurrencyException
import com.toshi.model.local.network.Network
import com.toshi.util.CurrencyUtil
import com.toshi.util.sharedPrefs.AppPrefsInterface

class TestAppPrefs : AppPrefsInterface {

    companion object {
        private const val HAS_ONBOARDED = "hasOnboarded"
        private const val HAS_SIGNED_OUT = "hasSignedIn"
        private const val HAS_BACKED_UP_PHRASE = "hasBackedUpPhrase"
        private const val HAS_LOADED_APP_FIRST_TIME = "hasLoadedAppFirstTime"
        private const val LOCAL_CURRENCY_CODE = "localCurrencyCode"
        private const val WAS_MIGRATED = "wasMigrated"
        private const val FORCE_USER_UPDATE = "forceUserUpdate_2"
        private const val CURRENT_NETWORK = "currentNetwork"
        private const val HAS_CLEARED_NOTIFICATION_CHANNELS = "hasClearedNotificationChannels"
    }

    private val map by lazy { HashMap<String, Any?>() }

    override fun hasOnboarded(): Boolean {
        return map[HAS_ONBOARDED] as Boolean? ?: false
    }

    override fun setHasOnboarded(hasOnboarded: Boolean) {
        map[HAS_ONBOARDED] = hasOnboarded
    }

    override fun hasLoadedApp(): Boolean {
        return map[HAS_LOADED_APP_FIRST_TIME] as Boolean? ?: false
    }

    override fun setHasLoadedApp() {
        map[HAS_LOADED_APP_FIRST_TIME] = true
    }

    override fun setSignedIn() {
        map[HAS_SIGNED_OUT] = false
    }

    override fun setSignedOut() {
        map[HAS_SIGNED_OUT] = true
    }

    override fun hasSignedOut(): Boolean {
        return map[HAS_SIGNED_OUT] as Boolean? ?: false
    }

    override fun setHasBackedUpPhrase() {
        map[HAS_BACKED_UP_PHRASE] = true
    }

    override fun hasBackedUpPhrase(): Boolean {
        return map[HAS_BACKED_UP_PHRASE] as Boolean? ?: false
    }

    override fun saveCurrency(currencyCode: String) {
        map[LOCAL_CURRENCY_CODE] = currencyCode
    }

    @Throws(CurrencyException::class)
    override fun getCurrency(): String {
        val currencyCode = map[LOCAL_CURRENCY_CODE] as String?
        return currencyCode ?: getCurrencyFromLocaleAndSave()
    }

    override fun getCurrencyFromLocaleAndSave(): String {
        val currency = CurrencyUtil.getCurrencyFromLocale()
        saveCurrency(currency)
        return currency
    }

    override fun setWasMigrated(wasMigrated: Boolean) {
        map[WAS_MIGRATED] = wasMigrated
    }

    override fun wasMigrated(): Boolean {
        return map[WAS_MIGRATED] as Boolean? ?: false
    }

    override fun setForceUserUpdate(forceUpdate: Boolean) {
        map[FORCE_USER_UPDATE] = forceUpdate
    }

    override fun shouldForceUserUpdate(): Boolean {
        return map[FORCE_USER_UPDATE] as Boolean? ?: false
    }

    override fun setCurrentNetwork(network: Network) {
        map[CURRENT_NETWORK] = network.id
    }

    override fun getCurrentNetworkId(): String? {
        return map[CURRENT_NETWORK] as String?
    }

    override fun setHasClearedNotificationChannels() {
        map[HAS_CLEARED_NOTIFICATION_CHANNELS] = true
    }

    override fun hasClearedNotificationChannels(): Boolean {
        return map[HAS_CLEARED_NOTIFICATION_CHANNELS] as Boolean? ?: false
    }

    override fun clear() {
        map[HAS_BACKED_UP_PHRASE] = false
        map[LOCAL_CURRENCY_CODE] = null
        map[WAS_MIGRATED] = false
        map[HAS_ONBOARDED] = false
    }
}