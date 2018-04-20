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

package com.toshi.view.fragment.DialogFragment

import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.toshi.R
import com.toshi.util.fringerPrint.FingerPrint
import kotlinx.android.synthetic.main.fragment_fingerprint.cancelButton

@RequiresApi(api = Build.VERSION_CODES.M)
class FingerPrintDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "FingerPrintDialogFragment"
    }

    var onSuccessListener: (() -> Unit)? = null
    var onCancelListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateDialog(state: Bundle?): Dialog {
        val dialog = super.onCreateDialog(state)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_fingerprint, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = init()

    private fun init() {
        initClickListeners()
        authenticateUserWithFingerPrint()
    }

    private fun initClickListeners() {
        cancelButton.setOnClickListener { handleOnCanceled() }
    }

    private fun authenticateUserWithFingerPrint() {
        val fingerprintManager = activity?.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        val keyguardManager = activity?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        val fingerPrint = FingerPrint(
                onSuccessListener = { handleOnSuccess() },
                onErrorListener = {},
                onFailedListener = {}
        )

        if (fingerPrint.isSensorAvailable(fingerprintManager, keyguardManager)) {
            fingerPrint.authenticateUser(fingerprintManager)
        } else {
            // Show an error message
        }
    }

    private fun handleOnSuccess() {
        onSuccessListener?.invoke()
        dismiss()
    }

    private fun handleOnCanceled() {
        onCancelListener?.invoke()
        dismiss()
    }
}