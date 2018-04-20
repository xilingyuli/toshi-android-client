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

import com.toshi.exception.KeyStoreException;

public class KeyStoreHandler16 extends KeyStoreBase {

    public static final String VERSION_IDENTIFIER = "$$KeystoreVersion_16$$";

    /*package */ KeyStoreHandler16() throws KeyStoreException {
        super();
    }

    @Override
    void createNewKeysIfNeeded() throws KeyStoreException {}

    @Override
    public String encrypt(final String stringToEncrypt) throws KeyStoreException {
        return String.format("%s%s", VERSION_IDENTIFIER, stringToEncrypt);
    }

    @Override
    public String decrypt(final String encryptedData, final KeyStoreBase.KeystoreListener listener) throws KeyStoreException {
        return encryptedData.replace(VERSION_IDENTIFIER, "");
    }
}
