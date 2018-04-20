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

package com.toshi.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.toshi.crypto.hdshim.EthereumKeyChainGroup;
import com.toshi.crypto.keyStore.WalletKeystore.WalletKeyStoreHandler;
import com.toshi.crypto.util.TypeConverter;
import com.toshi.exception.InvalidMasterSeedException;
import com.toshi.exception.KeyStoreException;
import com.toshi.util.FileNames;
import com.toshi.util.logging.LogUtil;
import com.toshi.view.BaseApplication;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;

import java.io.IOException;

import rx.Single;

import static com.toshi.crypto.util.HashUtil.sha3;

public class HDWallet {

    private static final String ALIAS = "MasterSeedAlias";
    private static final String MASTER_SEED = "ms";

    private SharedPreferences prefs;
    private ECKey identityKey;
    private ECKey paymentKey;
    private String masterSeed;

    public HDWallet() {
        this.prefs = BaseApplication.get().getSharedPreferences(FileNames.WALLET_PREFS, Context.MODE_PRIVATE);
    }

    public HDWallet(@NonNull final SharedPreferences preferences) {
        this.prefs = preferences;
    }

    public Single<HDWallet> getExistingWallet() {
        return Single.fromCallable(() -> {
            this.masterSeed = readMasterSeedFromStorage();
            if (this.masterSeed == null) throw new InvalidMasterSeedException(new Throwable("Master seed is null"));
            final Wallet wallet = initFromMasterSeed(this.masterSeed);
            deriveKeysFromWallet(wallet);
            return this;
        })
        .doOnError(throwable -> LogUtil.exception("Error while getting existing wallet", throwable));
    }

    // WARNING: This will delete any wallet stored on disk
    public Single<HDWallet> createWallet() {
        return Single.fromCallable(() -> {
            final Wallet wallet = generateNewWallet();
            deriveKeysFromWallet(wallet);
            return this;
        })
        .doOnError(throwable -> LogUtil.exception("Error while creating new wallet", throwable));
    }

    private Wallet generateNewWallet() {
        final Wallet walletForSeed = new Wallet(getNetworkParameters());
        final DeterministicSeed seed = walletForSeed.getKeyChainSeed();
        final Wallet wallet = new Wallet(getNetworkParameters(),  new EthereumKeyChainGroup(getNetworkParameters(), seed));
        final String masterSeed = seedToString(seed);
        saveMasterSeedToStorage(masterSeed);
        return wallet;
    }

    private Wallet initFromMasterSeed(final String masterSeed) {
        try {
            final DeterministicSeed seed = getSeed(masterSeed);
            seed.check();
            return constructFromSeed(seed);
        } catch (final UnreadableWalletException | MnemonicException e) {
            LogUtil.exception("Error while initiating from from master seed", e);
            throw new RuntimeException("Unable to create wallet. Seed is invalid");
        }
    }

    public Single<HDWallet> createFromMasterSeed(final String masterSeed) {
        return Single.fromCallable(() -> {
            try {
                final DeterministicSeed seed = getSeed(masterSeed);
                seed.check();
                final Wallet wallet = constructFromSeed(seed);
                deriveKeysFromWallet(wallet);
                saveMasterSeedToStorage(masterSeed);
                return this;
            } catch (final UnreadableWalletException | MnemonicException e) {
                LogUtil.exception("Error while creating wallet from master seed", e);
                throw new InvalidMasterSeedException(e);
            }
        });
    }

    @NonNull
    private Wallet constructFromSeed(final DeterministicSeed seed) {
        return new Wallet(getNetworkParameters(),  new EthereumKeyChainGroup(getNetworkParameters(), seed));
    }

    private DeterministicSeed getSeed(final String masterSeed) throws UnreadableWalletException {
        return new DeterministicSeed(masterSeed, null, "", 0);
    }

    private NetworkParameters getNetworkParameters() {
        return NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
    }

    private void deriveKeysFromWallet(final Wallet wallet) {
        try {
            deriveIdentityKey(wallet);
            derivePaymentKey(wallet);
        } catch (final UnreadableWalletException | IOException ex) {
            LogUtil.exception("Error while deriving keys from wallet", ex);
            throw new RuntimeException("Error deriving keys: " + ex);
        }
    }

    private void deriveIdentityKey(final Wallet wallet) throws IOException, UnreadableWalletException {
        this.identityKey = deriveKeyFromWallet(wallet, 0, KeyChain.KeyPurpose.AUTHENTICATION);
    }

    private void derivePaymentKey(final Wallet wallet) throws IOException, UnreadableWalletException {
        this.paymentKey = deriveKeyFromWallet(wallet, 0, KeyChain.KeyPurpose.RECEIVE_FUNDS);
    }

