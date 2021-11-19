package tj.hedera;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.TimeoutException;

public final class GetHederaAccountBalance {
    private static final Client client = HederaClient.CLIENT;

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException {
        System.out.println("OPERATOR_ID balance = " + getBalance(Accounts.OPERATOR_ID));
        System.out.println("ACCOUNT_1 balance = " + getBalance(Accounts.ACCOUNT_1));
        System.out.println("ACCOUNT_2 balance = " + getBalance(Accounts.ACCOUNT_2));
    }

    private static Hbar getBalance(final AccountId accountId) throws PrecheckStatusException, TimeoutException {
        return new AccountBalanceQuery()
                .setAccountId(accountId)
                .execute(client)
                .hbars;
    }
}