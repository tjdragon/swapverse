package tj.hedera;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.TimeoutException;

public final class TransferHBARs {
    private static final String HEDERA_NETWORK = "testnet";
    private static final Client HEDERA_CLIENT = Client.forName(HEDERA_NETWORK);

    private TransferHBARs() {
    }

    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final AccountId recipientId = Accounts.ACCOUNT_1;
        final Hbar amount = new Hbar(200);

        final Hbar senderBalanceBefore = new AccountBalanceQuery()
                .setAccountId(Accounts.OPERATOR_ID)
                .execute(HEDERA_CLIENT)
                .hbars;

        final Hbar receiptBalanceBefore = new AccountBalanceQuery()
                .setAccountId(recipientId)
                .execute(HEDERA_CLIENT)
                .hbars;

        System.out.println("OPERATOR_ID balance = " + senderBalanceBefore);
        System.out.println("RecipientId balance = " + receiptBalanceBefore);

        final TransactionResponse transactionResponse = new TransferTransaction()
                .addHbarTransfer(Accounts.OPERATOR_ID, amount.negated())
                .addHbarTransfer(recipientId, amount)
                .setTransactionMemo("transfer test")
                .execute(HEDERA_CLIENT);

        System.out.println("transaction ID: " + transactionResponse);

        final TransactionRecord record = transactionResponse.getRecord(HEDERA_CLIENT);

        System.out.println("transferred " + amount + "...");

        final Hbar senderBalanceAfter = new AccountBalanceQuery()
                .setAccountId(Accounts.OPERATOR_ID)
                .execute(HEDERA_CLIENT)
                .hbars;

        final Hbar receiptBalanceAfter = new AccountBalanceQuery()
                .setAccountId(recipientId)
                .execute(HEDERA_CLIENT)
                .hbars;

        System.out.println("OPERATOR_ID balance = " + senderBalanceAfter);
        System.out.println("RecipientId balance = " + receiptBalanceAfter);
        System.out.println("Transfer memo: " + record.transactionMemo);
    }
}