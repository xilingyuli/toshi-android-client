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

package com.toshi.managers.balanceManager

import com.toshi.crypto.HDWallet
import com.toshi.manager.ethRegistration.EthGcmRegistration
import com.toshi.manager.network.EthereumInterface
import com.toshi.masterSeed
import com.toshi.mockNetwork
import com.toshi.mockWallet
import com.toshi.model.network.GcmDeregistration
import com.toshi.model.network.GcmRegistration
import com.toshi.model.network.ServerTime
import com.toshi.network
import com.toshi.storage.TestAppPrefs
import com.toshi.storage.TestEthGcmPrefs
import com.toshi.storage.TestGcmToken
import com.toshi.util.gcm.GcmTokenInterface
import com.toshi.util.sharedPrefs.AppPrefsInterface
import com.toshi.util.sharedPrefs.EthGcmPrefsInterface
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import rx.Completable
import rx.Single
import rx.schedulers.Schedulers

class EthGcmRegistrationTests {

    private lateinit var wallet: HDWallet
    private lateinit var ethGcmRegistration: EthGcmRegistration
    private lateinit var appPrefs: AppPrefsInterface
    private lateinit var gcmPrefs: EthGcmPrefsInterface
    private lateinit var ethApi: EthereumInterface
    private lateinit var gcmToken: GcmTokenInterface

    @Before
    fun before() {
        mock()
    }

    private fun mock() {
        ethApi = mockEthApi()
        appPrefs = TestAppPrefs()
        gcmPrefs = TestEthGcmPrefs()
        gcmToken = TestGcmToken()
        wallet = mockWallet(masterSeed)
    }

    private fun mockEthApi(): EthereumInterface {
        val ethApi = Mockito.mock(EthereumInterface::class.java)
        Mockito.`when`(ethApi.timestamp)
                .thenReturn(Single.just(ServerTime(1L)))
        Mockito.`when`(ethApi.registerGcm(any(Long::class.java), any(GcmRegistration::class.java)))
                .thenReturn(Completable.complete())
        Mockito.`when`(ethApi.unregisterGcm(any(Long::class.java), any(GcmDeregistration::class.java)))
                .thenReturn(Completable.complete())
        return ethApi
    }

    private fun createEthGcmRegistration(appPrefs: AppPrefsInterface,
                                         gcmPrefs: EthGcmPrefsInterface,
                                         ethApi: EthereumInterface,
                                         gcmToken: GcmTokenInterface): EthGcmRegistration {
        val networks = mockNetwork(network)
        return EthGcmRegistration(
                networks = networks,
                appPrefs = appPrefs,
                gcmPrefs = gcmPrefs,
                ethService = ethApi,
                gcmToken = gcmToken,
                subscribeOnScheduler = Schedulers.trampoline()
        )
    }

    @Test
    fun `register before wallet is set`() {
        initEthGcmRegistration()
        try {
            ethGcmRegistration.forceRegisterEthGcm().await()
        } catch (e: IllegalStateException) {
            assertTrue(true)
            val isEthGcmTokenSentToServer = gcmPrefs.isEthGcmTokenSentToServer(network.id)
            assertFalse(isEthGcmTokenSentToServer)
            return
        }
        assertTrue(false)
    }

    @Test
    fun `register after wallet is set`() {
        initEthGcmRegistrationWitWallet(wallet)
        try {
            ethGcmRegistration.forceRegisterEthGcm().await()
        } catch (e: IllegalStateException) {
            assertTrue(false)
            return
        }
        val isEthGcmTokenSentToServer = gcmPrefs.isEthGcmTokenSentToServer(network.id)
        assertTrue(isEthGcmTokenSentToServer)
    }

    @Test
    fun unregisterGcm() {
        initEthGcmRegistrationWitWallet(wallet)
        val token = gcmToken.get().toBlocking().value()
        try {
            ethGcmRegistration.unregisterFromEthGcm(token).await()
        } catch (e: IllegalStateException) {
            assertTrue(false)
            return
        }
        val isEthGcmTokenSentToServer = gcmPrefs.isEthGcmTokenSentToServer(network.id)
        assertFalse(isEthGcmTokenSentToServer)
    }

    @Test
    fun clearPrefs() {
        initEthGcmRegistrationWitWallet(wallet)
        gcmPrefs.setEthGcmTokenSentToServer(network.id, true)
        assertTrue(gcmPrefs.isEthGcmTokenSentToServer(network.id))
        ethGcmRegistration.clear()
        assertFalse(gcmPrefs.isEthGcmTokenSentToServer(network.id))
    }

    private fun initEthGcmRegistrationWitWallet(wallet: HDWallet) {
        initEthGcmRegistration()
        ethGcmRegistration.init(wallet)
    }

    private fun initEthGcmRegistration() {
        appPrefs.clear()
        gcmPrefs.clear()
        ethGcmRegistration = createEthGcmRegistration(appPrefs, gcmPrefs, ethApi, gcmToken)
    }
}