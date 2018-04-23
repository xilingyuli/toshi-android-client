package com.toshi.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.toshi.extensions.isLocalStatusMessage
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class MainViewModel : ViewModel() {

    private val subscriptions by lazy { CompositeSubscription() }
    val unreadMessages by lazy { MutableLiveData<Boolean>() }

    init {
        attachUnreadMessagesSubscription()
    }

    private fun attachUnreadMessagesSubscription() {
        val allChangesSubscription = getChatManager()
                .registerForAllConversationChanges()
                .filter { !it.latestMessage.isLocalStatusMessage() }
                .flatMap { getChatManager().areUnreadMessages().toObservable() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { unreadMessages.value = it },
                        { LogUtil.exception("Error while fetching unread messages $it") }
                )

        val firstTimeSubscription = getChatManager()
                .areUnreadMessages()
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { unreadMessages.value = it },
                        { LogUtil.exception("Error while fetching unread messages $it") }
                )

        this.subscriptions.addAll(allChangesSubscription, firstTimeSubscription)
    }

    private fun getChatManager() = BaseApplication.get().chatManager

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}