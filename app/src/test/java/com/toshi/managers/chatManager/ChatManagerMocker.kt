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

import com.toshi.manager.ChatManager
import com.toshi.manager.store.ConversationStore
import com.toshi.managers.baseApplication.BaseApplicationMocker
import com.toshi.managers.recipientManager.RecipientManagerMocker
import com.toshi.managers.userManager.UserManagerMocker
import com.toshi.storage.TestToshiDB
import rx.schedulers.Schedulers

class ChatManagerMocker {
    fun mock(toshiId: String): ChatManager {
        val recipientManager = RecipientManagerMocker().mock(toshiId)
        val userManager = UserManagerMocker().mock(recipientManager = recipientManager, toshiId = toshiId)
        val baseApplication = BaseApplicationMocker().mock()
        val conversationStore = ConversationStore(TestToshiDB())

        return ChatManager(
                recipientManager = recipientManager,
                userManager = userManager,
                conversationStore = conversationStore,
                baseApplication = baseApplication,
                scheduler = Schedulers.trampoline()
        )
    }
}