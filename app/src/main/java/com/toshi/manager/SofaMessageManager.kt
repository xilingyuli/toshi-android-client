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

package com.toshi.manager


import com.toshi.BuildConfig
import com.toshi.R
import com.toshi.crypto.HDWallet
import com.toshi.crypto.signal.ChatService
import com.toshi.crypto.signal.store.ProtocolStore
import com.toshi.crypto.signal.store.SignalTrustStore
import com.toshi.manager.chat.SofaMessageReceiver
import com.toshi.manager.chat.SofaMessageRegistration
import com.toshi.manager.chat.SofaMessageSender
import com.toshi.manager.chat.tasks.NewGroupMembersTask
import com.toshi.manager.chat.tasks.NewGroupNameTask
import com.toshi.manager.model.SofaMessageTask
import com.toshi.manager.store.ConversationStore
import com.toshi.model.local.Conversation
import com.toshi.model.local.ConversationObservables
import com.toshi.model.local.Group
import com.toshi.model.local.IncomingMessage
import com.toshi.model.local.Recipient
import com.toshi.model.local.User
import com.toshi.model.sofa.Init
import com.toshi.model.sofa.SofaAdapters
import com.toshi.model.sofa.SofaMessage
import com.toshi.util.LocaleUtil
import com.toshi.util.logging.LogUtil
import com.toshi.util.sharedPrefs.SignalPrefs
import com.toshi.view.BaseApplication
import com.toshi.view.notification.ChatNotificationManager
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl

import rx.Completable
import rx.Observable
import rx.Single
import rx.Subscription
import rx.schedulers.Schedulers

