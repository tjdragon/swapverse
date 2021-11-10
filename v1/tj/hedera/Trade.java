package tj.hedera;

import java.math.BigInteger;

public final class Trade {
    public final String counterParty1;
    public final BigInteger amount1;
    public final String token1;
    public final String counterParty2;
    public final BigInteger amount2;
    public final String token2;

    public Trade(String counterParty1, BigInteger amount1, String token1, String counterParty2, BigInteger amount2, String token2) {
        this.counterParty1 = counterParty1;
        this.amount1 = amount1;
        this.token1 = token1;
        this.counterParty2 = counterParty2;
        this.amount2 = amount2;
        this.token2 = token2;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "counterParty1='" + counterParty1 + '\'' +
                ", amount1=" + amount1 +
                ", token1='" + token1 + '\'' +
                ", counterParty2='" + counterParty2 + '\'' +
                ", amount2=" + amount2 +
                ", token2='" + token2 + '\'' +
                '}';
    }
}
