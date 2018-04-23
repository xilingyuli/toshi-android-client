package com.toshi.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.toshi.R;
import com.toshi.model.local.Recipient;
import com.toshi.model.sofa.PaymentRequest;
import com.toshi.model.sofa.SofaMessage;
import com.toshi.util.logging.LogUtil;
import com.toshi.view.BaseApplication;
import com.toshi.view.notification.ChatNotificationManager;

import java.util.concurrent.TimeUnit;

public class RejectPaymentRequestService extends IntentService {

    public static final int REJECT_REQUEST_CODE = 100;

    public static final String MESSAGE_ID = "messageId";

    public RejectPaymentRequestService() {
        super("NotificationPaymentService");
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (intent == null) return;
        try {
            tryInit();
            final String messageId = intent.getStringExtra(MESSAGE_ID);
            rejectPaymentRequest(messageId);
        } catch (Exception e) {
            LogUtil.w("PaymentRequestService " + e);
        }
    }

    private void tryInit() throws Exception {
        BaseApplication
                .get()
                .getToshiManager()
                .tryInit()
                .timeout(5000, TimeUnit.MILLISECONDS)
                .await();
    }

    private void rejectPaymentRequest(final String messageId) {
        try {
            final SofaMessage sofaMessage = getSofaMessageFromId(messageId);
            if (sofaMessage == null) return;

            final Recipient recipient = new Recipient(sofaMessage.getSender());
            final String content = BaseApplication.get().getString(R.string.payment_request_rejected);
            updatePaymentRequestState(messageId, recipient, PaymentRequest.REJECTED);
            ChatNotificationManager.showChatNotification(recipient, content);
        } catch (Exception e) {
            LogUtil.w("Error while rejecting payment request " + e);
        }
    }

    private void updatePaymentRequestState(final String messageId,
                                           final Recipient recipient,
                                           final @PaymentRequest.State int paymentState) {
        // Todo - Handle groups
        final SofaMessage sofaMessage = getSofaMessageFromId(messageId);
        BaseApplication
                .get()
                .getTransactionManager()
                .updatePaymentRequestState(recipient.getUser(), sofaMessage, paymentState);
    }

    private SofaMessage getSofaMessageFromId(final String messageId) {
        return BaseApplication
                .get()
                .getChatManager()
                .getSofaMessageById(messageId)
                .timeout(5000, TimeUnit.MILLISECONDS)
                .toBlocking()
                .value();
    }
}
