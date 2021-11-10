package tj.hedera;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DecodeHelper {
    public static final Function DYNAMIC_ARRAY_UINT256_FN = new Function("DYNAMIC_ARRAY_UINT256_FN",
            Arrays.asList(),
            Arrays.asList(new TypeReference<DynamicArray<Uint256>>() {})
    );

    public static final Function ORDER_TUPLE = new Function("ORDER_TUPLE",
            Arrays.asList(),
            Arrays.asList(
                    new TypeReference<Address>() {},
                    new TypeReference<Uint256>() {},
                    new TypeReference<Bool>() {},
                    new TypeReference<Uint256>() {},
                    new TypeReference<Uint256>() {})
    );

    public static final Function TRADE_TUPLE = new Function("TRADE_TUPLE",
            Arrays.asList(),
            Arrays.asList(
                    new TypeReference<Address>() {},
                    new TypeReference<Uint256>() {},
                    new TypeReference<Address>() {},
                    new TypeReference<Address>() {},
                    new TypeReference<Uint256>() {},
                    new TypeReference<Address>() {})
    );

    public static BigInteger[] toUint256Array(final String data) {
        final List<BigInteger> array = new LinkedList<>();
        final List<Type> decodeList = FunctionReturnDecoder.decode(data, DYNAMIC_ARRAY_UINT256_FN.getOutputParameters());
        final Type type = decodeList.get(0);
        final ArrayList<Uint256> list = (ArrayList)type.getValue();
        for(Uint256 elem : list) {
            array.add(elem.getValue());
        }
        return array.toArray(new BigInteger[]{});
    }

    public static Order toOrder(final String data) {
        final List<Type> decodeList = FunctionReturnDecoder.decode(data, ORDER_TUPLE.getOutputParameters());
        final Order order = new Order(
                ((Address)decodeList.get(0)).getValue(),
                ((Uint256)decodeList.get(1)).getValue(),
                ((Bool)decodeList.get(2)).getValue(),
                ((Uint256)decodeList.get(3)).getValue(),
                ((Uint256)decodeList.get(4)).getValue()
        );
        return order;
    }

    public static Trade toTrade(final String data) {
        final List<Type> decodeList = FunctionReturnDecoder.decode(data, TRADE_TUPLE.getOutputParameters());
        final Trade trade = new Trade(
                ((Address)decodeList.get(0)).getValue(),
                ((Uint256)decodeList.get(1)).getValue(),
                ((Address)decodeList.get(0)).getValue(),
                ((Address)decodeList.get(0)).getValue(),
                ((Uint256)decodeList.get(4)).getValue(),
                ((Address)decodeList.get(0)).getValue()
        );
        return trade;
    }
}