class SofaMessageManager(
        private val conversationStore: ConversationStore = ConversationStore(),
        private val baseApplication: BaseApplication = BaseApplication.get(),
        private val trustStore: SignalTrustStore = SignalTrustStore(),
        private val signalServiceUrl: SignalServiceUrl = SignalServiceUrl(baseApplication.getString(R.string.chat_url), trustStore),
        private val signalServiceUrls: Array<SignalServiceUrl> = Array(1, { signalServiceUrl }),
        private var protocolStore: ProtocolStore = ProtocolStore().init(),
        private val userAgent: String = "Android " + BuildConfig.APPLICATION_ID + " - " + BuildConfig.VERSION_NAME + ":" + BuildConfig.VERSION_CODE
) {

    private var chatService: ChatService? = null
    private var messageRegister: SofaMessageRegistration? = null
    private var messageReceiver: SofaMessageReceiver? = null
    private var messageSender: SofaMessageSender? = null
    private var connectivitySub: Subscription? = null
    private var wallet: HDWallet? = null

    fun init(wallet: HDWallet): Completable {
        this.wallet = wallet
        return initEverything(wallet)
    }

    private fun initEverything(wallet: HDWallet): Completable {
        initChatService(wallet)
        initSenderAndReceiver(wallet)
        return initRegistrationTask()
                .doOnCompleted { attachConnectivityObserver() }
    }

    private fun initChatService(wallet: HDWallet) {
        chatService = ChatService(signalServiceUrls, wallet, protocolStore, userAgent)
    }

    private fun initSenderAndReceiver(wallet: HDWallet) {
        val messageSender = initMessageSender(wallet, protocolStore, conversationStore, signalServiceUrls)
        this.messageReceiver = initMessageReceiver(wallet, protocolStore, conversationStore, signalServiceUrls, messageSender)
        this.messageSender = messageSender
    }

    private fun initMessageSender(wallet: HDWallet, protocolStore: ProtocolStore,
                                  conversationStore: ConversationStore,
                                  signalServiceUrls: Array<SignalServiceUrl>): SofaMessageSender {
        return messageSender ?: SofaMessageSender(
                wallet,
                protocolStore,
                conversationStore,
                signalServiceUrls
        )
    }

    private fun initMessageReceiver(wallet: HDWallet, protocolStore: ProtocolStore, conversationStore: ConversationStore,
                                    signalServiceUrls: Array<SignalServiceUrl>, messageSender: SofaMessageSender): SofaMessageReceiver {
        return messageReceiver ?: SofaMessageReceiver(
                wallet,
                protocolStore,
                conversationStore,
                signalServiceUrls,
                messageSender
        )
    }

    private fun attachConnectivityObserver() {
        clearConnectivitySubscription()
        connectivitySub =
                baseApplication
                .isConnectedSubject
                .subscribeOn(Schedulers.io())
                .filter { isConnected -> isConnected }
                .subscribe(
                        { handleConnectivity() },
                        { LogUtil.exception("Error checking connection state", it) }
                )
    }

    private fun handleConnectivity() {
        redoRegistrationTask()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { },
                        { LogUtil.exception("Error during registration task", it) }
                )
    }

    private fun initRegistrationTask(): Completable {
        return if (messageRegister != null) return Completable.complete()
        else initSofaMessageRegistration()
    }

    private fun initSofaMessageRegistration(): Completable {
        val messageRegister = SofaMessageRegistration(chatService, protocolStore)
        this.messageRegister = messageRegister
        return messageRegister
                .registerIfNeeded()
                .doOnCompleted { messageReceiver?.receiveMessagesAsync() }
    }

    private fun redoRegistrationTask(): Completable {
        val messageRegister = messageRegister ?: SofaMessageRegistration(chatService, protocolStore)
        this.messageRegister = messageRegister
        return messageRegister
                .registerIfNeededWithOnboarding()
                .doOnCompleted { messageReceiver?.receiveMessagesAsync() }
    }

    // Will send the message to a remote peer
    // and store the message in the local database
    fun sendAndSaveMessage(receiver: Recipient, message: SofaMessage) {
        val messageTask = SofaMessageTask(receiver, message, SofaMessageTask.SEND_AND_SAVE)
        messageSender?.addNewTask(messageTask)
    }

    // Will send the message to a remote peer
    // but not store the message in the local database
    fun sendMessage(recipient: Recipient, message: SofaMessage) {
        val messageTask = SofaMessageTask(recipient, message, SofaMessageTask.SEND_ONLY)
        messageSender?.addNewTask(messageTask)
    }

    // Will send an init message to remote peer
    fun sendInitMessage(sender: User, recipient: Recipient) {
        val initMessage = Init()
                .setPaymentAddress(sender.paymentAddress)
                .setLanguage(LocaleUtil.getLocale().language)
        val messageBody = SofaAdapters.get().toJson(initMessage)
        val sofaMessage = SofaMessage().makeNew(sender, messageBody)
        val messageTask = SofaMessageTask(recipient, sofaMessage, SofaMessageTask.SEND_ONLY)
        messageSender?.addNewTask(messageTask)
    }

    // Will store a transaction in the local database
    // but not send the message to a remote peer. It will also save the state as "SENDING".
    fun saveTransaction(user: User, message: SofaMessage) {
        val recipient = Recipient(user)
        val messageTask = SofaMessageTask(recipient, message, SofaMessageTask.SAVE_TRANSACTION)
        messageSender?.addNewTask(messageTask)
    }

    // Updates a pre-existing message.
    fun updateMessage(recipient: Recipient, message: SofaMessage) {
        val messageTask = SofaMessageTask(recipient, message, SofaMessageTask.UPDATE_MESSAGE)
        messageSender?.addNewTask(messageTask)
    }

    fun resendPendingMessage(sofaMessage: SofaMessage) = messageSender?.sendPendingMessage(sofaMessage)

    // Create a new group
    fun createConversationFromGroup(group: Group): Single<Conversation> {
        return messageSender
                ?.createGroup(group)
                ?.flatMap { conversationStore.createNewConversationFromGroup(it) }
                ?: Single.error(IllegalStateException("SofaMessageSender is null while createConversationFromGroup"))
    }

    fun updateConversationFromGroup(group: Group): Completable {
        return baseApplication
                .userManager
                .getCurrentUser()
                .map { it?.getToshiId() ?: throw IllegalStateException("Local user is null while updateConversationFromGroup") }
                .flatMapCompletable { updateGroup(group, it) }
                .andThen(sendGroupUpdate(group))
                .subscribeOn(Schedulers.io())
    }

    private fun sendGroupUpdate(group: Group): Completable {
        return messageSender
                ?.sendGroupUpdate(group)
                ?: Completable.error(IllegalStateException("SofaMessageSender is null while sendGroupUpdate"))
    }

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
        return conversationStore.saveGroupAvatar(group.id, group.avatar)
                .onErrorComplete()
    }

    fun leaveGroup(group: Group): Completable {
        return messageSender
                ?.leaveGroup(group)
                ?.andThen(conversationStore.deleteByThreadId(group.id))
                ?.doAfterTerminate { ChatNotificationManager.removeNotificationsForConversation(group.id) }
                ?.subscribeOn(Schedulers.io())
                ?: Completable.error(IllegalStateException("SofaMessageSender is nul while leaveGroup"))
    }

    fun resumeMessageReceiving() {
        if (haveRegisteredWithServer() && wallet != null && messageReceiver != null) {
            messageReceiver?.receiveMessagesAsync()
        }
    }

    private fun haveRegisteredWithServer(): Boolean = SignalPrefs.getRegisteredWithServer()

    fun loadAllAcceptedConversations(): Single<List<Conversation>> {
        return conversationStore
                .loadAllAcceptedConversation()
                .subscribeOn(Schedulers.io())
    }

    fun loadAllUnacceptedConversations(): Single<List<Conversation>> {
        return conversationStore
                .loadAllUnacceptedConversation()
                .subscribeOn(Schedulers.io())
    }

    fun loadConversation(threadId: String): Single<Conversation> {
        return conversationStore
                .loadByThreadId(threadId)
                .subscribeOn(Schedulers.io())
    }

    fun loadConversationAndResetUnreadCounter(threadId: String): Single<Conversation> {
        return loadConversation(threadId)
                .flatMap { createEmptyConversationIfNullAndSetToAccepted(it, threadId) }
                .doOnSuccess { conversationStore.resetUnreadMessageCounter(it.threadId) }
    }

    private fun createEmptyConversationIfNullAndSetToAccepted(conversation: Conversation?, threadId: String): Single<Conversation> {
        return if (conversation != null) Single.just(conversation)
        else baseApplication
                .recipientManager
                .getUserFromToshiId(threadId)
                .map { Recipient(it) }
                .flatMap { conversationStore.createEmptyConversation(it) }
    }

    fun deleteConversation(conversation: Conversation): Completable {
        return conversationStore
                .deleteByThreadId(conversation.threadId)
                .subscribeOn(Schedulers.io())
    }

    fun deleteMessage(recipient: Recipient, sofaMessage: SofaMessage): Completable {
        return conversationStore
                .deleteMessageById(recipient, sofaMessage)
    }

    fun registerForAllConversationChanges(): Observable<Conversation> {
        return conversationStore.conversationChangedObservable
    }

    fun registerForConversationChanges(threadId: String): ConversationObservables {
        return conversationStore.registerForChanges(threadId)
    }

    fun registerForDeletedMessages(threadId: String): Observable<SofaMessage> {
        return conversationStore.registerForDeletedMessages(threadId)
    }

    fun stopListeningForChanges(threadId: String) {
        conversationStore.stopListeningForChanges(threadId)
    }

    fun areUnreadMessages(): Single<Boolean> {
        return Single
                .fromCallable { conversationStore.areUnreadMessages() }
                .subscribeOn(Schedulers.io())
    }

    fun getSofaMessageById(id: String): Single<SofaMessage> {
        return conversationStore
                .getSofaMessageById(id)
                .subscribeOn(Schedulers.io())
    }

    fun isConversationMuted(threadId: String): Single<Boolean> {
        return conversationStore
                .loadByThreadId(threadId)
                .map { it.conversationStatus.isMuted }
                .subscribeOn(Schedulers.io())
    }

    fun muteConversation(threadId: String): Completable {
        return conversationStore
                .loadByThreadId(threadId)
                .flatMap { muteConversation(it) }
                .subscribeOn(Schedulers.io())
                .toCompletable()
    }

    fun unmuteConversation(threadId: String): Completable {
        return conversationStore
                .loadByThreadId(threadId)
                .flatMap { unmuteConversation(it) }
                .subscribeOn(Schedulers.io())
                .toCompletable()
    }

    fun muteConversation(conversation: Conversation): Single<Conversation> {
        return conversationStore
                .muteConversation(conversation, true)
                .subscribeOn(Schedulers.io())
    }

    fun unmuteConversation(conversation: Conversation): Single<Conversation> {
        return conversationStore
                .muteConversation(conversation, false)
                .subscribeOn(Schedulers.io())
    }

    fun acceptConversation(conversation: Conversation): Single<Conversation> {
        return conversationStore
                .acceptConversation(conversation)
                .subscribeOn(Schedulers.io())
    }

    fun rejectConversation(conversation: Conversation): Single<Conversation> {
        return if (conversation.isGroup) {
            leaveGroup(conversation.recipient.group)
                    .toSingle { conversation }
        } else baseApplication
                .recipientManager
                .blockUser(conversation.threadId)
                .andThen(deleteConversation(conversation))
                .toSingle { conversation }
    }

    fun tryUnregisterGcm(): Completable {
        return messageRegister
                ?.tryUnregisterGcm()
                ?: Completable.error(IllegalStateException("Unable to register as class hasn't been initialised yet."))
    }

    fun forceRegisterChatGcm(): Completable {
        return messageRegister
                ?.registerChatGcm()
                ?: Completable.error(IllegalStateException("Unable to register as class hasn't been initialised yet."))
    }

    @Throws(InterruptedException::class)
    fun fetchLatestMessage(): Single<IncomingMessage> {
        while (messageReceiver == null) Thread.sleep(200)
        return messageReceiver
                ?.fetchLatestMessage()
                ?: Single.error(IllegalStateException("SofaMessageReceiver is null while fetchLatestMessage"))
    }

    fun clear() {
        clearMessageReceiver()
        clearMessageSender()
        clearConnectivitySubscription()
    }

    private fun clearMessageReceiver() {
        messageReceiver?.shutdown()
        messageReceiver = null
    }

    private fun clearMessageSender() {
        messageSender?.clear()
        messageSender = null
    }

    private fun clearConnectivitySubscription() = connectivitySub?.unsubscribe()

    fun deleteSession() = protocolStore.deleteAllSessions()

    fun disconnect() = messageReceiver?.shutdown()
}
