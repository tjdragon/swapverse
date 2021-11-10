package tj.hedera;

import com.hedera.hashgraph.sdk.*;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeoutException;

public final class SwapVerse {
    private static final String HEDERA_NETWORK = "testnet";
    private static final Client HEDERA_CLIENT = Client.forName(HEDERA_NETWORK);

    public SwapVerse() {
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
                log(" (1): Allow Trading");
                log(" (2): Stop Trading");
                log(" (3): Add Participant");
                log(" (4): Place Limit Order");
                log(" (5): Place Market Order [TODO]");
                log(" (6): Display Order Book");
                log(" (7): Display Trades");
                log("Please enter your choice: ");
                final int choice = readConsoleInt();
                switch (choice) {
                    case 0:
                        System.exit(0);
                    case 1:
                        TransactionReceipt receipt = allowTrading("allow_trading");
                        log(receipt.status + ": " + receipt);
                        break;
                    case 2:
                        receipt = allowTrading("stop_trading");
                        log(receipt.status + ": " + receipt);
                        break;
                    case 3:
                        log("New participant Ethereum address (like " + Accounts.OPERATOR_ID_ADDRESS + "): ");
                        String input = readConsoleStr();
                        receipt = addParticipant(input);
                        log(receipt.status + ": " + receipt);
                        break;
                    case 4:
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
                    case 5:
                        log("Enter market order: is_buy (true), size like 'true 1500': NYI");
                        break;
                    case 6:
                        displayOrderBook();
                        break;
                    case 7:
                        displayTrades();
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
        new SwapVerse();
    }

    private TransactionReceipt placeLimitOrder(final boolean isBuy, final BigInteger size, final BigInteger price) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        final TransactionResponse contractExecTransactionResponse = new ContractExecuteTransaction()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(10_000_000)
                .setFunction("place_limit_order", new ContractFunctionParameters()
                        .addBool(isBuy)
                        .addUint256(size)
                        .addUint256(price))
                .execute(HEDERA_CLIENT);

        return contractExecTransactionResponse.getReceipt(HEDERA_CLIENT);
    }

    private void displayTrades() throws PrecheckStatusException, TimeoutException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final ContractFunctionResult nbTradesQuery = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("get_trade_size")
                .execute(HEDERA_CLIENT);
        final BigInteger nbTrades = nbTradesQuery.getUint256(0);

        if (nbTrades.equals(BigInteger.ZERO)) {
            log("No trades");
        } else {
            log("-- TOTAL NUMBER OF TRADES: " + nbTrades.intValue());
            for(int i = 0; i < nbTrades.intValue(); i++) {
                final Trade trade = getTrade(new BigInteger(""+i));
                log(" " + trade);
            }
        }
    }

    private Trade getTrade(final BigInteger index) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("get_trade", new ContractFunctionParameters()
                        .addUint256(index))
                .execute(HEDERA_CLIENT);

        final String raw = Hex.toHexString(contractUpdateResult.asBytes());
        return DecodeHelper.toTrade(raw);
    }

    private void displayOrderBook() throws PrecheckStatusException, TimeoutException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final BigInteger[] buy_prices = prices("buy_prices");
        final List<Order> buyOrders = new LinkedList<>();
        for(BigInteger p : buy_prices) {
            final BigInteger[] buy_order_ids = orderIds("buy_order_ids", p);
            for(BigInteger oid : buy_order_ids) {
                final Order order = getOrder(oid);
                buyOrders.add(order);
            }
        }

        final BigInteger[] sell_prices = prices("sell_prices");
        final List<Order> sellOrders = new LinkedList<>();
        for(BigInteger p : sell_prices) {
            final BigInteger[] sell_order_ids = orderIds("sell_order_ids", p);
            for(BigInteger oid : sell_order_ids) {
                final Order order = getOrder(oid);
                sellOrders.add(order);
            }
        }

        Collections.reverse(buyOrders);

        log("ORDER BOOK HBAR/BTC");
        log("--Buy---------Sell-");
        for(Order buyOrder : buyOrders) {
            log(owner(buyOrder) + " "  + buyOrder.size + " @ " + buyOrder.price);
        }
        for(Order sellOrder : sellOrders) {
            log("\t\t\t\t" + sellOrder.size + " @ " + sellOrder.price + " " + owner(sellOrder));
        }
        log("___________________");
    }

    private String owner(Order order) {
        return order.address.substring(order.address.length() -3);
    }

    private BigInteger[] prices(final String ops) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
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
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction(ops, new ContractFunctionParameters()
                        .addUint256(price))
                .execute(HEDERA_CLIENT);

        final String raw = Hex.toHexString(contractUpdateResult.asBytes());
        final BigInteger[] bigIntegers = DecodeHelper.toUint256Array(raw);
        return bigIntegers;
    }

    private Order getOrder(final BigInteger orderId) throws PrecheckStatusException, TimeoutException {
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction("get_order_tuple", new ContractFunctionParameters()
                        .addUint256(orderId))
                .execute(HEDERA_CLIENT);

        final String raw = Hex.toHexString(contractUpdateResult.asBytes());
        return DecodeHelper.toOrder(raw);
    }

    @Deprecated
    private void orders(final String ops, final BigInteger price) throws PrecheckStatusException, TimeoutException {
        log("orders("+ops+","+price+")");
        final ContractFunctionResult contractUpdateResult = new ContractCallQuery()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(100_000) // gasUsed=2876
                .setQueryPayment(new Hbar(1))
                .setFunction(ops, new ContractFunctionParameters()
                        .addUint256(price))
                .execute(HEDERA_CLIENT);
        if (contractUpdateResult.errorMessage != null) {
            log("Error: " + contractUpdateResult.errorMessage);
        } else {
            final String raw = Hex.toHexString(contractUpdateResult.asBytes());
            log(":: " + DecodeHelper.toOrder(raw));
        }
    }

    private TransactionReceipt addParticipant(final String participant) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final TransactionResponse contractExecTransactionResponse = new ContractExecuteTransaction()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(50_000)
                .setFunction("add_participant", new ContractFunctionParameters()
                        .addAddress(participant))
                .execute(HEDERA_CLIENT);

        return contractExecTransactionResponse.getReceipt(HEDERA_CLIENT);
    }

    private TransactionReceipt allowTrading(final String ops) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        HEDERA_CLIENT.setOperator(Accounts.OPERATOR_ID, Accounts.OPERATOR_KEY);

        final TransactionResponse contractExecTransactionResponse = new ContractExecuteTransaction()
                .setContractId(Accounts.SWAPVERSE_CONTRACT)
                .setGas(10_000)
                .setFunction(ops)
                .execute(HEDERA_CLIENT);

        return contractExecTransactionResponse.getReceipt(HEDERA_CLIENT);
    }
}
