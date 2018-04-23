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
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.widget.Toast
import com.toshi.R
import com.toshi.extensions.addHorizontalLineDivider
import com.toshi.extensions.startActivity
import com.toshi.extensions.startActivityAndFinish
import com.toshi.extensions.toast
import com.toshi.model.local.Group
import com.toshi.model.local.User
import com.toshi.util.DialogUtil
import com.toshi.util.ImageUtil
import com.toshi.util.logging.LogUtil
import com.toshi.view.adapter.GroupParticipantAdapter
import com.toshi.view.adapter.viewholder.GroupParticipantViewHolder.Companion.MENU_MESSAGE
import com.toshi.view.adapter.viewholder.GroupParticipantViewHolder.Companion.MENU_VIEW_PROFILE
import com.toshi.viewModel.GroupInfoViewModel
import kotlinx.android.synthetic.main.activity_group_info.addParticipants
import kotlinx.android.synthetic.main.activity_group_info.avatar
import kotlinx.android.synthetic.main.activity_group_info.closeButton
import kotlinx.android.synthetic.main.activity_group_info.edit
import kotlinx.android.synthetic.main.activity_group_info.groupName
import kotlinx.android.synthetic.main.activity_group_info.leaveGroup
import kotlinx.android.synthetic.main.activity_group_info.notificationSwitch
import kotlinx.android.synthetic.main.activity_group_info.notificationsWrapper
import kotlinx.android.synthetic.main.activity_group_info.numberOfParticipants
import kotlinx.android.synthetic.main.activity_group_info.participants

class GroupInfoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA__GROUP_ID = "extra_group_id"
    }

    private lateinit var userAdapter: GroupParticipantAdapter
    private lateinit var viewModel: GroupInfoViewModel

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)
        init()
    }

    private fun init() {
        initViewModel()
        initClickListeners()
        processIntentData()
        initObservers()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(GroupInfoViewModel::class.java)
    }

    private fun initClickListeners() {
        closeButton.setOnClickListener { finish() }
        leaveGroup.setOnClickListener { showLeaveGroupConfirmationDialog() }
        edit.setOnClickListener { startGroupEditActivity(viewModel.group.value) }
        notificationsWrapper.setOnClickListener { viewModel.muteConversation(notificationSwitch.isChecked) }
        addParticipants.setOnClickListener { startAddParticipantsActivity(viewModel.group.value) }
    }

    private fun showLeaveGroupConfirmationDialog() {
        DialogUtil.getBaseDialog(
                this,
                R.string.leave_group,
                R.string.leave_group_dialog_message,
                R.string.leave,
                R.string.cancel
        ) { _, _ -> viewModel.leaveGroup() }.show()
    }

    private fun startGroupEditActivity(group: Group?) = startActivityAndFinish<EditGroupActivity> {
        putExtra(EditGroupActivity.EXTRA__GROUP_ID, group?.id)
    }

    private fun startAddParticipantsActivity(group: Group?) = startActivityAndFinish<AddGroupParticipantsActivity> {
        putExtra(AddGroupParticipantsActivity.EXTRA__GROUP_ID, group?.id)
    }

    private fun processIntentData() {
        val groupId = getGroupIdFromIntent()
        groupId?.let { this.viewModel.fetchGroup(groupId) }
    }

    private fun getGroupIdFromIntent(): String? = intent.getStringExtra(EXTRA__GROUP_ID)

    private fun initObservers() {
        viewModel.group.observe(this, Observer {
            it?.let { updateView(it) }
        })
        viewModel.fetchGroupError.observe(this, Observer {
            LogUtil.exception(it)
            toast(R.string.error_unknown_group, Toast.LENGTH_LONG)
            finish()
        })
        viewModel.leaveGroup.observe(this, Observer {
            if (it == true) returnToMainActivity()
        })
        viewModel.leaveGroupError.observe(this, Observer {
            if (it != null) toast(it, Toast.LENGTH_LONG)
        })
        viewModel.isMuted.observe(this, Observer {
            isMuted -> isMuted?.let { notificationSwitch.isChecked = !it }
        })
        viewModel.isMutedError.observe(this, Observer {
            if (it != null) toast(it)
        })
        viewModel.isUpdatingMuteState.observe(this, Observer {
            isUpdatingMuteState -> isUpdatingMuteState?.let { notificationsWrapper.isEnabled = !it }
        })
    }

    private fun updateView(group: Group) {
        groupName.text = group.title
        ImageUtil.load(group.avatar, avatar)
        initRecyclerView(group.members)
        initNumberOfParticipantsView(group.members)
        edit.isEnabled = true
    }

    private fun initRecyclerView(members: List<User>) {
        userAdapter = GroupParticipantAdapter(true).addUsers(members)
        participants.apply {
            layoutManager = LinearLayoutManager(this.context)
            itemAnimator = DefaultItemAnimator()
            adapter = userAdapter
            isNestedScrollingEnabled = false
            addHorizontalLineDivider()
        }
    }

    private fun initNumberOfParticipantsView(members: List<User>) {
        val participantSize = members.size
        val participantsLabel = this.resources.getQuantityString(R.plurals.participants, participantSize, participantSize)
        numberOfParticipants.text = participantsLabel
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val clickedUser = userAdapter.users[item?.order ?: -1]
        when (item?.itemId) {
            MENU_VIEW_PROFILE -> startProfileActivity(clickedUser)
            MENU_MESSAGE -> startChatActivity(clickedUser)
        }
        return super.onContextItemSelected(item)
    }

    private fun startProfileActivity(user: User) = startActivityAndFinish<ViewUserActivity> {
        putExtra(ViewUserActivity.EXTRA__USER_ADDRESS, user.toshiId)
    }

    private fun startChatActivity(user: User) = startActivityAndFinish<ChatActivity> {
        putExtra(ChatActivity.EXTRA__THREAD_ID, user.toshiId)
    }

    private fun returnToMainActivity() = startActivity<MainActivity> {
        putExtra(MainActivity.EXTRA__ACTIVE_TAB, MainActivity.CHATS_TAB)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}