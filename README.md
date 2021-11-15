# SwapVerse

This project investigates [CLOB](https://en.wikipedia.org/wiki/Central_limit_order_book) using [Solidity](https://docs.soliditylang.org/) with the ultimate purpose to execute on [Hedera](https://hedera.com/).  

If you like what you see, please donate some BTC to bc1qf3gsvfk0yp9fvw0k8xvq7a8dk80rqw0apcy8kx or some ETH to 0xcDE1EcaFCa4B4c7A6902c648CD01db52d8c943F3

## Intro

A [CLOB FIFO](https://en.wikipedia.org/wiki/Order_matching_system) is a typical order matching algorithm for centralized exchanges.

"While order book mechanisms are the dominant medium of exchange of electronic assets in traditional finance [13], they are challenging to use within a smart contract environment. 
The size of the state needed by an order book to represent the set of outstanding orders (e.g., passive liquidity) is large and extremely costly in the
smart contract environment, where users must pay for space and compute power utilized" ([An analysis of Uniswap markets](https://web.stanford.edu/~guillean/papers/uniswap_analysis.pdf)).

The above is the reason why we do not see decentralised CLOB in existing DEXs... until [Solana](https://blog.saber.so/solana-dex-ecosystem-exploring-amms-and-clobs-ceafb78353fe)'s [Serum](https://solana.com/ecosystem/serum) and [Hedera](https://hedera.com/hh-ieee_coins_paper-200516.pdf).

The below is my personal investigation of CLOBs in Solidity to be executed on Hedera while learning to code in [Solidity](https://docs.soliditylang.org/) using [Hardhat](https://hardhat.org/), [Remix](http://remix.ethereum.org/), [Hedera](https://docs.hedera.com/guides/docs/sdks/smart-contracts/create-a-smart-contract), [web3j](https://docs.web3j.io/) and [VS Studio](https://code.visualstudio.com/).

I definitely should have taken the [blue pill](https://en.wikipedia.org/wiki/Red_pill_and_blue_pill) on this one!

### Notes about the code
The code is most likely buggy and not so well designed with a sub-optimal implementation. 
That is not the point.
It is about exploring the ease of coding of decentralized CLOBs and governance.

## Design choices
Version 1 of [SwapVerse.sol](https://github.com/tjdragon/swapverse/blob/main/v1/tj/hedera/SwapVerse.sol) allows Participants to add Limit Orders and Market Orders.
The contract is designed to swap a given crypto ERC20 pair - hence the constructor takes two parameters which are the addresses of the ERC20 contracts.

Version 2 (Updated on Nov, 15th 2021) [SwapVerse2.sol](https://github.com/tjdragon/swapverse/blob/main/v1/tj/hedera/SwapVerse2.sol) is a simpler implementation, without recursion.
SwapVerse2.sol has only a subset of the operations from SwapVerse1.sol for now.

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
200 @ 629 | .

If there is now another Sell for 100 lots at 631, the new CLOB would be:

Buy | Sell
--- | ---
.   | 500 at 633
.   | 50 at 632
.   | 50 at 631
80 @ 630 | .
200 @ 629 | .

The functions that deal with order placement and matching are:

```solidity
function place_market_order(bool is_buy, uint256 size) public tradingAllowed returns (uint256)
function place_limit_order(bool is_buy, uint256 size, uint256 price) public tradingAllowed returns (uint256)
function internal_place_limit_order(uint256 orderId, bool is_buy) private
function process_matched_order(uint256 orderId, uint256[] storage matched_ids, bool is_buy) private 
```

## Settlement
I believe settlement represents the crux of the matter due [settlement risk](https://www.investopedia.com/terms/s/settlementrisk.asp), i.e., "the possibility that one or more parties will fail to deliver on the terms of a contract at the agreed-upon time".  
When a trade happens (See process_trade), assets must be transferred between parties.  

There are a few options to settle, each of them have merits and demerits: there is a the [Uniswap](https://academy.binance.com/en/articles/what-is-uniswap-and-how-does-it-work) model for example that has proved to be successful, but is it the one that institutions would be conformable with (coin provenance for [KYC](https://en.wikipedia.org/wiki/Know_your_customer) & [AML](https://www.investopedia.com/terms/a/aml.asp), compliance with the [FATF-16](https://www.fatf-gafi.org/media/fatf/documents/recommendations/Updated-Guidance-VA-VASP.pdf) rule)?

Another model would be that once a TradeEvent has been issued, custodians would notify participants for settlement (manually or via API, or ideally do it on the behalf-of).

For learning purposes, I went for the ERC20 approve functions which allows a participant to delegate transfer capability up to a certain amount to another address - like a smart contract:

```js
await _link_token.connect(_addr1).approve(_stl.address, 100);
```
Allowing the transfer from the smart contract:

```solidity
token_1.transferFrom(adr1, adr2, amount1);
```

## Testing

I use three testing methods: Hardhat, Remix and the Hedera test network.

### With Hardhat

Please check all the .js files in the repo. 

### With Remix

Copy and paste the SwapVerse.sol in [Remix](http://remix.ethereum.org/)

## Hedera

Hedera allows the deployment of Solidity smart contracts using [Besu EVM](https://www.hyperledger.org/use/besu) using the [Hashgraph](https://en.wikipedia.org/wiki/Hashgraph) consensus algorithm which gives blazing fast settlement finality.  
Solidity smart contracts therefore run "as-is" without any modification! 

[Java code samples](https://github.com/hashgraph/hedera-sdk-java/tree/main/examples/src/main/java) are here to help.

Use the Java code to deploy various contracts and transfer tokens (and update Accounts.java accordingly), in order:

- Execute CreateTokenContract which creates an ERC20 token (TJToken) with a 5_000_000 initial supply
- Execute TransferTokens to transfer tokens to Accounts 1 & 2
- Execute ApproveTransfer to approve the smart contract to spend tokens on behalf of the Accounts 1 & 2
- Execute SwapVerse first using the OPERATOR ID account to enable trading and add participants (Accounts 1 & 2)
- Execute SwapVerse as Account 1 and 2 to place orders and get trades

# Conclusion

## At this time of writing, on Nov 10th 2021:

- Code for this version is in [v1](https://github.com/tjdragon/swapverse/tree/main/v1/tj/hedera)
- Smart contracts on Hedera are not for prime usage for several reasons: No access to logs, no access to events (emit), no tool chain to develop and debug with ease
 (When the Solidity code works on Hardhat and Remix, you'd expect it to "work" on Hedera out-of-the box but the development experience is not great, 300000 gas limit)
- Avoid recursive functions in Solidity - a prefer changing states to functional-style programming
- Hedera has a great future for smart contracts but they need to get things right from day 1, focusing on developer productivity.

## Next Steps
- To modify SwapVers.sol and make it more efficient (gas-wise) so that I do not hit the 300000 gas limit (Update on Nov, 15h 2021: See [SwapVerse2.sol](https://github.com/tjdragon/swapverse/blob/main/v1/tj/hedera/SwapVerse2.sol))

## References
- [ethers.io](https://docs.ethers.io/v5/single-page/)
- [hardhat](https://hardhat.org/tutorial/testing-contracts.html)
- [security best practices](https://secureum.substack.com/p/security-pitfalls-and-best-practices-101)
- [OMS](https://en.wikipedia.org/wiki/Order_matching_system)
- [Dragon Glass](https://testnet.dragonglass.me/hedera/home)
