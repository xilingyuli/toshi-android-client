package com.toshi.crypto

import android.content.Context
import com.toshi.storage.TestWalletPrefs
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class HDWalletTest {

    private val expectedMasterSeed = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    // Path `m/0'/1/0/0
    private val expectedOwnerAddress = "0xa391af6a522436f335b7c6486640153641847ea2"

    // Path `m/44'/60'/0'/0'
    private val expectedPaymentAddress = "0x9858effd232b4033e47d90003d41ec34ecaeda94"

    // Mocks
    private lateinit var walletPrefs: TestWalletPrefs
    private lateinit var context: Context

    @Before
    fun setup() {
        walletPrefs = TestWalletPrefs()
        walletPrefs.setMasterSeed(expectedMasterSeed)
        context = Mockito.mock(Context::class.java)
    }

    @Test
    fun walletCreatedFromSeedUsesThatSeed() {
        val wallet = HDWallet(walletPrefs, context)
                .existingWallet
                .toBlocking()
                .value()
        assertThat(wallet.masterSeed, `is`(expectedMasterSeed))
    }

    @Test
    fun walletCreatedFromSeedDerivesCorrectOwnerAddress() {
        val wallet = HDWallet(walletPrefs, context)
                .existingWallet
                .toBlocking()
                .value()
        assertThat(wallet.ownerAddress, `is`(expectedOwnerAddress))
    }

    @Test
    fun walletCreatedFromSeedDerivesCorrectPaymentAddress() {
        val wallet = HDWallet(walletPrefs, context)
                .existingWallet
                .toBlocking()
                .value()
        assertThat(wallet.paymentAddress, `is`(expectedPaymentAddress))
    }
}