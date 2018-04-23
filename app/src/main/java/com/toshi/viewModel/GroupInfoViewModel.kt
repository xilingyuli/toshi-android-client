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

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.toshi.R
import com.toshi.model.local.Group
import com.toshi.util.SingleLiveEvent
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class GroupInfoViewModel : ViewModel() {

    private val recipientManager = BaseApplication.get().recipientManager
    private val sofaMessageManager = BaseApplication.get().sofaMessageManager
    private val subscriptions by lazy { CompositeSubscription() }

    val group by lazy { SingleLiveEvent<Group>() }
    val fetchGroupError by lazy { SingleLiveEvent<Throwable>() }
    val leaveGroup by lazy { SingleLiveEvent<Boolean>() }
    val leaveGroupError by lazy { SingleLiveEvent<Throwable>() }
    val isMuted by lazy { MutableLiveData<Boolean>() }
    val isMutedError by lazy { SingleLiveEvent<Int>() }
    val isUpdatingMuteState by lazy { MutableLiveData<Boolean>() }

    fun fetchGroup(groupId: String) {
        val subscription = recipientManager
                .getGroupFromId(groupId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { isMuted(it) }
                .subscribe(
                        { group.value = it },
                        { fetchGroupError.value = it }
                )
        subscriptions.add(subscription)
    }

    fun leaveGroup() {
        group.value?.let {
            val leaveSub = sofaMessageManager
                    .leaveGroup(it)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { leaveGroup.value = true },
                            { leaveGroupError.value = it }
                    )
            subscriptions.add(leaveSub)
        }
    }

    private fun isMuted(group: Group) {
        val sub = sofaMessageManager
                .isConversationMuted(group.id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { isMuted.value = it },
                        { LogUtil.exception("Error while GroupInfoViewModel::isMuted", it) }
                )

        subscriptions.add(sub)
    }

    fun muteConversation(isNotificationEnabled: Boolean) {
        if (isNotificationEnabled) muteConversation()
        else unmuteConversation()
    }

    private fun muteConversation() {
        val groupId = group.value?.id
        if (groupId == null) {
            isMutedError.value = R.string.mute_error
            return
        }

        val sub = sofaMessageManager
                .muteConversation(groupId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isUpdatingMuteState.value = true }
                .doOnTerminate { isUpdatingMuteState.value = false }
                .subscribe(
                        { isMuted.value = true },
                        { isMutedError.value = R.string.mute_error }
                )

        subscriptions.add(sub)
    }

    private fun unmuteConversation() {
        val groupId = group.value?.id
        if (groupId == null) {
            isMutedError.value = R.string.unmute_error
            return
        }

        val sub = sofaMessageManager
                .unmuteConversation(groupId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isUpdatingMuteState.value = true }
                .doOnTerminate { isUpdatingMuteState.value = false }
                .subscribe(
                        { isMuted.value = false },
                        { isMutedError.value = R.string.unmute_error }
                )

        subscriptions.add(sub)
    }

    override fun onCleared() {
        super.onCleared()
        this.subscriptions.clear()
    }
}