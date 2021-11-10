package tj.hedera;

import java.math.BigInteger;

public final class Order {
    public final String address;
    public final BigInteger id;
    public final boolean isBuy;
    public final BigInteger size;
    public final BigInteger price;

    public Order(String address, BigInteger id, boolean isBuy, BigInteger price, BigInteger size) {
        this.address = address;
        this.id = id;
        this.isBuy = isBuy;
        this.size = size;
        this.price = price;
    }

    @Override
    public String toString() {
        return "Order{" +
                "address='" + address + '\'' +
                ", id=" + id +
                ", isBuy=" + isBuy +
                ", size=" + size +
                ", price=" + price +
                '}';
    }
}
