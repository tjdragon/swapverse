package tj.hedera;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.PrivateKey;

public final class Accounts {
    public static final AccountId OPERATOR_ID = AccountId.fromString("0.0.....");
    public static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(".....");
    public static final String OPERATOR_ID_ADDRESS = OPERATOR_ID.toSolidityAddress();

    public static final AccountId ACCOUNT_1 = AccountId.fromString("0.0.....");
    public static final String ACCOUNT_1_ADDRESS = ACCOUNT_1.toSolidityAddress();
    public static final PrivateKey ACCOUNT_1_PRIV_KEY = PrivateKey.fromString("...");

    public static final AccountId ACCOUNT_2 = AccountId.fromString("0.0....");
    public static final String ACCOUNT_2_ADDRESS = ACCOUNT_2.toSolidityAddress();
    public static final PrivateKey ACCOUNT_2_PRIV_KEY = PrivateKey.fromString("...");

    public static final ContractId SWAPVERSE_CONTRACT = ContractId.fromString("0.0....");
    public static final ContractId TOKEN_1_CONTRACT = ContractId.fromString("0.0....");
}
