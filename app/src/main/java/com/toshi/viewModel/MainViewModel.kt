package com.toshi.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.toshi.extensions.isLocalStatusMessage
import com.toshi.model.local.network.Networks
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class MainViewModel : ViewModel() {

    private val chatManager by lazy { BaseApplication.get().chatManager }
    private val subscriptions by lazy { CompositeSubscription() }
    val unreadMessages by lazy { MutableLiveData<Boolean>() }

    init {
        attachUnreadMessagesSubscription()
    }

    private fun attachUnreadMessagesSubscription() {
        val allChangesSubscription = chatManager
                .registerForAllConversationChanges()
                .filter { !it.latestMessage.isLocalStatusMessage() }
                .flatMap { chatManager.areUnreadMessages().toObservable() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { unreadMessages.value = it },
                        { LogUtil.exception("Error while fetching unread messages $it") }
                )

        val firstTimeSubscription = chatManager
                .areUnreadMessages()
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { unreadMessages.value = it },
                        { LogUtil.exception("Error while fetching unread messages $it") }
                )

        subscriptions.addAll(allChangesSubscription, firstTimeSubscription)
    }

    fun getNetworks() = Networks.getInstance()

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}