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

package com.toshi.view;


import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.IntentFilter;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.toshi.BuildConfig;
import com.toshi.manager.BalanceManager;
import com.toshi.manager.DappManager;
import com.toshi.manager.RecipientManager;
import com.toshi.manager.ReputationManager;
import com.toshi.manager.ChatManager;
import com.toshi.manager.ToshiManager;
import com.toshi.manager.TransactionManager;
import com.toshi.manager.UserManager;
import com.toshi.service.NetworkChangeReceiver;
import com.toshi.util.logging.CrashlyticsTree;
import com.toshi.util.logging.LogUtil;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public final class BaseApplication extends MultiDexApplication implements LifecycleObserver {

    private static BaseApplication instance;
    public static BaseApplication get() { return instance; }
    private final BehaviorSubject<Boolean> isConnectedSubject = BehaviorSubject.create();

    private ToshiManager toshiManager;
    private boolean inBackground = false;

    public final Realm getRealm() {
        if (Thread.currentThread().getId() == 1) {
            LogUtil.w("DB call done on Main Thread. Move this to a background thread.");
        }
        return this.toshiManager.getRealm().toBlocking().value();
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        instance = this;
        init();
    }

    private void init() {
        initCrashlyticsAndTimber();
        initLifecycleObserver();
        initToshiManager();
        initConnectivityMonitor();
    }

    private void initCrashlyticsAndTimber() {
        Fabric.with(this, new Crashlytics());
        if (BuildConfig.DEBUG) Timber.plant(new Timber.DebugTree());
        else Timber.plant(new CrashlyticsTree());
    }

    private void initLifecycleObserver() {
        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(this);
    }

    private void initToshiManager() {
        this.toshiManager = new ToshiManager();
    }

    private void initConnectivityMonitor() {
        final IntentFilter connectivityIntent = new IntentFilter();
        connectivityIntent.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(new NetworkChangeReceiver(), connectivityIntent);
        isConnectedSubject().onNext(NetworkChangeReceiver.getCurrentConnectivityStatus(this));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResumed() {
        if (!this.inBackground) return;
        this.inBackground = false;
        this.toshiManager.getChatManager().resumeMessageReceiving();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        this.inBackground = true;
        this.toshiManager.getChatManager().disconnect();
    }

    public BehaviorSubject<Boolean> isConnectedSubject() {
        return isConnectedSubject;
    }

    public boolean isConnected() {
        return isConnectedSubject.getValue();
    }

    public boolean isInBackground() { return this.inBackground; }


    public final ToshiManager getToshiManager() {
        return this.toshiManager;
    }

    // Helper functions
    // Unwrap the ToshiManager container to reduce lines of code
    public final ChatManager getChatManager() {
        return this.toshiManager.getChatManager();
    }

    public final TransactionManager getTransactionManager() {
        return this.toshiManager.getTransactionManager();
    }

    public final UserManager getUserManager() {
        return this.toshiManager.getUserManager();
    }

    public final RecipientManager getRecipientManager() {
        return this.toshiManager.getRecipientManager();
    }

    public final BalanceManager getBalanceManager() {
        return this.toshiManager.getBalanceManager();
    }

    public final ReputationManager getReputationManager() {
        return this.toshiManager.getReputationManager();
    }

    public final DappManager getDappManager() {
        return this.toshiManager.getDappManager();
    }
}