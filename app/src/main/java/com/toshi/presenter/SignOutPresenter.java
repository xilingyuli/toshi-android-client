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

package com.toshi.presenter;

import android.content.Intent;

import com.toshi.util.gcm.GcmToken;
import com.toshi.view.BaseApplication;
import com.toshi.view.activity.LandingActivity;
import com.toshi.view.activity.SignOutActivity;

import rx.Completable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SignOutPresenter implements Presenter<SignOutActivity> {

    private SignOutActivity activity;
    private Subscription signOutSubscription;

    @Override
    public void onViewAttached(SignOutActivity view) {
        this.activity = view;
        clearTasks();
    }

    private void clearTasks() {
        this.signOutSubscription =
                clearAndUnregister()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::goToLandingActivity,
                        __ -> goToLandingActivity());
    }

    private Completable clearAndUnregister() {
        return
                unregisterChatGcm()
                .andThen(unregisterEthGcm())
                .doOnError(__ -> clearUserDataAndLogOut())
                .doOnCompleted(this::clearUserDataAndLogOut);
    }

    private Completable unregisterChatGcm() {
        return BaseApplication
                .get()
                .getChatManager()
                .tryUnregisterGcm();
    }

    private Completable unregisterEthGcm() {
        return new GcmToken()
                .get()
                .flatMapCompletable(token -> BaseApplication
                        .get()
                        .getBalanceManager()
                        .unregisterFromEthGcm(token));
    }

    private void clearUserDataAndLogOut() {
        BaseApplication
                .get()
                .getToshiManager()
                .signOut();
    }

    private void goToLandingActivity() {
        final Intent intent = new Intent(this.activity, LandingActivity.class);
        this.activity.startActivity(intent);
        this.activity.finish();
    }

    @Override
    public void onViewDetached() {
        if (this.signOutSubscription != null) {
            this.signOutSubscription.unsubscribe();
        }
        this.activity = null;
    }

    @Override
    public void onDestroyed() {
        this.signOutSubscription = null;
        this.activity = null;
    }
}
