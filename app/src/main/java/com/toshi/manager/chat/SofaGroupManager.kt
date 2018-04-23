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

package com.toshi.manager.chat

import com.toshi.manager.UserManager
import com.toshi.manager.chat.tasks.NewGroupMembersTask
import com.toshi.manager.chat.tasks.NewGroupNameTask
import com.toshi.manager.store.ConversationStore
import com.toshi.model.local.Conversation
import com.toshi.model.local.Group
import com.toshi.view.notification.ChatNotificationManager
import rx.Completable
import rx.Single

class SofaGroupManager(
        private val messageSender: SofaMessageSender,
        private val conversationStore: ConversationStore,
        private val userManager: UserManager
) {
    fun updateConversationFromGroup(group: Group): Completable {
        return userManager
                .getCurrentUser()
                .map { it?.getToshiId() ?: throw IllegalStateException("Local user is null while updateConversationFromGroup") }
                .flatMapCompletable { updateGroup(group, it) }
                .andThen(sendGroupUpdate(group))
    }

    private fun sendGroupUpdate(group: Group): Completable = messageSender.sendGroupUpdate(group)

    private fun updateGroup(group: Group, localUserId: String): Completable {
        return updateNewParticipants(group, localUserId)
                .andThen(updateGroupName(group, localUserId))
                .andThen(updateGroupAvatar(group))
    }

    private fun updateNewParticipants(group: Group, localUserId: String): Completable {
        return NewGroupMembersTask(conversationStore, true)
                .run(group.id, localUserId, group.memberIds)
                .onErrorComplete()
    }

    private fun updateGroupName(group: Group, localUserId: String): Completable {
        return NewGroupNameTask(conversationStore, true)
                .run(localUserId, group.id, group.title)
                .onErrorComplete()
    }

    private fun updateGroupAvatar(group: Group): Completable {
        if (group.avatar == null) Completable.complete()
        return conversationStore
                .saveGroupAvatar(group.id, group.avatar)
                .onErrorComplete()
    }

    fun leaveGroup(group: Group): Completable {
        return messageSender
                .leaveGroup(group)
                .andThen(conversationStore.deleteByThreadId(group.id))
                .doAfterTerminate { ChatNotificationManager.removeNotificationsForConversation(group.id) }
    }

    fun createConversationFromGroup(group: Group): Single<Conversation> {
        return messageSender
                .createGroup(group)
                .flatMap { conversationStore.createNewConversationFromGroup(it) }
    }
}