    private ECKey deriveKeyFromWallet(final Wallet wallet, final int iteration, final KeyChain.KeyPurpose keyPurpose) throws UnreadableWalletException, IOException {
        DeterministicKey key = null;
        for (int i = 0; i <= iteration; i++) {
            key = wallet.freshKey(keyPurpose);
        }

        if (key == null) {
            throw new IOException("Unable to derive key");
        }

        return ECKey.fromPrivate(key.getPrivKey());
    }

    public String signIdentity(final String data) {
        return sign(data.getBytes(), this.identityKey);
    }

    public String signTransaction(final String data) {
        try {
            final byte[] transactionBytes = TypeConverter.StringHexToByteArray(data);
            return sign(transactionBytes, this.paymentKey);
        } catch (final Exception e) {
            LogUtil.exception("Unable to sign transaction. " + e);
            return null;
        }
    }

    public String signTransactionWithoutMinus27(final String data) {
        try {
            final byte[] transactionBytes = TypeConverter.StringHexToByteArray(data);
            return signWithoutMinus27(transactionBytes, this.paymentKey);
        } catch (final Exception e) {
            LogUtil.exception("Unable to sign transaction. " + e);
            return null;
        }
    }

    public String hashAndSignTransactionWithoutMinus27(final String data) {
        try {
            final byte[] transactionBytes = TypeConverter.StringHexToByteArray(data);
            return hashAndSignWithoutMinus27(transactionBytes, this.paymentKey);
        } catch (final Exception e) {
            LogUtil.exception("Unable to sign transaction. " + e);
            return null;
        }
    }

    private String sign(final byte[] bytes, final ECKey key) {
        return sign(bytes, key, true);
    }

    private String sign(final byte[] bytes, final ECKey key, final Boolean doSha3Hash) {
        final byte[] msgHash = doSha3Hash ? sha3(bytes) : bytes;
        final ECKey.ECDSASignature signature = key.sign(msgHash);
        return signature.toHex();
    }

    private String signWithoutMinus27(final byte[] bytes, final ECKey key) {
        final ECKey.ECDSASignature signature = key.sign(bytes);
        return signature.toHexWithNoMinus27();
    }

    private String hashAndSignWithoutMinus27(final byte[] bytes, final ECKey key) {
        final byte[] msgHash = sha3(bytes);
        final ECKey.ECDSASignature signature = key.sign(msgHash);
        return signature.toHexWithNoMinus27();
    }

    public String getMasterSeed() {
        return this.masterSeed;
    }

    public byte[] generateDatabaseEncryptionKey() {
        final byte[] encryptionKey = new byte[64];
        final byte[] privateKey = getPrivateKeyBytes();
        System.arraycopy(privateKey, 0, encryptionKey, 0, 32);
        System.arraycopy(privateKey, 0, encryptionKey, 32, 32);
        return encryptionKey;
    }

    private byte[] getPrivateKeyBytes() {
        if (this.identityKey != null) {
            return this.identityKey.getPrivKeyBytes();
        }
        return null;
    }

    public String getOwnerAddress() {
        if (this.identityKey == null) return null;
        return TypeConverter.toJsonHex(this.identityKey.getAddress());
    }

    public String getPaymentAddress() {
        if(this.paymentKey != null) {
            return TypeConverter.toJsonHex(this.paymentKey.getAddress());
        }
        return null;
    }

    @Override
    public String toString() {
        return "Identity: " + getOwnerAddress() + "\nPayment: " + getPaymentAddress();
    }

    private void saveMasterSeedToStorage(final String masterSeed) {
        try {
            final WalletKeyStoreHandler walletKeyStoreHandler = new WalletKeyStoreHandler(ALIAS);
            final String encryptedMasterSeed = walletKeyStoreHandler.encrypt(masterSeed);
            saveMasterSeed(encryptedMasterSeed);
            this.masterSeed = masterSeed;
        } catch (KeyStoreException e) {
            LogUtil.exception("Error while saving master seed from storage", e);
            throw new IllegalStateException(e);
        }
    }

    private void saveMasterSeed(final String masterSeed) {
        this.prefs.edit()
                .putString(MASTER_SEED, masterSeed)
                .apply();
    }

    private String readMasterSeedFromStorage() {
        try {
            final WalletKeyStoreHandler walletKeyStoreHandler = new WalletKeyStoreHandler(ALIAS);
            final String encryptedMasterSeed = this.prefs.getString(MASTER_SEED, null);
            if (encryptedMasterSeed == null) return null;
            return walletKeyStoreHandler.decrypt(encryptedMasterSeed, this::saveMasterSeed);
        } catch (KeyStoreException e) {
            LogUtil.exception("Error while reading master seed from storage", e);
            throw new IllegalStateException(e);
        }
    }

    public void clear() {
        this.prefs
                .edit()
                .clear()
                .apply();
    }

    private String seedToString(final DeterministicSeed seed) {
        if (seed == null || seed.getMnemonicCode() == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        for (final String word : seed.getMnemonicCode()) {
            sb.append(word).append(" ");
        }

        // Remove the extraneous space character
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
