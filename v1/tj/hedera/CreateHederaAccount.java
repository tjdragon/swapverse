package tj.hedera;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.TimeoutException;

public class CreateHederaAccount {
    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        final Client client = HederaClient.CLIENT;
        client.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        // Generate a Ed25519 private, public key pair
        final PrivateKey privateKey = PrivateKey.generate();
        final PublicKey publicKey = privateKey.getPublicKey();

        System.out.println("private key = " + privateKey);
        System.out.println("public key = " + publicKey);

        final TransactionResponse transactionResponse = new AccountCreateTransaction()
                .setKey(publicKey)
                .setInitialBalance(new Hbar(250))
                .execute(client);

        // This will wait for the receipt to become available
        final TransactionReceipt receipt = transactionResponse.getReceipt(client);

        final AccountId newAccountId = receipt.accountId;

        System.out.println("account = " + newAccountId);
    }
}
