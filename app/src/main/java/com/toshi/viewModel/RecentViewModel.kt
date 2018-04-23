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

package com.toshi.viewModel

import android.arch.lifecycle.ViewModel
import com.toshi.model.local.Conversation
import com.toshi.model.local.ConversationInfo
import com.toshi.util.SingleLiveEvent
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import com.toshi.view.fragment.DialogFragment.Option
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class RecentViewModel : ViewModel() {

    private val subscriptions by lazy { CompositeSubscription() }

    val acceptedAndUnacceptedConversations by lazy { SingleLiveEvent<Pair<List<Conversation>, List<Conversation>>>() }
    val updatedConversation by lazy { SingleLiveEvent<Conversation>() }
    val updatedUnacceptedConversation by lazy { SingleLiveEvent<Conversation>() }
    val conversationInfo by lazy { SingleLiveEvent<ConversationInfo>() }
    val deleteConversation by lazy { SingleLiveEvent<Conversation>() }

    init {
        attachSubscriber()
    }

    private fun attachSubscriber() {
        val sub = getChatManager()
                .registerForAllConversationChanges()
                .filter { it.allMessages.size > 0 }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { handleUpdatedConversation(it) },
                        { LogUtil.w("Error fetching acceptedConversations $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun handleUpdatedConversation(conversation: Conversation) {
        if (conversation.conversationStatus.isAccepted) updatedConversation.value = conversation
        else updatedUnacceptedConversation.value = conversation
    }

    fun getAcceptedAndUnAcceptedConversations() {
        val sub = Single.zip(
                getChatManager().loadAllAcceptedConversations(),
                getChatManager().loadAllUnacceptedConversations(),
                { t1, t2 -> Pair(t1, t2) }
        )
        .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { acceptedAndUnacceptedConversations.value = it },
                        { LogUtil.w("Error fetching conversations $it") }
                )

        this.subscriptions.add(sub)
    }

    fun showConversationOptionsDialog(threadId: String) {
        val sub = Single.zip(
                getConversation(threadId),
                isMuted(threadId),
                isBlocked(threadId),
                { conversation, isMuted, isBlocked -> ConversationInfo(conversation, isMuted, isBlocked) }
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                { conversationInfo.value = it },
                { LogUtil.w("Error: $it") }
        )

        this.subscriptions.add(sub)
    }

    private fun getConversation(threadId: String) = getChatManager().loadConversation(threadId)

    private fun isBlocked(threadId: String) = getRecipientManager().isUserBlocked(threadId)

    private fun isMuted(threadId: String) = getChatManager().isConversationMuted(threadId)

    fun handleSelectedOption(conversation: Conversation, option: Option) {
        when (option) {
            Option.UNMUTE -> setMute(conversation, false)
            Option.MUTE -> setMute(conversation, true)
            Option.UNBLOCK -> setBlock(conversation, false)
            Option.BLOCK -> setBlock(conversation, true)
            Option.DELETE -> deleteConversation.value = conversation
        }
    }

    private fun setMute(conversation: Conversation, mute: Boolean) {
        val muteAction =
                if (mute) getChatManager().muteConversation(conversation)
                else getChatManager().unmuteConversation(conversation)

        val sub = muteAction
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { },
                        { LogUtil.w("Error while unmuting conversation $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun setBlock(conversation: Conversation, block: Boolean) {
        val muteAction =
                if (block) getRecipientManager().blockUser(conversation.threadId)
                else getRecipientManager().unblockUser(conversation.threadId)

        val sub = muteAction
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { },
                        { LogUtil.w("Error while blocking user $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun getChatManager() = BaseApplication.get().chatManager

    private fun getRecipientManager() = BaseApplication.get().recipientManager

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}