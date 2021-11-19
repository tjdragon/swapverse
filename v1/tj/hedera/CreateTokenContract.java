package tj.hedera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static tj.hedera.Accounts.TOKEN_SUPPLY;

public final class CreateTokenContract {
    private CreateTokenContract() {
    }

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, IOException, ReceiptStatusException {
        long t0 = System.currentTimeMillis();

        final ClassLoader cl = CreateTokenContract.class.getClassLoader();
        final Gson gson = new Gson();
        JsonObject jsonObject;

        try (InputStream jsonStream = cl.getResourceAsStream("TJToken.json")) {
            if (jsonStream == null) {
                throw new RuntimeException("failed to get TJToken.json");
            }
            jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
        }

        final String byteCodeHex = jsonObject.getAsJsonPrimitive("bytecode").getAsString();
        final byte[] byteCode = byteCodeHex.getBytes(StandardCharsets.UTF_8);

        final Client client = HederaClient.CLIENT;

        client.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        client.setDefaultMaxTransactionFee(new Hbar(10000));
        client.setDefaultMaxQueryPayment(new Hbar(1000));

        final TransactionResponse fileTransactionResponse = new FileCreateTransaction()
                .setKeys(Accounts.OPERATOR_KEY)
                .setContents("")
                .execute(client);

        final TransactionReceipt fileReceipt = fileTransactionResponse.getReceipt(client);
        final FileAppendTransaction fat = new FileAppendTransaction()
                .setFileId(fileReceipt.fileId)
                .setMaxChunks(40);
        fat.setContents(byteCode);
        fat.execute(client);

        final FileId newFileId = Objects.requireNonNull(fileReceipt.fileId);

        long tf = (System.currentTimeMillis() - t0) / 1000;
        System.out.println(tf + " secs. Contract bytecode file: " + newFileId);

        final TransactionResponse contractTransactionResponse = new ContractCreateTransaction()
                .setBytecodeFileId(newFileId)
                .setGas(100_000_000)
                .setConstructorParameters(
                        new ContractFunctionParameters()
                                .addUint256(BigInteger.valueOf(TOKEN_SUPPLY))
                                .addString("HBAR")
                                .addString("HBAR")
                )
                .execute(client);

        try {
            final TransactionReceipt contractReceipt = contractTransactionResponse.getReceipt(client);
            final ContractId newContractId = Objects.requireNonNull(contractReceipt.contractId);
            System.out.println("New contract ID: " + newContractId); //  0.0.6860985
            System.out.println("New Contract Solidity address: " + newContractId.toSolidityAddress()); // 000000000000000000000000000000000068b0b9
        } catch (ReceiptStatusException e) {
            e.printStackTrace();
        }
    }
}