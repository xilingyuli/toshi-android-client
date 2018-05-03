/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 *  This program is free software: you can redistribute it and/or modify
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

package com.toshi.view.adapter.viewholder

import android.support.v7.widget.RecyclerView
import android.view.View
import com.toshi.util.ImageUtil
import com.toshi.viewModel.Wallet
import kotlinx.android.synthetic.main.list_item__wallet.view.avatar
import kotlinx.android.synthetic.main.list_item__wallet.view.name
import kotlinx.android.synthetic.main.list_item__wallet.view.paymentAddress

class WalletViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
    fun setWallet(wallet: Wallet) {
        itemView.name.text = (wallet.name)
        itemView.paymentAddress.setCollapsedText(wallet.paymentAddress)
        ImageUtil.loadIdenticon(wallet.paymentAddress, itemView.avatar)
    }

    fun setOnItemClickedListener(wallet: Wallet, listener: (Wallet) -> Unit) {
        itemView.setOnClickListener { listener(wallet) }
    }
}