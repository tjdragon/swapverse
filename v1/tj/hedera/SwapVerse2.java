package tj.hedera;

import com.hedera.hashgraph.sdk.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeoutException;

// sudo docker-compose up
public final class SwapVerse2 {
    private static final Client HEDERA_CLIENT = HederaClient.CLIENT;

    public SwapVerse2() {
        log("SwapVerse by " + Accounts.OPERATOR_ID_ADDRESS);
        log(" Trading account 1: " + Accounts.ACCOUNT_1_ADDRESS);
        log(" Trading account 2: " + Accounts.ACCOUNT_2_ADDRESS);

        HEDERA_CLIENT.setDefaultMaxTransactionFee(new Hbar(100_000_000));
        HEDERA_CLIENT.setDefaultMaxQueryPayment(new Hbar(100_000_000));

        menu();
    }

    private void menu() {
        while(true) {
            try {
                log("");
                log("SWAPVERSE MENU - HEDERA NETWORK}");
                log(" (0): Quit");
                log(" (1): Place Limit Order");
                log(" (2): Display Order Book");
                log(" (3): Display Accounts");
                log("Please enter your choice: ");
                String input = null;
                TransactionReceipt receipt = null;
                final int choice = readConsoleInt();
                switch (choice) {
                    case 0:
                        System.exit(0);
                    case 1:
                        log("Enter limit order: participant (1|2) is_buy (true), size, price like '1 true 1500 631': ");
                        input = readConsoleStr();
                        final String[] data = input.split("\\s+");
                        final int participant = Integer.parseInt(data[0]);
                        if (participant == 1) {
                            HEDERA_CLIENT.setOperator(Accounts.ACCOUNT_1, Accounts.ACCOUNT_1_PRIV_KEY);
                        } else {
                            HEDERA_CLIENT.setOperator(Accounts.ACCOUNT_2, Accounts.ACCOUNT_2_PRIV_KEY);
                        }

                        final boolean is_buy = Boolean.parseBoolean(data[1]);
                        final BigInteger size = new BigInteger(data[2]);
                        final BigInteger price = new BigInteger(data[3]);
                        receipt = placeLimitOrder(is_buy, size, price);
                        log(receipt.status + ": " + receipt);
                        break;
                    case 2:
                        displayOrderBook();
                        break;
                    case 3:
                        displayAccounts();
                        break;
                }
            } catch (Exception e) {
                log("Error: " + e.getMessage());
            }
        }
    }

    private void log(final Object o) {
        System.out.println(o);
    }

    private String readConsoleStr() {
        return new Scanner(System.in).nextLine();
    }

    private int readConsoleInt() {
        return new Scanner(System.in).nextInt();
    }

    public static void main(String[] args) {
        new SwapVerse2();
    }

    // Remix shows 286 985 gas cost to create one
    // Full trade shows 80,000,000
    private TransactionReceipt placeLimitOrder(final boolean isBuy, final BigInteger size, final BigInteger price) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        final String orderId = RandomStringUtils.randomAlphabetic(5).toLowerCase();
        log("Creating order id " + orderId + " ...");
        final TransactionResponse contractExecTransactionResponse = new ContractExecuteTransaction()
                .setContractId(Accounts.SWAPVERSE2_CONTRACT)
                .setGas(Long.MAX_VALUE)
                .setFunction("place_limit_order", new ContractFunctionParameters()
                        .addString(orderId)
                        .addBool(isBuy)
                        .addUint256(size)
                        .addUint256(price))
                .execute(HEDERA_CLIENT);

