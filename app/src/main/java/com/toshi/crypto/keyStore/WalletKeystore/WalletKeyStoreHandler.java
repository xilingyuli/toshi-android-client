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
package com.toshi.crypto.keyStore.WalletKeystore;

import android.os.Build;

import com.toshi.exception.KeyStoreException;

public class WalletKeyStoreHandler {

    private KeyStoreBase secretHandler;

    public WalletKeyStoreHandler(final String alias) throws KeyStoreException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.secretHandler = new WalletKeystoreHandler23(alias);
        } else {
            this.secretHandler = new KeyStoreHandler16();
        }
    }

    public String encrypt(final String data) throws KeyStoreException {
        return this.secretHandler.encrypt(data);
    }

    public String decrypt(final String data, final KeyStoreBase.KeystoreListener listener) throws KeyStoreException {
        return this.secretHandler.decrypt(data, listener);
    }

    public void  delete(final String alias) throws KeyStoreException {
        this.secretHandler.deleteKey();
    }
}
