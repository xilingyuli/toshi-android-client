package com.toshi.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.toshi.R
import com.toshi.model.local.Group
import com.toshi.model.local.User
import com.toshi.util.SingleLiveEvent
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit

class AddGroupParticipantsViewModel(val groupId: String) : ViewModel() {

    private val chatManager by lazy { BaseApplication.get().chatManager }
    private val recipientManager by lazy { BaseApplication.get().recipientManager }

    private val subscriptions by lazy { CompositeSubscription() }
    private val querySubject by lazy { PublishSubject.create<String>() }
    private val participants by lazy { mutableListOf<User>() }
    private val defaultResults by lazy { mutableListOf<User>() }

    val searchResults by lazy { SingleLiveEvent<List<User>>() }
    val selectedParticipants by lazy { MutableLiveData<List<User>>() }
    val isUpdatingGroup by lazy { MutableLiveData<Boolean>() }
    val participantsAdded by lazy { SingleLiveEvent<Unit>() }
    val error by lazy { SingleLiveEvent<Int>() }

    init {
        subscribeForQueryChanges()
    }

    private fun subscribeForQueryChanges() {
        val startSearchSub = querySubject.debounce(500, TimeUnit.MILLISECONDS)
                .filter { query -> query.length >= 3 }
                .subscribe(
                        { runSearchQuery(it) },
                        { LogUtil.w("Error while listening for query changes $it") }
                )

        val clearSub = querySubject.filter { query -> query.length < 3 }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { showDefaultResults() },
                        { LogUtil.w("Error while listening for query changes $it") }
                )

        subscriptions.addAll(startSearchSub, clearSub)
    }

    private fun showDefaultResults() {
        if (searchResults.value != defaultResults) searchResults.value = defaultResults
    }

    fun queryUpdated(query: CharSequence?) = querySubject.onNext(query.toString())

    private fun runSearchQuery(query: String) {
        val searchSub =
                Single.zip(
                        recipientManager.searchOnlineUsers(query),
                        getGroupMembers(groupId),
                        { searchResult, groupMembers -> Pair(searchResult, groupMembers) }
                )
                .subscribeOn(Schedulers.io())
                .map { filterSearchResult(it.first, it.second) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { searchResults.value = it },
                        { LogUtil.w("Error while search for user $it") }
                )

        subscriptions.add(searchSub)
    }

    private fun filterSearchResult(searchResults: List<User>, groupMembers: List<User>): List<User> {
        val filteredList = mutableListOf<User>()
        for (searchUser in searchResults) {
            val contains = groupMembers.any { searchUser.toshiId == it.toshiId }
            if (!contains) filteredList.add(searchUser)
        }
        return filteredList
    }

    private fun getGroupMembers(groupId: String) = Group.fromId(groupId).map { it.members }

    fun toggleSelectedParticipant(user: User) {
        if (participants.contains(user)) participants.remove(user)
        else participants.add(user)
        selectedParticipants.value = participants
    }

    fun updateGroup(groupId: String) {
        if (isUpdatingGroup.value == true) return
        val subscription =
                Group.fromId(groupId)
                .map { it.addMembers(selectedParticipants.value) }
                .flatMapCompletable { chatManager.updateConversationFromGroup(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isUpdatingGroup.value = true }
                .doAfterTerminate { isUpdatingGroup.value = false }
                .subscribe(
                        { participantsAdded.value = null },
                        { error.value = R.string.add_participants_error }
                )

        subscriptions.add(subscription)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}