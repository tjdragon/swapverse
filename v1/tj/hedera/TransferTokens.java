package tj.hedera;

import com.hedera.hashgraph.sdk.*;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

import static tj.hedera.Accounts.TOKEN_TRANSFERRED;

public class TransferTokens {
    private static final Client HEDERA_CLIENT = HederaClient.CLIENT;

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        transfer(Accounts.ACCOUNT_1_ADDRESS, Accounts.HBAR_TOKEN);
        transfer(Accounts.ACCOUNT_2_ADDRESS, Accounts.HBAR_TOKEN);
        transfer(Accounts.ACCOUNT_1_ADDRESS, Accounts.BTC_TOKEN);
        transfer(Accounts.ACCOUNT_2_ADDRESS, Accounts.BTC_TOKEN);
    }

    private static void transfer(final String to, final ContractId contractId) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        final TransactionResponse contractExecTransactionResponse = new ContractExecuteTransaction()
                .setContractId(contractId)
                .setGas(1_000_000)
                .setFunction("transfer", new ContractFunctionParameters()
                        .addAddress(to)
                        .addUint256(BigInteger.valueOf(TOKEN_TRANSFERRED)))
                .execute(HEDERA_CLIENT);

        final TransactionReceipt receipt = contractExecTransactionResponse.getReceipt(HEDERA_CLIENT);
        System.out.println(receipt);

        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(contractId)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("balanceOf", new ContractFunctionParameters()
                        .addAddress(to))
                .execute(HEDERA_CLIENT);
        final BigInteger balance = contractUpdateResult.getUint256(0);
        System.out.println(balance);
    }
}
