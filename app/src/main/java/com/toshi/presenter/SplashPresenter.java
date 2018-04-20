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

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.toshi.util.logging.LogUtil;
import com.toshi.util.sharedPrefs.AppPrefs;
import com.toshi.view.BaseApplication;
import com.toshi.view.activity.LandingActivity;
import com.toshi.view.activity.MainActivity;
import com.toshi.view.activity.QrCodeHandlerActivity;
import com.toshi.view.activity.SplashActivity;
import com.toshi.view.fragment.DialogFragment.FingerPrintDialogFragment;

import kotlin.Unit;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SplashPresenter implements Presenter<SplashActivity> {

    private SplashActivity activity;
    private CompositeSubscription subscriptions;
    private boolean firstTimeAttaching = true;

    @Override
    public void onViewAttached(SplashActivity view) {
        this.activity = view;

        if (this.firstTimeAttaching) {
            this.firstTimeAttaching = false;
            initLongLivingObjects();
        }

        redirect();
    }

    private void initLongLivingObjects() {
        this.subscriptions = new CompositeSubscription();
    }

    private void redirect() {
        final boolean hasSignedOut = AppPrefs.INSTANCE.hasSignedOut();
        if (hasSignedOut) goToLandingActivity();
        else authenticateUser();
    }

    private void authenticateUser() {
        if (Build.VERSION.SDK_INT >= 23) showFingerprintDialog();
        else initManagersAndGoToAnotherActivity();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showFingerprintDialog() {
        final FingerPrintDialogFragment existingFingerprintDialog = (FingerPrintDialogFragment)
                this.activity.getSupportFragmentManager().findFragmentByTag(FingerPrintDialogFragment.TAG);

        final FingerPrintDialogFragment fingerprintDialog;
        if (existingFingerprintDialog != null) {
            fingerprintDialog = existingFingerprintDialog;
        } else {
            fingerprintDialog = new FingerPrintDialogFragment();
            fingerprintDialog.show(this.activity.getSupportFragmentManager(), FingerPrintDialogFragment.TAG);
        }
        addDialogListeners(fingerprintDialog);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addDialogListeners(final FingerPrintDialogFragment dialog) {
        dialog.setOnSuccessListener(this::initManagersAndGoToAnotherActivity);
        dialog.setOnCancelListener(this::finishAcitvity);
    }

    private Unit initManagersAndGoToAnotherActivity() {
        final Subscription sub =
                BaseApplication
                .get()
                .getToshiManager()
                .tryInit()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::goToAnotherActivity,
                        __ -> goToLandingActivity()
                );

        this.subscriptions.add(sub);
        return null;
    }

    private Unit finishAcitvity() {
        if (this.activity == null) return null;
        this.activity.finish();
        return null;
    }

    private void goToAnotherActivity() {
        AppPrefs.INSTANCE.setSignedIn();

        final PendingIntent nextIntent = this.activity.getIntent().getParcelableExtra(SplashActivity.EXTRA__NEXT_INTENT);
        if (nextIntent != null) {
            try {
                nextIntent.send();
            } catch (final PendingIntent.CanceledException ex) {
                LogUtil.exception(ex);
            }
            this.activity.finish();
        } else {
            if (!tryGoToQrActivity()) {
                goToMainActivity();
            }
        }
    }

    private boolean tryGoToQrActivity() {
        final Uri uri = this.activity.getIntent().getData();
        if (uri != null) {
            goToQrCodeActivity(uri);
            return true;
        }
        return false;
    }

    private void goToQrCodeActivity(final Uri uri) {
        final Intent intent = new Intent(this.activity, QrCodeHandlerActivity.class)
                .setData(uri);
        goToActivity(intent);
    }

    private void goToMainActivity() {
        final Intent intent = new Intent(this.activity, MainActivity.class);
        goToActivity(intent);
    }

    private void goToLandingActivity() {
        final Intent intent = new Intent(this.activity, LandingActivity.class);
        goToActivity(intent);
    }

    private void goToActivity(final Intent intent) {
        this.activity.startActivity(intent);
        this.activity.finish();
    }

    @Override
    public void onViewDetached() {
        this.subscriptions.clear();
        this.activity = null;
    }

    @Override
    public void onDestroyed() {
        this.subscriptions = null;
        this.activity = null;
    }
}
