package tj.hedera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static tj.hedera.Accounts.*;


public final class CreateSwapVerseContract {
    private static final String HEDERA_NETWORK = "testnet";

    private CreateSwapVerseContract() {
    }

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, IOException, ReceiptStatusException {
        long t0 = System.currentTimeMillis();
        final String solidityAddress = OPERATOR_ID.toSolidityAddress();
        System.out.println("OPERATOR_ID: " + solidityAddress); // 0000000000000000000000000000000000001daf

        final ClassLoader cl = CreateSwapVerseContract.class.getClassLoader();
        final Gson gson = new Gson();
        JsonObject jsonObject;

        try (InputStream jsonStream = cl.getResourceAsStream("SwapVerse.json")) {
            if (jsonStream == null) {
                throw new RuntimeException("failed to get SwapVerse.json");
            }
            jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
        }

        final String byteCodeHex = jsonObject.getAsJsonPrimitive("bytecode").getAsString();
        final byte[] byteCode = byteCodeHex.getBytes(StandardCharsets.UTF_8);

        final Client client = Client.forName(HEDERA_NETWORK);

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        client.setDefaultMaxTransactionFee(new Hbar(100));
        client.setDefaultMaxQueryPayment(new Hbar(10));

        final TransactionResponse fileTransactionResponse = new FileCreateTransaction()
                .setKeys(OPERATOR_KEY)
                .setContents("")
                .execute(client);

        final TransactionReceipt fileReceipt = fileTransactionResponse.getReceipt(client);
        final FileAppendTransaction fat = new FileAppendTransaction()
                .setFileId(fileReceipt.fileId)
                .setMaxChunks(50);
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
                                .addAddress(TOKEN_1_CONTRACT.toSolidityAddress())
                                .addAddress(TOKEN_1_CONTRACT.toSolidityAddress()) // Laziness
                )
                .execute(client);

        final TransactionReceipt contractReceipt = contractTransactionResponse.getReceipt(client);
        final ContractId newContractId = Objects.requireNonNull(contractReceipt.contractId);

        System.out.println("New contract ID: " + newContractId); // 0.0.6861401
    }
}