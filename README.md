# SwapVerse

This project investigates [CLOB](https://en.wikipedia.org/wiki/Central_limit_order_book) using [Solidity](https://docs.soliditylang.org/) with the ultimate purpose to execute on [Hedera](https://hedera.com/).

## Intro

A [CLOB FIFO](https://en.wikipedia.org/wiki/Order_matching_system) is a typical order matching algorithm for centralised exchanges.

[While order book mechanisms are the dominant medium
of exchange of electronic assets in traditional finance [13], they are challenging to use within
a smart contract environment. The size of the state needed by an order book to represent
the set of outstanding orders (e.g., passive liquidity) is large and extremely costly in the
smart contract environment, where users must pay for space and compute power utilized](https://web.stanford.edu/~guillean/papers/uniswap_analysis.pdf) (An analysis of Uniswap markets).

The above is the reason why we do not see decentralised CLOB in existing DEXs... until [Solana](https://blog.saber.so/solana-dex-ecosystem-exploring-amms-and-clobs-ceafb78353fe) and [Hedera](https://hedera.com/hh-ieee_coins_paper-200516.pdf).

The below is my personal investigation of CLOBs in Solidity to be executed on Hedera while learning to code [Solidity](https://docs.soliditylang.org/), [Hardhat](https://hardhat.org/), [Remix](http://remix.ethereum.org/), [Hedera](https://docs.hedera.com/guides/docs/sdks/smart-contracts/create-a-smart-contract) and [VS Studio](https://code.visualstudio.com/)

### Notes about the code
The code is most likely buggy and not so well designed with a sub-optimal implementation. 
That is not the point.
It is about exploring the ease of coding of decentralised CLOBs and governance.

## Design choices
[SwapVerse.sol](https://github.com/tjdragon/swapverse/blob/main/SwapVerse.sol) allows Participants to add Limit Orders and Market Orders.
The contract is designed to swap a given crypto ERC20 pair - hence the constructor takes two parameters which are the addresses of the ERC20 contracts.

```solidity
constructor(address token_address_1, address token_address_2)
```

The contract has obviously an owner and only specific operations are allowed for this owner:

- stop_trading
- allow_trading
- add_participant
- remove_participant

Orders are placed in a mapping structure (_orders) and there are two sorted arrays representing
the buy (_buy_prices) and sell prices (_sell_prices).
I used a sort on insert algorithm (add_price) which seems to be the easiest way to maintain a sorted array. Obviously, buy prices are ordered in decreasing order, sell prices in increasing order so that a typical CLOB for a given crypto pair (let's say HBAR/BTC) would be represented as:

Buy | Sell
--- | ---
.   | 500 at 14
.   | 50 at 13
100 @ 12 | .
80 @ 11 | .
200 @ 10 | .

Second to that price structure is the mapping from a price to an array of order ids (_buy_list & _sell_list).
If you want to reconstruct the above CLOB, for each side, let's say the buy side, call buy_prices and for each price, call buy_orders which will return the Order type:

```solidity
struct Order {
    address owner;
    uint256 id;
    bool is_buy; 
    uint256 size; 
    uint256 price;
}
```

Adopting the same principle from [FX](https://www.investopedia.com/terms/c/currencypair.asp), and taking HBAR/BTC has an example (HBAR is the base currency, BTC is the quoted currency), 
1 HBAR would cost at this time of writing 0.000006 BTC. 
Another design decision would be to only work with integers, therefore 1 HBAR equals to 600 Satoshis, the CLOB would more or less look like:

Buy | Sell
--- | ---
.   | 500 at 633
.   | 50 at 632
100 @ 631 | .
80 @ 630 | .
200 @ 629 | .

If a trade happens for a Sell for 50 lots at 631, it means that the buyer will buy 50 HBAR for 631 * 50 = 31550 Satoshis and the seller will give away 31550 Satoshis to receive 50 HBAR resulting in a new CLOB:

Buy | Sell
--- | ---
.   | 500 at 633
.   | 50 at 632
50 @ 631 | .
80 @ 630 | .
200 @ 1629 | .

The functions that deal with order placement are matching are:

```solidity
function place_market_order(bool is_buy, uint256 size) public tradingAllowed returns (uint256)
function place_limit_order(bool is_buy, uint256 size, uint256 price) public tradingAllowed returns (uint256)
function internal_place_limit_order(uint256 orderId, bool is_buy) private
function process_matched_order(uint256 orderId, uint256[] storage matched_ids, bool is_buy) private 
```

## Settlement
I believe settlement represents the crux of the matter due [settlement risk](https://www.investopedia.com/terms/s/settlementrisk.asp), i.e., "the possibility that one or more parties will fail to deliver on the terms of a contract at the agreed-upon time".  
When a trade happens (See process_trade), assets must be transferred between parties.
... 

### TODO
Proper calculation for sizes
