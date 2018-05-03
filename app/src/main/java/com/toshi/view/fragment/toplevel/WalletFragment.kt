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

package com.toshi.view.fragment.toplevel

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.toshi.R
import com.toshi.extensions.getColorById
import com.toshi.extensions.toast
import com.toshi.util.ImageUtil
import com.toshi.view.adapter.WalletPagerAdapter
import com.toshi.view.fragment.DialogFragment.ShareWalletAddressDialog
import com.toshi.view.fragment.RefreshFragment
import com.toshi.viewModel.WalletViewModel
import kotlinx.android.synthetic.main.fragment_wallet.*

class WalletFragment : TopLevelFragment() {
    companion object {
        private const val TAG = "WalletFragment"
    }

    override fun getFragmentTag() = TAG

    private lateinit var viewModel: WalletViewModel
    private lateinit var walletAdapter: WalletPagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = init()

    private fun init() {
        val activity = activity ?: return
        setStatusBarColor(activity)
        initViewModel(activity)
        initClickListeners()
        initAdapter()
        initRefreshListener()
        initObservers()
    }

    private fun setStatusBarColor(activity: FragmentActivity) {
        activity.window.statusBarColor = getColorById(R.color.colorPrimaryDark) ?: 0
    }

    private fun initViewModel(activity: FragmentActivity) {
        viewModel = ViewModelProviders.of(activity).get(WalletViewModel::class.java)
    }

    private fun initClickListeners() {
        receive.setOnClickListener { showShareWalletDialog() }
    }

    private fun showShareWalletDialog() {
        val dialog = ShareWalletAddressDialog.newInstance()
        dialog.show(activity?.supportFragmentManager, ShareWalletAddressDialog.TAG)
    }

    private fun initAdapter() {
        val tabs = listOf(
                getString(R.string.tokens),
                getString(R.string.collectibles)
        )
        walletAdapter = WalletPagerAdapter(tabs, childFragmentManager)
        viewPager.adapter = walletAdapter
        tabLayout.setupWithViewPager(viewPager)
    }

    private fun initRefreshListener() {
        refreshLayout.setOnRefreshListener { refreshChildFragment() }
    }

    private fun refreshChildFragment() {
        val childFragments = childFragmentManager.fragments
        childFragments.forEach {
            (it as? RefreshFragment)?.refresh()
        }
    }

    private fun initObservers() {
        viewModel.walletAddress.observe(this, Observer {
            if (it != null) handleWalletAddress(it)
        })
        viewModel.error.observe(this, Observer {
            if (it != null) toast(it)
        })
    }

    private fun handleWalletAddress(paymentAddress: String) {
        walletAddress.setCollapsedText(paymentAddress)
        setWalletAvatar(paymentAddress)
    }

    private fun setWalletAvatar(paymentAddress: String) {
        ImageUtil.loadIdenticon(paymentAddress, avatar)
    }

    fun stopRefreshing() {
        refreshLayout.isRefreshing = false
    }
}