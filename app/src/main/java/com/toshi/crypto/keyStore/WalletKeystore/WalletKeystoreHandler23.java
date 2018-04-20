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

import android.annotation.TargetApi;
import android.support.annotation.NonNull;

import com.toshi.crypto.keyStore.Decryptor;
import com.toshi.crypto.keyStore.Encryptor;
import com.toshi.crypto.keyStore.KeyGenerator;
import com.toshi.crypto.keyStore.KeystoreSecretKey;
import com.toshi.exception.KeyStoreException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

@TargetApi(23)
public class WalletKeystoreHandler23 extends KeyStoreBase {

    /*package */ WalletKeystoreHandler23(final String alias) throws KeyStoreException {
        super(alias);
    }

    @Override
    protected void createNewKeysIfNeeded() throws KeyStoreException {
        try {
            if (this.keyStore.containsAlias(this.alias)) return;
            new KeyGenerator(this.alias).generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | java.security.KeyStoreException e) {
            throw new KeyStoreException(new Throwable(e.getMessage()));
        }
    }

    @Override
    public String encrypt(final String textToEncrypt) throws KeyStoreException {
        try {
            return new Encryptor().encrypt(getSecretKey(), textToEncrypt);
        } catch (NoSuchAlgorithmException | BadPaddingException | InvalidKeyException
                | NoSuchPaddingException | IllegalBlockSizeException | UnsupportedEncodingException
                | InvalidAlgorithmParameterException | java.security.KeyStoreException
                | UnrecoverableEntryException e) {
            throw new KeyStoreException(new Throwable(e.getMessage()));
        }
    }

    @Override
    public String decrypt(final String encryptedData, final KeyStoreBase.KeystoreListener listener) throws KeyStoreException {
        final String oldVersion = tryDecryptOldKeystoreVersion(encryptedData);
        if (oldVersion != null) {
            return portOldKeystoreVersion(oldVersion, listener);
        }
        return decryptCurrentKeystoreVersion(encryptedData);
    }

    private String portOldKeystoreVersion(final String oldVersion, final KeystoreListener listener) throws KeyStoreException {
        final String encryptedData = encrypt(oldVersion);
        listener.onUpdate(encryptedData);
        return oldVersion;
    }

    private String tryDecryptOldKeystoreVersion(final String encryptedData) throws KeyStoreException {
        final String oldVersionDecryption = new KeyStoreHandler16().decrypt(encryptedData, null);
        // If the old version wasn't able to decrypt the data then return null.
        if (oldVersionDecryption.equals(encryptedData)) return null;
        return oldVersionDecryption;
    }

    @NonNull
    private String decryptCurrentKeystoreVersion(final String encryptedData) throws KeyStoreException {
        try {
            return new Decryptor().decrypt(getSecretKey(), encryptedData);
        } catch (UnrecoverableEntryException | UnsupportedEncodingException
                | IllegalBlockSizeException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | InvalidKeyException | java.security.KeyStoreException
                | BadPaddingException | NoSuchAlgorithmException e) {
            throw new KeyStoreException(new Throwable(e.getMessage()));
        }
    }

    private SecretKey getSecretKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, java.security.KeyStoreException {
        return new KeystoreSecretKey(this.keyStore, this.alias).get();
    }
}
