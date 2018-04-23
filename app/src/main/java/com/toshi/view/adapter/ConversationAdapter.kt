/*
 * 	Copyright (c) 2018. Toshi Inc
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

package com.toshi.view.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.toshi.R
import com.toshi.view.adapter.viewholder.ThreadViewHolder
import com.toshi.model.local.Conversation
import com.toshi.util.keyboard.SOFAMessageFormatter
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication

class ConversationAdapter(
        private val onItemClickListener: (Conversation) -> Unit,
        private val onItemLongClickListener: (Conversation) -> Unit
    ) : BaseCompoundableAdapter<ThreadViewHolder, Conversation>() {

    private val messageFormatter: SOFAMessageFormatter by lazy { SOFAMessageFormatter() }

    override fun compoundableBindViewHolder(viewHolder: RecyclerView.ViewHolder, adapterIndex: Int) {
        val typedHolder = viewHolder as? ThreadViewHolder
                ?: throw AssertionError("This is not the right type!")
        onBindViewHolder(typedHolder, adapterIndex)
    }

    override fun setItemList(items: List<Conversation>) {
        // Don't show conversations with invalid recipients.
        super.setItemList(items.filter { !it.isRecipientInvalid })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item__recent, parent, false)
        return ThreadViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        val conversation = safelyAt(position)
                ?: throw AssertionError("No conversation at $position")
        holder.setThread(conversation)

        val formattedLatestMessage = messageFormatter.formatMessage(conversation.latestMessage)

        holder.setLatestMessage(formattedLatestMessage)
        holder.setOnItemClickListener(conversation, onItemClickListener)
        holder.setOnItemLongClickListener(conversation, onItemLongClickListener)
    }

    override fun deleteItem(item: Conversation) {
        BaseApplication
                .get()
                .chatManager
                .deleteConversation(item)
                .subscribe(
                        { }
                ) { _ -> LogUtil.w("Unable to delete conversation") }
    }
}