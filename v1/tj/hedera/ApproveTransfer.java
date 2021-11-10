package tj.hedera;

import com.hedera.hashgraph.sdk.*;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

public class ApproveTransfer {
    private static final String HEDERA_NETWORK = "testnet";
    private static final Client HEDERA_CLIENT = Client.forName(HEDERA_NETWORK);

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        final String spender = Accounts.SWAPVERSE_CONTRACT.toSolidityAddress();

        HEDERA_CLIENT.setOperator(Accounts.ACCOUNT_1, Accounts.ACCOUNT_1_PRIV_KEY);
        approve(spender);
        allowance(Accounts.ACCOUNT_1_ADDRESS, spender);
        balance(Accounts.ACCOUNT_1_ADDRESS);

        HEDERA_CLIENT.setOperator(Accounts.ACCOUNT_2, Accounts.ACCOUNT_2_PRIV_KEY);
        approve(spender);
        allowance(Accounts.ACCOUNT_2_ADDRESS, spender);
        balance(Accounts.ACCOUNT_2_ADDRESS);
    }

    private static void approve(final String spender) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        System.out.println("*** APPROVE ***");
        final TransactionResponse contractExecTransactionResponse = new ContractExecuteTransaction()
                .setContractId(Accounts.TOKEN_1_CONTRACT)
                .setGas(1_000_000)
                .setFunction("approve", new ContractFunctionParameters()
                        .addAddress(spender)
                        .addUint256(new BigInteger("500000")))
                .execute(HEDERA_CLIENT);

        final TransactionReceipt receipt = contractExecTransactionResponse.getReceipt(HEDERA_CLIENT);
        System.out.println(receipt);
    }

    private static void allowance(final String accountAddress, final String spender) throws PrecheckStatusException, TimeoutException {
        System.out.println("*** ALLOWANCE ***");
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.TOKEN_1_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("allowance", new ContractFunctionParameters()
                        .addAddress(accountAddress)
                        .addAddress(spender))
                .execute(HEDERA_CLIENT);
        final BigInteger allowance = contractUpdateResult.getUint256(0);
        System.out.println("Allowance: " + allowance);
    }

    private static void balance(final String accountAddress) throws PrecheckStatusException, TimeoutException {
        System.out.println("*** BALANCE ***");
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.TOKEN_1_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("balanceOf", new ContractFunctionParameters()
                        .addAddress(accountAddress))
                .execute(HEDERA_CLIENT);
        final BigInteger allowance = contractUpdateResult.getUint256(0);
        System.out.println("Balance: " + allowance);
    }
}
