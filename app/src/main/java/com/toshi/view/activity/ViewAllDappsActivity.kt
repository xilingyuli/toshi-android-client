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

package com.toshi.view.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.toshi.R
import com.toshi.extensions.*
import com.toshi.model.network.dapp.Dapp
import com.toshi.view.adapter.AllDappsAdapter
import com.toshi.viewModel.LoadingState
import com.toshi.viewModel.PagingState
import com.toshi.viewModel.ViewAllDappsViewModel
import com.toshi.viewModel.ViewModelFactory.ViewAllDappsViewModelFactory
import kotlinx.android.synthetic.main.activity_settings_advanced.*
import kotlinx.android.synthetic.main.activity_view_dapps.closeButton
import kotlinx.android.synthetic.main.activity_view_dapps.dapps
import kotlinx.android.synthetic.main.activity_view_dapps.toolbarTitle

class ViewAllDappsActivity : AppCompatActivity() {

    companion object {
        const val CATEGORY = 1
        const val ALL = 2

        const val VIEW_TYPE = "viewType"
        const val CATEGORY_ID = "categoryId"

        private const val PREFETCH_NUMBER = 5
    }

    private lateinit var viewModel: ViewAllDappsViewModel
    private lateinit var allDappsAdapter: AllDappsAdapter
    private lateinit var dappsLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_dapps)
        init()
    }

    private fun init() {
        initViewModel()
        initNetworkView()
        initClickListeners()
        initAdapter()
        initScrollListener()
        initObservers()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(
                this,
                ViewAllDappsViewModelFactory(intent)
        ).get(ViewAllDappsViewModel::class.java)
    }

    private fun initNetworkView() {
        networkStatusView.setNetworkVisibility(viewModel.getNetworks())
    }

    private fun initClickListeners() {
        closeButton.setOnClickListener { finish() }
    }

    private fun initAdapter() {
        allDappsAdapter = AllDappsAdapter().apply {
            onItemClickedListener = { startViewDappActivity(it) }
        }
        dappsLayoutManager = LinearLayoutManager(this)
        dapps.apply {
            adapter = allDappsAdapter
            layoutManager = dappsLayoutManager
            addHorizontalLineDivider(leftPadding = getPxSize(R.dimen.avatar_size_medium)
                    + getPxSize(R.dimen.margin_primary)
                    + getPxSize(R.dimen.margin_primary))
        }
    }

    private fun startViewDappActivity(dapp: Dapp) {
        val categories = viewModel.dappCategories
        startActivity<ViewDappActivity> {
            putExtra(ViewDappActivity.DAPP_CATEGORIES, categories.toArrayList())
            Dapp.buildIntent(this, dapp)
        }
    }

    private fun initScrollListener() {
        dapps.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                handleScroll()
            }
        })
    }

    private fun handleScroll() {
        val isLoading = viewModel.loadingState == LoadingState.LOADING
        val hasReachedEnd = viewModel.pagingState == PagingState.REACHED_END
        if (isLoading || hasReachedEnd) return
        val shouldFetch = (getVisibleItemCount() + PREFETCH_NUMBER + getFirstVisibleItemPosition()) >= getTotalItemCount()
                && getFirstVisibleItemPosition() > 0
        if (shouldFetch) viewModel.gethMoreDapps()
    }

    private fun getVisibleItemCount() = dappsLayoutManager.childCount
    private fun getTotalItemCount() = dappsLayoutManager.itemCount
    private fun getFirstVisibleItemPosition() = dappsLayoutManager.findFirstVisibleItemPosition()

    private fun initObservers() {
        viewModel.dapps.observe(this, Observer {
            if (it != null) allDappsAdapter.setDapps(it)
        })
        viewModel.dappsError.observe(this, Observer {
            if (it != null) toast(it)
        })
        viewModel.categoryName.observe(this, Observer {
            if (it != null) toolbarTitle.text = it
        })
    }
}