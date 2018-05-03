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
import com.toshi.extensions.toMap
import com.toshi.model.local.network.Networks
import com.toshi.model.network.dapp.Dapp
import com.toshi.model.network.dapp.DappResult
import com.toshi.util.SingleLiveEvent
import com.toshi.view.activity.ViewDappActivity
import rx.subscriptions.CompositeSubscription

class ViewDappViewModel(private val intent: Intent) : ViewModel() {

    private val subscriptions by lazy { CompositeSubscription() }

    val dapp by lazy { MutableLiveData<DappResult>() }
    val error by lazy { SingleLiveEvent<Int>() }

    init {
        getDappFromIntent()
    }

    private fun getDappFromIntent() {
        val dapp = Dapp.getDappFromIntent(intent)
        if (dapp == null) {
            error.value = R.string.unable_to_open_dapp
            return
        }

        val categoriesFromIntent = getCategoriesFromIntent()
        val dappCategories = getDappCategories(dapp, categoriesFromIntent)
        this.dapp.value = DappResult(dapp, dappCategories)
    }

    private fun getDappCategories(dapp: Dapp, categories: Map<Int, String>): Map<Int, String> {
        val dappCategories = mutableMapOf<Int, String>()
        dapp.categories.forEach { dappCategories[it] = categories[it] ?: "" }
        return dappCategories
    }

    private fun getCategoriesFromIntent(): Map<Int, String> {
        return try {
            val categories = intent.getSerializableExtra(ViewDappActivity.DAPP_CATEGORIES) as ArrayList<Pair<Int, String>>
            categories.toMap()
        } catch (e: ClassCastException) {
            emptyMap()
        }
    }

    fun getNetworks() = Networks.getInstance()

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}