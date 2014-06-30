package io.bitsquare.gui.util;

import com.google.bitcoin.core.*;
import com.google.bitcoin.script.Script;
import io.bitsquare.btc.BtcFormatter;
import io.bitsquare.gui.components.confidence.ConfidenceProgressIndicator;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("CanBeFinal")
public class ConfidenceDisplay
{
    private static final Logger log = LoggerFactory.getLogger(ConfidenceDisplay.class);
    @Nullable
    private WalletEventListener walletEventListener;

    @NotNull
    private Wallet wallet;
    @NotNull
    private Label confirmationLabel;
    @Nullable
    private TextField balanceTextField;
    private Transaction transaction;
    @NotNull
    private ConfidenceProgressIndicator progressIndicator;

    public ConfidenceDisplay(@NotNull Wallet wallet, @NotNull Label confirmationLabel, @NotNull TextField balanceTextField, @NotNull ConfidenceProgressIndicator progressIndicator)
    {
        this.wallet = wallet;
        this.confirmationLabel = confirmationLabel;
        this.balanceTextField = balanceTextField;
        this.progressIndicator = progressIndicator;

        balanceTextField.setText("");
        confirmationLabel.setVisible(false);
        progressIndicator.setVisible(false);
        progressIndicator.setProgress(0);

        updateBalance(wallet.getBalance());
        walletEventListener = new WalletEventListener()
        {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, BigInteger prevBalance, @NotNull BigInteger newBalance)
            {
                updateBalance(newBalance);
                // log.debug("onCoinsReceived  " + newBalance);
            }

            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, @NotNull Transaction tx)
            {
                updateConfidence(tx);
                // log.debug("onTransactionConfidenceChanged tx " + tx.getHashAsString());
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, BigInteger prevBalance, @NotNull BigInteger newBalance)
            {
                updateBalance(newBalance);
            }

            @Override
            public void onReorganize(Wallet wallet)
            {
            }

            @Override
            public void onWalletChanged(Wallet wallet)
            {
            }

            @Override
            public void onKeysAdded(Wallet wallet, List<ECKey> keys)
            {
            }

            @Override
            public void onScriptsAdded(Wallet wallet, List<Script> scripts)
            {
            }
        };
        wallet.addEventListener(walletEventListener);
    }

    public ConfidenceDisplay(@NotNull Wallet wallet, @NotNull Label confirmationLabel, @NotNull final Transaction transaction, @NotNull ConfidenceProgressIndicator progressIndicator)
    {
        this.wallet = wallet;
        this.confirmationLabel = confirmationLabel;
        this.transaction = transaction;
        this.progressIndicator = progressIndicator;

        confirmationLabel.setVisible(false);
        progressIndicator.setVisible(false);
        progressIndicator.setProgress(0);

        updateBalance(wallet.getBalance());
        updateConfidence(transaction);

        walletEventListener = new WalletEventListener()
        {
            @Override
            public void onCoinsReceived(Wallet wallet, @NotNull Transaction tx, BigInteger prevBalance, @NotNull BigInteger newBalance)
            {
                if (tx.getHashAsString().equals(transaction.getHashAsString()))
                    updateBalance(newBalance);
                // log.debug("onCoinsReceived " + newBalance);
            }

            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, @NotNull Transaction tx)
            {
                if (tx.getHashAsString().equals(transaction.getHashAsString()))
                    updateConfidence(transaction);
                // log.debug("onTransactionConfidenceChanged newTransaction " + newTransaction.getHashAsString());
            }

            @Override
            public void onCoinsSent(Wallet wallet, @NotNull Transaction tx, BigInteger prevBalance, @NotNull BigInteger newBalance)
            {
                if (tx.getHashAsString().equals(transaction.getHashAsString()))
                    updateBalance(newBalance);
            }

            @Override
            public void onReorganize(Wallet wallet)
            {
            }

            @Override
            public void onWalletChanged(Wallet wallet)
            {
            }

            @Override
            public void onKeysAdded(Wallet wallet, List<ECKey> keys)
            {
            }

            @Override
            public void onScriptsAdded(Wallet wallet, List<Script> scripts)
            {
            }
        };
        wallet.addEventListener(walletEventListener);
    }

    public void destroy()
    {
        wallet.removeEventListener(walletEventListener);
        progressIndicator.setProgress(0);
        confirmationLabel.setText("");
        if (balanceTextField != null)
            balanceTextField.setText("");
    }

    private void updateBalance(@NotNull BigInteger balance)
    {
        if (balance.compareTo(BigInteger.ZERO) > 0)
        {
            confirmationLabel.setVisible(true);
            progressIndicator.setVisible(true);
            progressIndicator.setProgress(-1);

            Set<Transaction> transactions = wallet.getTransactions(false);
            @Nullable Transaction latestTransaction = null;
            for (@NotNull Transaction transaction : transactions)
            {
                if (latestTransaction != null)
                {
                    if (transaction.getUpdateTime().compareTo(latestTransaction.getUpdateTime()) > 0)
                    {
                        latestTransaction = transaction;
                    }
                }
                else
                {
                    latestTransaction = transaction;
                }
            }
            if (latestTransaction != null && (transaction == null || latestTransaction.getHashAsString().equals(transaction.getHashAsString())))
                updateConfidence(latestTransaction);
        }

        if (balanceTextField != null)
            balanceTextField.setText(BtcFormatter.satoshiToString(balance));
    }

    private void updateConfidence(@NotNull Transaction tx)
    {
        TransactionConfidence confidence = tx.getConfidence();
        double progressIndicatorSize = 50;
        switch (confidence.getConfidenceType())
        {
            case UNKNOWN:
                confirmationLabel.setText("");
                progressIndicator.setProgress(0);
                break;
            case PENDING:
                confirmationLabel.setText("Seen by " + confidence.numBroadcastPeers() + " peer(s) / 0 confirmations");
                progressIndicator.setProgress(-1.0);
                progressIndicatorSize = 20;
                break;
            case BUILDING:
                confirmationLabel.setText("Confirmed in " + confidence.getDepthInBlocks() + " block(s)");
                progressIndicator.setProgress(Math.min(1, (double) confidence.getDepthInBlocks() / 6.0));
                break;
            case DEAD:
                confirmationLabel.setText("Transaction is invalid.");
                break;
        }

        progressIndicator.setMaxHeight(progressIndicatorSize);
        progressIndicator.setPrefHeight(progressIndicatorSize);
        progressIndicator.setMaxWidth(progressIndicatorSize);
        progressIndicator.setPrefWidth(progressIndicatorSize);
    }


}
