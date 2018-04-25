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

package com.toshi.managers.chatManager

import android.content.Context
import com.toshi.crypto.HdWalletBuilder
import com.toshi.model.local.Recipient
import com.toshi.model.local.User
import com.toshi.storage.TestWalletPrefs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ChatManagerTests {

    private val acceptedToshiId = "0x038e28d55eb16713ae20b2c0d3894d2857c7f29e"
    private val chatManager by lazy { ChatManagerMocker().mock(acceptedToshiId) }

    @Before
    fun before() {
        val acceptedRecipient = Recipient(User(acceptedToshiId))
        chatManager.loadConversationOrCreateNew(acceptedRecipient.threadId).toBlocking().value()
        val context = Mockito.mock(Context::class.java)
        val walletBuilder = HdWalletBuilder(TestWalletPrefs(), context)
        val wallet = walletBuilder.createWallet().toBlocking().value()
        chatManager.init(wallet).await()
    }

    @Test
    fun loadSingleConversation() {
        val conversation = chatManager.loadConversation(acceptedToshiId).toBlocking().value()
        assertEquals(acceptedToshiId, conversation.threadId)
    }

    @Test
    fun loadAcceptedConversations() {
        val conversation = chatManager.loadAllAcceptedConversations().toBlocking().value()
        assertEquals(true, conversation.size == 1)
    }

    @Test
    fun muteConversation() {
        chatManager.muteConversation(acceptedToshiId).await()
        val mutedConversation = chatManager.loadConversation(acceptedToshiId).toBlocking().value()
        assertEquals(true, mutedConversation.conversationStatus.isMuted)
        chatManager.unmuteConversation(acceptedToshiId).await()
        val unmutedConversation = chatManager.loadConversation(acceptedToshiId).toBlocking().value()
        assertEquals(false, unmutedConversation.conversationStatus.isMuted)
    }


}