package com.bakkenbaeck.token.view.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.bakkenbaeck.token.R;
import com.bakkenbaeck.token.databinding.ActivityChatBinding;
import com.bakkenbaeck.token.model.local.ActivityResultHolder;
import com.bakkenbaeck.token.model.local.User;
import com.bakkenbaeck.token.presenter.ChatPresenter;
import com.bakkenbaeck.token.presenter.factory.ChatPresenterFactory;
import com.bakkenbaeck.token.presenter.factory.PresenterFactory;

public final class ChatActivity extends BasePresenterActivity<ChatPresenter, ChatActivity> {
    public static final String EXTRA__REMOTE_USER = "remote_user";
    private ActivityChatBinding binding;
    private User remoteUser;
    private ActivityResultHolder resultHolder;
    private ChatPresenter presenter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);
        this.remoteUser = getIntent().getParcelableExtra(EXTRA__REMOTE_USER);
    }

    public final ActivityChatBinding getBinding() {
        return this.binding;
    }

    @NonNull
    @Override
    protected PresenterFactory<ChatPresenter> getPresenterFactory() {
        return new ChatPresenterFactory();
    }

    @Override
    protected void onPresenterPrepared(@NonNull final ChatPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setRemoteUser(this.remoteUser);
        tryProcessResultHolder();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        this.resultHolder = new ActivityResultHolder(requestCode, resultCode, data);
        tryProcessResultHolder();
    }

    private void tryProcessResultHolder() {
        if (this.presenter == null || this.resultHolder == null) {
            return;
        }

        this.presenter.handleActivityResult(this.resultHolder);
        this.resultHolder = null;
    }

    @Override
    protected int loaderId() {
        return 4002;
    }
}
