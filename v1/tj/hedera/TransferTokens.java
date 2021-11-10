package tj.hedera;

import com.hedera.hashgraph.sdk.*;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

public class TransferTokens {
    private static final String HEDERA_NETWORK = "testnet";
    private static final Client HEDERA_CLIENT = Client.forName(HEDERA_NETWORK);

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final TransactionResponse contractExecTransactionResponse = new ContractExecuteTransaction()
                .setContractId(Accounts.TOKEN_1_CONTRACT)
                .setGas(1_000_000)
                .setFunction("transfer", new ContractFunctionParameters()
                        .addAddress(Accounts.ACCOUNT_2_ADDRESS)
                        .addUint256(new BigInteger("250000")))
                .execute(HEDERA_CLIENT);

        final TransactionReceipt receipt = contractExecTransactionResponse.getReceipt(HEDERA_CLIENT);
        System.out.println(receipt);

        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.TOKEN_1_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("balanceOf", new ContractFunctionParameters()
                        .addAddress(Accounts.ACCOUNT_2_ADDRESS))
                .execute(HEDERA_CLIENT);
        final BigInteger balance = contractUpdateResult.getUint256(0);
        System.out.println(balance);
    }
}
