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

package com.toshi.manager.transaction

import android.util.Pair
import com.toshi.exception.UnknownTransactionException
import com.toshi.manager.store.PendingTransactionStore
import com.toshi.model.local.PendingTransaction
import com.toshi.model.local.Recipient
import com.toshi.model.local.User
import com.toshi.model.sofa.PaymentRequest
import com.toshi.model.sofa.SofaAdapters
import com.toshi.model.sofa.SofaMessage
import com.toshi.model.sofa.SofaType
import com.toshi.model.sofa.payment.Payment
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.io.IOException

class UpdateTransactionManager(private val pendingTransactionStore: PendingTransactionStore) {

    private val sofaMessageManager by lazy { BaseApplication.get().chatManager }
    private val balanceManager by lazy { BaseApplication.get().balanceManager }
    private val updatePaymentQueue by lazy { PublishSubject.create<Payment>() }
    private val subscriptions by lazy { CompositeSubscription() }
    private var updatePaymentSub: Subscription? = null

    fun attachUpdatePaymentSubscriber() {
        // Explicitly clear first to avoid double subscription
        clearSubscription()
        updatePaymentSub = updatePaymentQueue
                .filter { it != null }
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { processUpdatedPayment(it) },
                        { LogUtil.exception("Error when updating payment $it") }
                )

        subscriptions.add(updatePaymentSub)
    }

    private fun processUpdatedPayment(payment: Payment) {
        val sub = pendingTransactionStore
                .loadTransaction(payment.txHash)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { updatePendingTransaction(it, payment) },
                        { LogUtil.exception("Error while handling pending transactions $it") }
                )

        subscriptions.add(sub)
    }

    fun updatePendingTransactions() {
        val sub = pendingTransactionStore
                .loadAllTransactions()
                .toObservable()
                .flatMapIterable { it }
                .filter { isUnconfirmed(it) }
                .flatMap {
                    Observable.zip(
                            Observable.just(it),
                            getTransactionStatus(it),
                            { first, second -> Pair(first, second) }
                    )
                }
                .onErrorReturn { Pair(null, null) }
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { updatePendingTransaction(it.first, it.second) },
                        { LogUtil.exception("Error during updating pending transaction $it") }
                )

        subscriptions.add(sub)
    }

    private fun getTransactionStatus(pendingTransaction: PendingTransaction): Observable<Payment> {
        return balanceManager
                .getTransactionStatus(pendingTransaction.txHash)
                .toObservable()
    }

    // Returns false if this is a new transaction that the app is unaware of.
    // Returns true if the transaction was correctly updated.
    private fun updatePendingTransaction(pendingTransaction: PendingTransaction?, updatedPayment: Payment?): Boolean {
        if (pendingTransaction == null || updatedPayment == null) return false
        return try {
            val updatedMessage = updateStatusFromPendingTransaction(pendingTransaction, updatedPayment)
            val updatedPendingTransaction = PendingTransaction()
                    .setTxHash(pendingTransaction.txHash)
                    .setSofaMessage(updatedMessage)
            pendingTransactionStore.save(updatedPendingTransaction)
            true
        } catch (ex: IOException) {
            LogUtil.exception("Unable to update pending transaction $ex")
            false
        }
    }

    @Throws(IOException::class, UnknownTransactionException::class)
    private fun updateStatusFromPendingTransaction(pendingTransaction: PendingTransaction, updatedPayment: Payment): SofaMessage {
        val sofaMessage = pendingTransaction.sofaMessage
        val existingPayment = SofaAdapters.get().paymentFrom(sofaMessage.payload)
        existingPayment.status = updatedPayment.status
        val messageBody = SofaAdapters.get().toJson(existingPayment)
        return sofaMessage.setPayload(messageBody)
    }

    private fun isUnconfirmed(pendingTransaction: PendingTransaction): Boolean {
        return try {
            val sofaMessage = pendingTransaction.sofaMessage
            val payment = SofaAdapters.get().paymentFrom(sofaMessage.payload)
            payment.status == SofaType.UNCONFIRMED
        } catch (ex: IOException) {
            false
        }
    }

    fun updatePaymentRequestState(remoteUser: User, sofaMessage: SofaMessage, @PaymentRequest.State newState: Int) {
        try {
            val paymentRequest = SofaAdapters.get()
                    .txRequestFrom(sofaMessage.payload)
                    .setState(newState)
            val recipient = Recipient(remoteUser)
            val updatedPayload = SofaAdapters.get().toJson(paymentRequest)
            sofaMessage.payload = updatedPayload
            sofaMessageManager.updateMessage(recipient, sofaMessage)
        } catch (ex: IOException) {
            LogUtil.exception("Error changing Payment Request state $ex")
        }
    }

    fun updatePayment(payment: Payment) = updatePaymentQueue.onNext(payment)
    fun clearSubscription() = updatePaymentSub?.unsubscribe()
}