        return contractExecTransactionResponse.getReceipt(HEDERA_CLIENT);
    }

    private void displayAccounts() throws PrecheckStatusException, TimeoutException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final BigInteger acc1HBARBalance = balance(Accounts.ACCOUNT_1_ADDRESS, Accounts.HBAR_TOKEN);
        final BigInteger acc1BTCBalance = balance(Accounts.ACCOUNT_1_ADDRESS, Accounts.BTC_TOKEN);

        final BigInteger acc1AllowanceHBARBalance = allowance(Accounts.ACCOUNT_1_ADDRESS, Accounts.SWAPVERSE2_CONTRACT.toSolidityAddress(), Accounts.HBAR_TOKEN);
        final BigInteger acc1AllowanceBTCBalance = allowance(Accounts.ACCOUNT_1_ADDRESS, Accounts.SWAPVERSE2_CONTRACT.toSolidityAddress(), Accounts.BTC_TOKEN);

        log("-- ACCOUNTS");
        log("  ACCOUNT 1: " + Accounts.ACCOUNT_1_ADDRESS);
        log("    HBAR Balance: " + acc1HBARBalance);
        log("    BTC Balance: " + acc1BTCBalance);
        log("    HBAR Allowance: " + acc1AllowanceHBARBalance);
        log("    BTC Allowance: " + acc1AllowanceBTCBalance);

        final BigInteger acc2HBARBalance = balance(Accounts.ACCOUNT_2_ADDRESS, Accounts.HBAR_TOKEN);
        final BigInteger acc2BTCBalance = balance(Accounts.ACCOUNT_2_ADDRESS, Accounts.BTC_TOKEN);

        final BigInteger acc2AllowanceHBARBalance = allowance(Accounts.ACCOUNT_2_ADDRESS, Accounts.SWAPVERSE2_CONTRACT.toSolidityAddress(), Accounts.HBAR_TOKEN);
        final BigInteger acc2AllowanceBTCBalance = allowance(Accounts.ACCOUNT_2_ADDRESS, Accounts.SWAPVERSE2_CONTRACT.toSolidityAddress(), Accounts.BTC_TOKEN);

        log("  ACCOUNT 2: " + Accounts.ACCOUNT_2_ADDRESS);
        log("    HBAR Balance: " + acc2HBARBalance);
        log("    BTC Balance: " + acc2BTCBalance);
        log("    HBAR Allowance: " + acc2AllowanceHBARBalance);
        log("    BTC Allowance: " + acc2AllowanceBTCBalance);
    }

    private static BigInteger balance(final String accountAddress, final ContractId contractId) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(contractId)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("balanceOf", new ContractFunctionParameters()
                        .addAddress(accountAddress))
                .execute(HEDERA_CLIENT);
        return contractUpdateResult.getUint256(0);
    }

    private static BigInteger allowance(final String accountAddress, final String spender, final ContractId contractId) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(contractId)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("allowance", new ContractFunctionParameters()
                        .addAddress(accountAddress)
                        .addAddress(spender))
                .execute(HEDERA_CLIENT);
        return contractUpdateResult.getUint256(0);
    }

    private void displayOrderBook() throws PrecheckStatusException, TimeoutException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final BigInteger[] buy_prices = prices("buy_prices");
        final List<Order2> buyOrders = new LinkedList<>();
        for(BigInteger p : buy_prices) {
            final BigInteger[] buy_order_ids = orderIds("buy_order_ids", p);
            for(BigInteger oid : buy_order_ids) {
                final Order2 order = getOrder(oid);
                buyOrders.add(order);
            }
        }

        final BigInteger[] sell_prices = prices("sell_prices");
        final List<Order2> sellOrders = new LinkedList<>();
        for(BigInteger p : sell_prices) {
            final BigInteger[] sell_order_ids = orderIds("sell_order_ids", p);
            for(BigInteger oid : sell_order_ids) {
                final Order2 order = getOrder(oid);
                sellOrders.add(order);
            }
        }

        Collections.reverse(buyOrders);

        log("ORDER BOOK HBAR/BTC");
        log("--Buy---------Sell-");
        for(Order2 buyOrder : buyOrders) {
            log(buyOrder.id + " " + owner(buyOrder) + " "  + buyOrder.size + " @ " + buyOrder.price);
        }
        for(Order2 sellOrder : sellOrders) {
            log("\t\t\t\t" + sellOrder.size + " @ " + sellOrder.price + " " + owner(sellOrder) + " " + sellOrder.id);
        }
        log("___________________");
    }

    private String owner(Order2 order) {
        return order.address.substring(order.address.length() -3);
    }

    private BigInteger[] prices(final String ops) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE2_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction(ops)
                .execute(HEDERA_CLIENT);

        final String raw = Hex.toHexString(contractUpdateResult.asBytes());
        final BigInteger[] bigIntegers = DecodeHelper.toUint256Array(raw);
        return bigIntegers;
    }

    private BigInteger[] orderIds(final String ops, final BigInteger price) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE2_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction(ops, new ContractFunctionParameters()
                        .addUint256(price))
                .execute(HEDERA_CLIENT);

        final String raw = Hex.toHexString(contractUpdateResult.asBytes());
        final BigInteger[] bigIntegers = DecodeHelper.toUint256Array(raw);
        return bigIntegers;
    }

    private Order2 getOrder(final BigInteger orderId) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE2_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("get_order", new ContractFunctionParameters()
                        .addUint256(orderId))
                .execute(HEDERA_CLIENT);

        final String raw = Hex.toHexString(contractUpdateResult.asBytes());
        return DecodeHelper.toOrder2(raw);
    }
}
