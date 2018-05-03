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
import android.graphics.Color
import android.os.Bundle
import android.support.v4.graphics.ColorUtils
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import com.toshi.R
import com.toshi.extensions.getAbsoluteY
import com.toshi.extensions.openWebView
import com.toshi.extensions.startActivity
import com.toshi.extensions.toast
import com.toshi.model.network.dapp.Dapp
import com.toshi.model.network.dapp.DappResult
import com.toshi.util.ImageUtil
import com.toshi.viewModel.ViewDappViewModel
import com.toshi.viewModel.ViewModelFactory.ViewDappViewModelFactory
import kotlinx.android.synthetic.main.activity_view_dapp.*
import kotlinx.android.synthetic.main.view_dapp_header.*
import kotlinx.android.synthetic.main.view_dapp_header.view.*

class ViewDappActivity : AppCompatActivity() {

    companion object {
        const val DAPP_CATEGORIES = "dappCategories"
    }

    private lateinit var viewModel: ViewDappViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_dapp)
        init()
    }

    private fun init() {
        setStatusBarColor()
        initViewModel()
        initNetworkView()
        initListeners()
        initObservers()
    }

    private fun setStatusBarColor() {
        window.statusBarColor = Color.TRANSPARENT
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(
                this,
                ViewDappViewModelFactory(intent)
        ).get(ViewDappViewModel::class.java)
    }

    private fun initNetworkView() {
        networkStatusView.setNetworkVisibility(viewModel.getNetworks())
    }

    private fun initListeners() {
        categories.itemClickedListener = { categoryId, _ -> startDappCategoryActivity(categoryId) }
        openBtn.setOnClickListener { openWebViewActivity() }
        header.closeButton.setOnClickListener { finish() }
        header.offsetChangedListener = { setStatusBarAlpha(it) }
        scrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
            setToolbarTitleAlpha()
            setYOfToolbarTitle()
        }
    }

    private fun startDappCategoryActivity(categoryId: Int) {
        startActivity<ViewAllDappsActivity> {
            putExtra(ViewAllDappsActivity.CATEGORY_ID, categoryId)
            putExtra(ViewAllDappsActivity.VIEW_TYPE, ViewAllDappsActivity.CATEGORY)
        }
    }

    private fun setStatusBarAlpha(percentage: Float) {
        val maxAlpha = 26 // 15%
        val alpha = maxAlpha * (1 - percentage)
        val safeAlpha = if (alpha < 0) 0 else if (alpha > 255) 255 else alpha.toInt()
        val alphaColor = ColorUtils.setAlphaComponent(Color.BLACK, safeAlpha)
        window.statusBarColor = alphaColor
    }

    private fun openWebViewActivity() {
        val address = viewModel.dapp.value?.dapp?.url
        if (address != null) openWebView(address)
        else toast(R.string.unable_to_open_dapp)
    }

    private fun setToolbarTitleAlpha() {
        val nameTop = getAbsoluteY(name) - getStartY()
        val minY = (header.toolbar.height / 2) - (toolbarTitle.height / 2)
        val toolbarY = header.toolbar.height - minY
        val alpha = 1 - nameTop.toDouble() / toolbarY.toDouble()
        val safeAlpha = if (alpha < 0) 0.0 else if (alpha > 1) 1.0 else alpha
        toolbarTitle.alpha = safeAlpha.toFloat()
    }

    private fun setYOfToolbarTitle() {
        val nameTop = getAbsoluteY(name) - getStartY()
        val toolbarTitle = header.toolbar.toolbarTitle
        val minY = (header.toolbar.height / 2) - (toolbarTitle.height / 2)
        if (nameTop < minY) toolbarTitle.y = minY.toFloat()
        else toolbarTitle.y = nameTop.toFloat()
    }

    private fun getStartY() = getAbsoluteY(header.toolbar)

    private fun initObservers() {
        viewModel.dapp.observe(this, Observer {
            if (it == null) return@Observer
            renderDappInfo(it)
            setToolbar(it.dapp)
        })
        viewModel.error.observe(this, Observer {
            if (it != null) toast(it)
        })
    }

    private fun renderDappInfo(dappResult: DappResult) {
        val dapp = dappResult.dapp
        name.text = dapp?.name.orEmpty()
        toolbarTitle.text = dapp?.name.orEmpty()
        description.text = dapp?.description.orEmpty()
        url.text = dapp?.url.orEmpty()
        categories.addCategories(dappResult.categories)
        ImageUtil.loadImageOrPlaceholder(header.headerImage, dapp?.cover)
        ImageUtil.loadImageOrPlaceholder(dappAvatar, dapp?.icon)
    }

    private fun setToolbar(dapp: Dapp?) {
        if (dapp?.cover != null) {
            header.enableCollapsing()
            scrollView.isNestedScrollingEnabled = true
        } else {
            header.disableCollapsing()
            scrollView.isNestedScrollingEnabled = false
        }
    }
}
