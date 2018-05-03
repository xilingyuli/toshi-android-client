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
import android.content.Intent
import com.toshi.R
import com.toshi.model.local.network.Networks
import com.toshi.model.network.dapp.Dapp
import com.toshi.model.network.dapp.DappSearchResult
import com.toshi.model.network.dapp.Dapps
import com.toshi.util.SingleLiveEvent
import com.toshi.view.BaseApplication
import com.toshi.view.activity.ViewAllDappsActivity.Companion.ALL
import com.toshi.view.activity.ViewAllDappsActivity.Companion.CATEGORY
import com.toshi.view.activity.ViewAllDappsActivity.Companion.CATEGORY_ID
import com.toshi.view.activity.ViewAllDappsActivity.Companion.VIEW_TYPE
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class ViewAllDappsViewModel(private val intent: Intent) : ViewModel() {

    private val dappsManager by lazy { BaseApplication.get().dappManager }
    private val subscriptions by lazy { CompositeSubscription() }

    val dapps by lazy { MutableLiveData<MutableList<Dapp>>() }
    val categoryName by lazy { MutableLiveData<String>() }
    val dappsError by lazy { SingleLiveEvent<Int>() }
    val dappCategories by lazy { mutableMapOf<Int, String>() }

    var pagingState = PagingState.AVAILABLE_ITEMS
    var loadingState = LoadingState.NOT_LOADING

    init {
        if (getViewType() == CATEGORY) getInitialDappsInCategory(getCategoryId())
        else getInitialDapps()
    }

    private fun getViewType() = intent.getIntExtra(VIEW_TYPE, ALL)
    private fun getCategoryId() = intent.getIntExtra(CATEGORY_ID, -1)

    private fun getInitialDapps() {
        val sub = getDappsWithOffset(0)
                .doOnSuccess { updateInitialPagingState(it) }
                .map { it.results }
                .doOnSuccess { setCategoryName(it, getCategoryId()) }
                .map { it.dapps.toMutableList() }
                .subscribe(
                        { dapps.value = it },
                        { dappsError.value = R.string.error_fetching_dapps }
                )

        subscriptions.add(sub)
    }

    private fun getInitialDappsInCategory(categoryId: Int) {
        val sub = getDappsInCategoryWithOffset(categoryId, 0)
                .doOnSuccess { updateInitialPagingState(it) }
                .map { it.results }
                .doOnSuccess { setCategoryName(it, getCategoryId()) }
                .map { it.dapps.toMutableList() }
                .subscribe(
                        { dapps.value = it },
                        { dappsError.value = R.string.error_fetching_dapps }
                )

        subscriptions.add(sub)
    }

    fun gethMoreDapps() {
        if (getViewType() == CATEGORY) getDappsInCategory(getCategoryId())
        else getDapps()
    }

    private fun getDapps() {
        val sub = getDappsWithOffset(getOffset())
                .doOnSuccess { updatePagingState(it) }
                .map { it.results.dapps }
                .subscribe(
                        { addNewDappsToList(it) },
                        { dappsError.value = R.string.error_fetching_dapps }
                )

        subscriptions.add(sub)
    }

    private fun getDappsWithOffset(offset: Int): Single<DappSearchResult> {
        return dappsManager
                .getAllDappsWithOffset(offset)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { loadingState = LoadingState.LOADING }
                .doAfterTerminate { loadingState = LoadingState.NOT_LOADING }
                .doOnSuccess { updateCategories(it) }
    }

    private fun getDappsInCategory(categoryId: Int) {
        val sub = getDappsInCategoryWithOffset(categoryId, getOffset())
                .doOnSuccess { updatePagingState(it) }
                .map { it.results.dapps }
                .subscribe(
                        { addNewDappsToList(it) },
                        { dappsError.value = R.string.error_fetching_dapps }
                )

        subscriptions.add(sub)
    }

    private fun getDappsInCategoryWithOffset(categoryId: Int, offset: Int): Single<DappSearchResult> {
        return dappsManager
                .getAllDappsInCategoryWithOffset(categoryId, offset)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { loadingState = LoadingState.LOADING }
                .doAfterTerminate { loadingState = LoadingState.NOT_LOADING }
                .doOnSuccess { updateCategories(it) }
    }

    private fun addNewDappsToList(newDapps: List<Dapp>) {
        dapps.value?.addAll(newDapps)
        dapps.value = dapps.value
    }

    private fun setCategoryName(dapps: Dapps, categoryId: Int) {
        val categoryName = when {
            getViewType() == ALL -> BaseApplication.get().getString(R.string.all_dapps)
            categoryId == -1 -> BaseApplication.get().getString(R.string.other)
            else -> dapps.categories[categoryId] ?: BaseApplication.get().getString(R.string.other)
        }
        this.categoryName.value = categoryName
    }

    private fun getOffset() = dapps.value?.size ?: 0

    private fun updateInitialPagingState(searchResult: DappSearchResult) {
        pagingState = if (searchResult.limit >= searchResult.total) PagingState.REACHED_END
        else PagingState.AVAILABLE_ITEMS
    }

    private fun updatePagingState(searchResult: DappSearchResult) {
        pagingState = if (searchResult.offset >= searchResult.total) PagingState.REACHED_END
        else PagingState.AVAILABLE_ITEMS
    }

    private fun updateCategories(searchResult: DappSearchResult) {
        dappCategories.putAll(searchResult.results.categories)
    }

    fun getNetworks() = Networks.getInstance()

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}

enum class LoadingState {
    LOADING,
    NOT_LOADING
}

enum class PagingState {
    AVAILABLE_ITEMS,
    REACHED_END
}