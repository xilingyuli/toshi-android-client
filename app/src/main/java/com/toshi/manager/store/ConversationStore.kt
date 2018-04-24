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

package com.toshi.manager.store


import com.toshi.extensions.isLocalStatusMessage
import com.toshi.model.local.Avatar
import com.toshi.model.local.Conversation
import com.toshi.model.local.ConversationObservables
import com.toshi.model.local.Group
import com.toshi.model.local.Recipient
import com.toshi.model.local.User
import com.toshi.model.sofa.SofaMessage
import com.toshi.util.logging.LogUtil
import com.toshi.util.statusMessage.StatusMessageBuilder
import com.toshi.view.BaseApplication
import io.realm.Sort
import org.whispersystems.signalservice.api.messages.SignalServiceGroup
import rx.Completable
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.util.concurrent.Executors

class ConversationStore(
        private val scheduler: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor()),
        private val baseApplication: BaseApplication = BaseApplication.get()
) {

    companion object {
        private const val FIFTEEN_MINUTES = 1000 * 60 * 15
        private const val THREAD_ID_FIELD = "threadId"
        private const val MESSAGE_ID_FIELD = "privateKey"
    }

    private var watchedThreadId: String? = null
    private val newMessageSubject = PublishSubject.create<SofaMessage>()
    private val updateMessageSubject = PublishSubject.create<SofaMessage>()
    private val deletedMessageSubject = PublishSubject.create<SofaMessage>()
    private val conversationChangedSubject = PublishSubject.create<Conversation>()
    private val conversationUpdatedSubject = PublishSubject.create<Conversation>()

    val conversationChangedObservable: Observable<Conversation>
        get() = conversationChangedSubject
                .filter { thread -> thread != null }

    //##############################################################################################
    // Observables
    //##############################################################################################
    fun registerForChanges(threadId: String): ConversationObservables {
        watchedThreadId = threadId
        return ConversationObservables(newMessageSubject, updateMessageSubject, conversationUpdatedSubject)
    }

    fun registerForDeletedMessages(threadId: String): Observable<SofaMessage> {
        watchedThreadId = threadId
        return deletedMessageSubject.asObservable()
    }

    fun stopListeningForChanges(threadId: String) {
        // Avoids the race condition where a second activity has already registered
        // before the first activity is destroyed. Thus the first activity can't deregister
        // changes for the second activity.
        if (watchedThreadId != null && watchedThreadId == threadId) {
            watchedThreadId = null
        }
    }

    //##############################################################################################
    // Creation
    //##############################################################################################

    fun saveSignalGroup(signalGroup: SignalServiceGroup): Single<Conversation> {
        val group = Group(signalGroup)
        return copyOrUpdateGroup(group)
                .doOnSuccess { broadcastConversationChanged(it) }
                .doOnError { LogUtil.exception("Error while saving group $it") }
    }

    fun createNewConversationFromGroup(group: Group): Single<Conversation> {
        return createEmptyConversation(Recipient(group))
                .flatMap { addGroupCreatedStatusMessage(it) }
                .observeOn(Schedulers.immediate())
                .doOnSuccess { broadcastConversationChanged(it) }
                .doOnError { handleError(it, "Error while creating new conversation from group") }
    }

    fun createEmptyConversation(recipient: Recipient): Single<Conversation> {
        return Single.fromCallable {
            val realm = baseApplication.realm
            realm.beginTransaction()
            val conversation = Conversation(recipient)
            conversation.conversationStatus.isAccepted = true
            realm.copyToRealmOrUpdate(conversation)
            realm.commitTransaction()
            realm.close()
            conversation
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while creating empty conversation") }
    }

    private fun copyOrUpdateGroup(group: Group): Single<Conversation> {
        return Single.fromCallable {
            val conversationToStore = getOrCreateConversation(group)
            val realm = baseApplication.realm
            realm.beginTransaction()
            conversationToStore.updateRecipient(Recipient(group))
            val storedConversation = realm.copyToRealmOrUpdate(conversationToStore)
            realm.commitTransaction()
            val conversationForBroadcast = realm.copyFromRealm(storedConversation)
            realm.close()
            return@fromCallable conversationForBroadcast
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while updating group") }
    }

    private fun getOrCreateConversation(group: Group): Conversation {
        val recipient = Recipient(group)
        return getOrCreateConversation(recipient)
    }

    private fun getOrCreateConversation(recipient: Recipient): Conversation {
        val existingConversation = loadWhere(THREAD_ID_FIELD, recipient.threadId)
        return existingConversation ?: Conversation(recipient)
    }

    //##############################################################################################
    // Saving Messages
    //##############################################################################################

    fun saveNewMessageSingle(receiver: Recipient, message: SofaMessage): Single<Conversation> {
        return saveMessage(receiver, message)
                .observeOn(Schedulers.immediate())
                .doOnSuccess { broadcastConversationChanged(it) }
                .doOnError { handleError(it, "Error while saving message") }
    }

    fun saveNewMessage(receiver: Recipient, message: SofaMessage) {
        saveMessage(receiver, message)
                .observeOn(Schedulers.immediate())
                .subscribe(
                        { broadcastConversationChanged(it) },
                        { handleError(it, "Error while saving new message") }
                )
    }

    private fun saveMessage(receiver: Recipient, message: SofaMessage?): Single<Conversation> {
        return Single.fromCallable {
            val conversationToStore = getOrCreateConversation(receiver)

            if (message != null && shouldSaveTimestampMessage(message, conversationToStore)) {
                val timestampMessage = generateTimestampMessage()
                conversationToStore.addMessage(timestampMessage)
                broadcastNewChatMessage(receiver.threadId, timestampMessage)
            }

            val realm = baseApplication.realm
            realm.beginTransaction()

            if (message != null) {
                val storedMessage = realm.copyToRealmOrUpdate(message)
                val updateUnreadCounter = conversationToStore.threadId != watchedThreadId && !storedMessage.isLocalStatusMessage()
                if (updateUnreadCounter) conversationToStore.setLatestMessageAndUpdateUnreadCounter(storedMessage)
                else conversationToStore.setLatestMessage(storedMessage)
                broadcastNewChatMessage(receiver.threadId, message)
            }

            val storedConversation = realm.copyToRealmOrUpdate(conversationToStore)
            realm.commitTransaction()
            val conversationForBroadcast = realm.copyFromRealm(storedConversation)
            realm.close()

            return@fromCallable conversationForBroadcast
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while saving new message") }
    }

    fun updateMessage(receiver: Recipient, message: SofaMessage) {
        Completable.fromAction {
            val realm = baseApplication.realm
            realm.beginTransaction()
            realm.insertOrUpdate(message)
            realm.commitTransaction()
            realm.close()
        }
        .observeOn(Schedulers.immediate())
        .subscribeOn(scheduler)
        .subscribe(
                { broadcastUpdatedChatMessage(receiver.threadId, message) },
                { handleError(it, "Error while updating message") }
        )
    }

    private fun updateLatestMessage(threadId: String): Completable {
        return Completable.fromAction {
            val realm = baseApplication.realm
            val conversation = realm
                    .where(Conversation::class.java)
                    .equalTo(THREAD_ID_FIELD, threadId)
                    .findFirst()
            if (conversation == null) {
                realm.close()
                return@fromAction
            }
            if (conversation.allMessages != null && conversation.allMessages.size > 0) {
                val lastMessage = conversation.allMessages[conversation.allMessages.size - 1]
                realm.beginTransaction()
                conversation.updateLatestMessage(lastMessage)
                realm.commitTransaction()
            }
            realm.close()
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while updating latest message") }
    }

    //##############################################################################################
    // Status Messages
    //##############################################################################################

    fun addAddedToGroupStatusMessage(conversation: Conversation): Single<Conversation> {
        if (!conversation.recipient.isGroup) throw IllegalStateException("Adding status message to non-group")
        val statusMessage = StatusMessageBuilder.buildAddedToGroupStatusMessage(conversation.recipient.group)
        return saveMessage(conversation.recipient, statusMessage)
    }

    private fun addGroupCreatedStatusMessage(conversation: Conversation): Single<Conversation> {
        val localStatusMessage = StatusMessageBuilder.buildGroupCreatedStatusMessage()
        return saveMessage(conversation.recipient, localStatusMessage)
    }

    private fun addUserLeftStatusMessage(conversation: Conversation, sender: User): Single<Conversation> {
        val localStatusMessage = StatusMessageBuilder.buildUserLeftStatusMessage(sender)
        return saveMessage(conversation.recipient, localStatusMessage)
    }

    fun addNewGroupMembersStatusMessage(recipient: Recipient, sender: User?, newUsers: List<User>): Single<Conversation> {
        val statusMessage = StatusMessageBuilder.buildAddStatusMessage(sender, newUsers)
                ?: throw IllegalStateException("Status message is null")
        return saveMessage(recipient, statusMessage)
    }

    fun addGroupNameUpdatedStatusMessage(recipient: Recipient, sender: User?, updatedGroupName: String): Single<Conversation> {
        val statusMessage = StatusMessageBuilder.addGroupNameUpdatedStatusMessage(sender, updatedGroupName)
        return saveMessage(recipient, statusMessage)
    }

    //##############################################################################################
    // Timestamp
    //##############################################################################################

    private fun generateTimestampMessage(): SofaMessage = SofaMessage().makeNewTimeStampMessage()

    private fun shouldSaveTimestampMessage(message: SofaMessage, conversation: Conversation): Boolean {
        if (!message.isUserVisible) return false
        val newMessageTimestamp = message.creationTime
        val latestMessageTimestamp = conversation.updatedTime
        return newMessageTimestamp - latestMessageTimestamp > FIFTEEN_MINUTES
    }

    //##############################################################################################
    // Group Updates
    //##############################################################################################
    fun saveGroupAvatar(groupId: String, avatar: Avatar?): Completable {
        return loadByThreadId(groupId)
                .map { it?.recipient?.group }
                .map { it.setAvatar(avatar) }
                .flatMapCompletable { saveGroup(it) }
                .doOnError { handleError(it, "Error while saving group avatar") }
    }

    fun saveGroupTitle(groupId: String, title: String): Completable {
        return loadByThreadId(groupId)
                .map { it?.recipient?.group }
                .map { it.setTitle(title) }
                .flatMapCompletable { saveGroup(it) }
                .doOnError { handleError(it, "Error while saving group title") }
    }

    fun addNewMembersToGroup(groupId: String, newMembers: List<User>): Completable {
        return loadByThreadId(groupId)
                .map { it?.recipient?.group }
                .map { it.addMembers(newMembers) }
                .flatMapCompletable { saveGroup(it) }
                .doOnError { handleError(it, "Error while adding new members to group") }
    }

    fun removeUserFromGroup(groupId: String, user: User): Completable {
        return loadByThreadId(groupId)
                .flatMap { addUserLeftStatusMessage(it, user) }
                .map { it.recipient.group }
                .map { it.removeMember(user) }
                .flatMapCompletable { saveGroup(it) }
                .doOnError { handleError(it, "Error while removing user from group") }
    }

    private fun saveGroup(group: Group): Completable {
        return copyOrUpdateGroup(group)
                .observeOn(Schedulers.immediate())
                .doOnSuccess { broadcastConversation(it) }
                .doOnError { handleError(it, "Error while saving group") }
                .toCompletable()
    }

    //##############################################################################################
    // Reading from DB
    //##############################################################################################

    fun loadAllAcceptedConversation(): Single<List<Conversation>> = loadAllConversations(true)

    fun loadAllUnacceptedConversation(): Single<List<Conversation>> = loadAllConversations(false)

    private fun loadAllConversations(isAccepted: Boolean): Single<List<Conversation>> {
        return Single.fromCallable {
            val realm = baseApplication.realm
            val query = realm.where(Conversation::class.java)
                    .equalTo("conversationStatus.isAccepted", isAccepted)
                    .isNotEmpty("allMessages")
            val results = query
                    .sort("updatedTime", Sort.DESCENDING)
                    .findAll()
            val allConversations = realm.copyFromRealm(results)
            realm.close()
            return@fromCallable allConversations
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while loading all conversations") }
    }

    fun loadByThreadId(threadId: String): Single<Conversation?> {
        return Single.fromCallable { loadWhere(THREAD_ID_FIELD, threadId) }
                .subscribeOn(scheduler)
                .doOnError { handleError(it, "Error while loading thread by id") }
    }

    private fun loadWhere(fieldName: String, value: String): Conversation? {
        val realm = baseApplication.realm
        val result = realm
                .where(Conversation::class.java)
                .equalTo(fieldName, value)
                .findFirst()
        val queriedConversation = if (result == null) null else realm.copyFromRealm(result)
        realm.close()
        return queriedConversation
    }

    fun areUnreadMessages(): Boolean {
        val realm = baseApplication.realm
        val result = realm
                .where(Conversation::class.java)
                .greaterThan("numberOfUnread", 0)
                .findFirst()
        val areUnreadMessages = result != null
        realm.close()
        return areUnreadMessages
    }

    fun getSofaMessageById(id: String): Single<SofaMessage?> {
        return Single.fromCallable {
            val realm = baseApplication.realm
            val result = realm
                    .where(SofaMessage::class.java)
                    .equalTo("privateKey", id)
                    .findFirst()
            val sofaMessage = realm.copyFromRealm(result)
            realm.close()
            return@fromCallable sofaMessage
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while getting message by id") }
    }


    //##############################################################################################
    // Deletion
    //##############################################################################################

    fun deleteByThreadId(threadId: String): Completable {
        return Completable.fromAction {
            val realm = baseApplication.realm
            realm.beginTransaction()
            val conversationToDelete = realm
                    .where(Conversation::class.java)
                    .equalTo(THREAD_ID_FIELD, threadId)
                    .findFirst()
            conversationToDelete?.cascadeDelete()
            realm.commitTransaction()
            realm.close()
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while deleting thread by id") }
    }

    fun deleteMessageById(receiver: Recipient, message: SofaMessage): Completable {
        return Completable.fromAction {
            val realm = baseApplication.realm
            realm.beginTransaction()
            realm
                    .where(SofaMessage::class.java)
                    .equalTo(MESSAGE_ID_FIELD, message.privateKey)
                    .findFirst()
                    ?.deleteFromRealm()
            realm.commitTransaction()
            realm.close()
        }
        .observeOn(Schedulers.immediate())
        .subscribeOn(scheduler)
        .andThen(updateLatestMessage(receiver.threadId))
        .doOnCompleted { broadcastDeletedChatMessage(receiver.threadId, message) }
        .doOnError { handleError(it, "Error while deleting message by id") }
    }

    //##############################################################################################
    // Set Conversation State
    //##############################################################################################

    fun muteConversation(conversation: Conversation, mute: Boolean): Single<Conversation> {
        return Single.fromCallable {
            val realm = baseApplication.realm
            realm.beginTransaction()
            conversation.conversationStatus.isMuted = mute
            realm.copyToRealmOrUpdate(conversation)
            realm.commitTransaction()
            realm.close()
            return@fromCallable conversation
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while muting conversation") }
    }

    fun acceptConversation(conversation: Conversation): Single<Conversation> {
        return Single.fromCallable {
            val realm = baseApplication.realm
            realm.beginTransaction()
            conversation.conversationStatus.isAccepted = true
            realm.copyToRealmOrUpdate(conversation)
            realm.commitTransaction()
            realm.close()
            return@fromCallable conversation
        }
        .subscribeOn(scheduler)
        .doOnError { handleError(it, "Error while accepting conversation") }
    }

    fun resetUnreadMessageCounter(threadId: String) {
        Single.fromCallable {
            val storedConversation = loadWhere(THREAD_ID_FIELD, threadId)
            val realm = baseApplication.realm
            realm.beginTransaction()
            storedConversation.resetUnreadCounter()
            realm.insertOrUpdate(storedConversation)
            realm.commitTransaction()
            realm.close()
            return@fromCallable storedConversation
        }
        .observeOn(Schedulers.immediate())
        .subscribeOn(scheduler)
        .subscribe(
                { broadcastConversationChanged(it) },
                { handleError(it, "Error while resetting unread message counter") }
        )
    }

    //##############################################################################################
    // Broadcasting changes
    //##############################################################################################

    private fun broadcastNewChatMessage(threadId: String, newMessage: SofaMessage?) {
        if (watchedThreadId == null || watchedThreadId != threadId) return
        newMessageSubject.onNext(newMessage)
    }

    private fun broadcastUpdatedChatMessage(threadId: String, updatedMessage: SofaMessage) {
        if (watchedThreadId == null || watchedThreadId != threadId) return
        updateMessageSubject.onNext(updatedMessage)
    }

    private fun broadcastDeletedChatMessage(threadId: String, deletedMessage: SofaMessage) {
        if (watchedThreadId == null || watchedThreadId != threadId) return
        deletedMessageSubject.onNext(deletedMessage)
    }

    private fun broadcastConversation(conversation: Conversation) {
        broadcastConversationChanged(conversation)
        broadcastConversationUpdated(conversation)
    }

    private fun broadcastConversationChanged(conversation: Conversation) {
        conversationChangedSubject.onNext(conversation)
    }

    private fun broadcastConversationUpdated(conversation: Conversation) {
        if (watchedThreadId == null || watchedThreadId != conversation.threadId) return
        conversationUpdatedSubject.onNext(conversation)
    }

    private fun handleError(throwable: Throwable, message: String) = LogUtil.exception(message, throwable)
}
