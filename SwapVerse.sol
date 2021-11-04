//SPDX-License-Identifier: Unlicense
pragma solidity ^0.8.4;
pragma experimental ABIEncoderV2;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "hardhat/console.sol";

/**
 * First pass at a Central Limit Order Book to run on Ethereum or Hedera.
 * Investigating DEX, CLOB for some DeFi projects - Learning Solidity in the process.
 * Implementation of this PoC is clearly sub-optimal, but this is not the point right now.
 * Lots of bugs I guess as well (need to handle deletes better, add more tests, etc)
 * This conceptually is for a given pair.
 *
 * The most important aspect of a Dex is the settlement. Some options:
 * (1) Institutions are still owning the amount and are relying on events to settle
 * (2) Institutions delegate to the Dex to settle (approve & allowance)
 * (3) Full amounts are sent to the Dex (OBO)
 * 
 * Options (1) and (3) are straightforward, will focus on (2) to learn
 *
 * https://docs.ethers.io/v5/single-page/
 * https://hardhat.org/tutorial/testing-contracts.html
 * https://secureum.substack.com/p/security-pitfalls-and-best-practices-101
 * https://en.wikipedia.org/wiki/Order_matching_system
 *
 * @author TJ
 */

struct Order {
    address owner; // Owner
    uint256 id; // Order Id
    bool is_buy; // Is this a Buy or Sell 
    uint256 size; // Size
    uint256 price; // Price (Can be null for Market Orders)
}

contract SwapVerse {
    // List of events generated by this contract
    event TradingStoppedEvent();
    event TradingAllowedEvent();
    event ParticipantAddedEvent(address adr);
    event ParticipantRemovedEvent(address adr);
    event PlacingOrderEvent(Order order);
    event PlacedOrderEvent(Order order);
    event DeletedOrderEvent(uint oid);
    event TradeEvent(Order leg1, Order leg2);
    event AssetReceived(address from, uint256 amount);

    // Who owns this smart contract
    address private _owner;
    // Is trading allowed?
    bool private _trading_allowed = true;
    // Allowed participants
    mapping(address => bool) private _participants;
    // Current order id
    uint256 private _order_id = 1;
    // List of orders placed
    mapping(uint256 => Order) private _orders;
    // Mapping of Price to Array of order ids
    mapping(uint256 => uint256[]) private _buy_list;
    // Sorted on insert list of prices: 12, 10, 8
    uint256[] private _buy_prices;
     // Mapping of Price to Array of order ids
    mapping(uint256 => uint256[]) private _sell_list;
    // Sorted on insert list of prices: 13, 15, 18
    uint256[] private _sell_prices;
    // Array used by the place market orders to keep track of matched order ids
    uint256[] matchedOrderIds;
    
    // Utility function
    modifier onlyOwner {
      require(msg.sender == _owner);
      _;
    }

    modifier tradingAllowed {
        require(_trading_allowed, "Trading has been disabled");
        require(_participants[msg.sender], "Participant is not allowed");
        _;
    }

    // Crypto Token that is traded _token_address_1/_token_address_2
    // Like HBAR/USDT
    address _token_address_1;
    IERC20 _token_1;
    address _token_address_2;
    IERC20 _token_2;

    constructor(address token_address_1, address token_address_2) {
        _owner = msg.sender;
        _token_address_1 = token_address_1;
        _token_1 = IERC20(_token_address_1);
        _token_address_2 = token_address_2;
        _token_2 = IERC20(_token_address_2);
    }

    function get_order(uint256 order_id) public view returns (Order memory) {
        return _orders[order_id];
    }

    function clean_up() private {
       for(uint i = 0; i < _buy_prices.length; i++) {
           uint256 buy_price = _buy_prices[i];
           uint256[] storage buy_list = _buy_list[buy_price];
           if (buy_list.length == 0) {
               delete _buy_list[buy_price];
               delete _buy_prices[i];
           }
       }
       for(uint i = 0; i < _sell_prices.length; i++) {
           uint256 sell_price = _sell_prices[i];
           uint256[] storage sell_list = _sell_list[sell_price];
           if (sell_list.length == 0) {
               delete _sell_list[sell_price];
               delete _sell_prices[i];
           }
       }
   }

    receive() external payable {
        // Method to receive native ethers
        require(_participants[msg.sender], "Participant is not allowed");
        emit AssetReceived(msg.sender, msg.value);
        console.log("Received %s from %s", msg.value, msg.sender);
    }

    function stop_trading() public onlyOwner {
        _trading_allowed = false;
        emit TradingStoppedEvent();
    }

    function allow_trading() public onlyOwner {
        _trading_allowed = true;
        emit TradingAllowedEvent();
    }

    function am_i_allowed() public view {
        require(_participants[msg.sender], "Participant is not allowed");
    }

    function add_participant(address participant) public onlyOwner {
        _participants[participant] = true;
        emit ParticipantAddedEvent(participant);
    }

    function remove_participant(address participant) public onlyOwner {
        delete _participants[participant];
        emit ParticipantRemovedEvent(participant);
    }

    function current_order_id() public view returns (uint256) {
        return _order_id;
    }

    function prices_by_verb(bool is_buy) private view returns (uint256[] storage) {
        if (is_buy) {
            return _buy_prices;
        } else {
            return _sell_prices;
        }
    }

    function list_by_verb(bool is_buy) private view returns (mapping(uint256 => uint256[]) storage) {
        if (is_buy) {
            return _buy_list;
        } else {
            return _sell_list;
        }
    }

    // Will be remove for "production"
    function test_add_price(uint256 price, bool is_buy) public { 
        return add_price(price, is_buy);
    }

    // Adds price - sort on insert, increasing for sell, decreasing for buy
    function add_price(uint256 price, bool is_buy) private { 
        uint256[] storage prices = prices_by_verb(is_buy);
        uint index = type(uint).min;
        bool found = false;

        for(uint i = 0; i < prices.length; i++) {
            if (price == prices[i]) {
                return;
            }
            if (!is_buy) {
                if (price < prices[i]) {
                    index = i;
                    found = true;
                    break;
                }
            } else {
                if (price > prices[i]) {
                    index = i;
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            prices.push(price);
        } else {
            uint256[] memory _new_array = new uint[](prices.length + 1);
            for(uint i = 0; i < index; i++) {
                _new_array[i] = prices[i];
            }
            _new_array[index] = price;
            for(uint i = index + 1; i <= prices.length; i++) {
                 _new_array[i] = prices[i - 1];
            }
            if (is_buy) {
                _buy_prices = _new_array;
            } else {
                _sell_prices = _new_array;
            }
        }
    }

    function buy_prices() public view returns (uint256[] memory)  {
        return _buy_prices;
    }


    function sell_prices() public view returns (uint256[] memory)  {
        return _sell_prices;
    }

    function buy_orders(uint256 price) public view returns (Order[] memory) {
        uint256[] storage order_ids = _buy_list[price];
        Order[] memory orders = new Order[](order_ids.length);

        for(uint j = 0; j < order_ids.length; j++) {
            orders[j] = _orders[order_ids[j]];
        }

        return orders;
    }

    function sell_orders(uint256 price) public view returns (Order[] memory) {
        uint256[] storage order_ids = _sell_list[price];
        Order[] memory orders = new Order[](order_ids.length);

        for(uint j = 0; j < order_ids.length; j++) {
            orders[j] = _orders[order_ids[j]];
        }

        return orders;
    }

    function print_clob() public view {
        console.log("> BEGIN CLOB <");
        console.log(" Buy Orders");
        for(uint i = 0; i < _buy_prices.length; i++) {
            uint256[] storage order_ids = _buy_list[_buy_prices[i]];
            if (order_ids.length > 0)
                console.log("  For price %s", _buy_prices[i]);
            for(uint j = 0; j < order_ids.length; j++) {
                Order storage lo = _orders[order_ids[j]];
                if (lo.price > 0 && lo.size > 0) {
                    console.log("    Order %s %s @ %s", lo.is_buy, lo.price, lo.size);
                }
            }
        }
        console.log(" Sell Orders");
        for(uint i = 0; i < _sell_prices.length; i++) {
            uint256[] storage order_ids = _sell_list[_sell_prices[i]];
            if (order_ids.length > 0)
                console.log("  For price %s", _sell_prices[i]);
            for(uint j = 0; j < order_ids.length; j++) {
                Order storage lo = _orders[order_ids[j]];
                if (lo.price > 0 && lo.size > 0) {
                    console.log("    Order %s %s @ %s", lo.is_buy, lo.price, lo.size);
                }
            }
        }
        console.log("> END ---- <");
    }

    function best_price(bool is_buy) private view returns (uint256) {
        uint256 bp = type(uint256).min;
        if (is_buy && _buy_prices.length > 0) {
            bp = _buy_prices[0];
        } else if (!is_buy && _sell_prices.length > 0) {
            bp = _sell_prices[0];
        }
        return bp;
    }

    function validate_limit_order(uint256 price, uint256 size, bool is_buy) private view {
        require(price > 0, "Price must be > 0");
        require(size > 0, "Size must be > 0");

        uint256 bestOtherPrice = best_price(!is_buy);
        if (bestOtherPrice == type(uint256).min) {
            return;
        }
        console.log("Validate Limit Order Buy? %s, Price %s, Best Other Price %s", is_buy, price, bestOtherPrice);
        if (is_buy) {
             require(price <= bestOtherPrice, "Crossed buy price > best sell price");
        } else {
            require(price >= bestOtherPrice, "Crossed sell price < best buy price");
        }
    }

    function delete_order(uint256 order_id) public tradingAllowed returns (bool) {
        require(_orders[order_id].owner == msg.sender, "Can only delete your own orders");
        console.log("delete_order %s", order_id);

        bool found = false;
        for(uint i = 0; i < _buy_prices.length; i++) {
            uint256 buy_price = _buy_prices[i];
            uint256[] storage buy_list = _buy_list[buy_price];
           
            for(uint j = 0; j < buy_list.length; j++) {
                if (buy_list[j] == order_id) {
                    delete _buy_list[buy_price][j];
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }

        if (found) {
            delete _orders[order_id];
            emit DeletedOrderEvent(order_id);
        }

        found = false;
        for(uint i = 0; i < _sell_prices.length; i++) {
            uint256 sell_price = _sell_prices[i];
            uint256[] storage sell_list = _buy_list[sell_price];
           
            for(uint j = 0; j < sell_list.length; j++) {
                if (sell_list[j] == order_id) {
                    delete _sell_list[sell_price][j];
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }

        if (found) {
            delete _orders[order_id];
            emit DeletedOrderEvent(order_id);
        }

        clean_up();

        return found;
    }

    function place_market_order(bool is_buy, uint256 size) public tradingAllowed returns (uint256) {
        console.log("place_market_order %s %s", is_buy, size);
        delete matchedOrderIds;
        
        uint256[] storage otherPrices = prices_by_verb(!is_buy);
        mapping(uint256 => uint256[]) storage otherList = list_by_verb(!is_buy);

        uint256 cumulativeOrderSize = type(uint256).min;

        for(uint i = 0; i < otherPrices.length; i++) {
            uint256[] storage otherOrderIds = otherList[otherPrices[i]];
            bool done = false;
            for(uint j = 0; j < otherOrderIds.length; j++) {
                Order storage otherOrder = _orders[otherOrderIds[j]];
                cumulativeOrderSize = cumulativeOrderSize + otherOrder.size;
                matchedOrderIds.push(otherOrderIds[j]);
                if (cumulativeOrderSize >= size) {
                    done = true;
                    break;
                }
            }
            if (done) {
                break;
            }
        }

        require(matchedOrderIds.length > 0, "No matching order found");

        uint256 oid = _order_id;
        Order memory marketOrder = Order(msg.sender, oid, is_buy, size, 0);
        _orders[oid] = marketOrder;
        _order_id = _order_id + 1;

        process_matched_order(oid, matchedOrderIds, is_buy);

        return oid; 
    }
    
    function place_limit_order(bool is_buy, uint256 size, uint256 price) public tradingAllowed returns (uint256) {
        console.log("place_limit_order %s %s @ %s", is_buy, size, price);
        validate_limit_order(price, size, is_buy);

        uint256 oid = _order_id;
        place_limit_order(oid, is_buy, size, price);
        _order_id = _order_id + 1;

        clean_up();

        return oid;
    }

    function place_limit_order(uint oid, bool is_buy, uint256 size, uint256 price) private {
        Order memory limitOrder = Order(msg.sender, oid, is_buy, size, price);
        _orders[oid] = limitOrder;

        emit PlacingOrderEvent(limitOrder);

        internal_place_limit_order(oid, is_buy);
    }

    function diff_address(address a1, address a2) private pure returns(bool) {
        return a1 != a2;
    }

    function internal_place_limit_order(uint256 orderId, bool is_buy) private {
        Order storage limitOrder = _orders[orderId];

        mapping(uint256 => uint256[]) storage list = list_by_verb(is_buy);
        mapping(uint256 => uint256[]) storage otherList = list_by_verb(!is_buy);

        uint256 limitOrderPrice = limitOrder.price;
        uint nbOppOrders = otherList[limitOrderPrice].length;
        uint256 bestOtherPrice = best_price(!is_buy);

        if (nbOppOrders == 0) {
            console.log("PSL : NO OPPOSITE ORDER -> PLACE ORDER");
            add_price(limitOrderPrice, is_buy);
            list[limitOrderPrice].push(orderId);
            emit PlacedOrderEvent(limitOrder);
        } else {
            if (limitOrderPrice != bestOtherPrice) {
                console.log("PSL : OPPOSITE ORDER AT DIFF PRICE -> PLACE ORDER");
                add_price(limitOrderPrice, is_buy);
                list[limitOrderPrice].push(orderId);
                emit PlacedOrderEvent(limitOrder);
            } else {
                console.log("PSL : OPPOSITE ORDER AND SAME PRICE -> MATCH");
                uint256[] storage matched_orders_ids = otherList[limitOrderPrice];
                process_matched_order(orderId, matched_orders_ids, is_buy);
            }
        }
    } //place_limit_order

    function process_matched_order(uint256 orderId, uint256[] storage matched_ids, bool is_buy) private {
        console.log("process_matched_order %s, is_buy %s", orderId, is_buy);

        Order storage order = _orders[orderId];
        uint256 size_left = order.size;

        for(uint i = 0; i < matched_ids.length && size_left > 0; i++) {
            console.log(" processing matched order id %s", matched_ids[i]);
            Order storage matched_order = _orders[matched_ids[i]];

            bool different_owners = diff_address(order.owner, matched_order.owner);
            require(different_owners, "Must be different owners");

            if (size_left >= matched_order.size) { // Order size is >= current matched order
                console.log(" order size %s >= matched order size %s", size_left, matched_order.size);
                Order storage leg1 = matched_order;
                Order memory leg2 = Order(order.owner, order.id, order.is_buy, matched_order.size, order.price);
                process_trade(leg1.owner, leg1.id, leg1.is_buy, leg1.size, leg1.price,
                              leg2.owner, leg2.id, leg2.is_buy, leg2.size, leg2.price);
                remove_order(matched_order.id);
                size_left = size_left - matched_order.size;
            } else {
                console.log(" order size %s < matched order size %s", size_left, matched_order.size);
                Order memory leg1 = Order(matched_order.owner, matched_order.id, matched_order.is_buy, size_left, matched_order.price);
                Order memory leg2 = Order(order.owner, order.id, order.is_buy, size_left, order.price);
                process_trade(leg1.owner, leg1.id, leg1.is_buy, leg1.size, leg1.price,
                              leg2.owner, leg2.id, leg2.is_buy, leg2.size, leg2.price);
                remove_order(matched_order.id);
                size_left = 0;
                place_limit_order(matched_order.id, matched_order.is_buy, matched_order.size - leg1.size, matched_order.price);
            }
        } // for

        if (size_left > 0) {
            console.log(" size left %s > 0", size_left);
            place_limit_order(order.is_buy, size_left, order.price);
        } else {
            console.log(" no size left");
        }
    }  // process_matched_order

    function process_trade(address owner1, uint256 id1, bool is_buy1, uint256 size1, uint256 price1,
                           address owner2, uint256 id2, bool is_buy2, uint256 size2, uint256 price2) private {
        console.log("process_trade %s <-> %s", id1, id2);
        Order memory leg1 = Order(owner1, id1, is_buy1, size1, price1);
        Order memory leg2 = Order(owner2, id2, is_buy2, size2, price2);
        emit TradeEvent(leg1, leg2);

        // There are many ways to settle a trade - this is where settlement risks come in
        // For this investigation - we will assume that approve has been called successfully by the owners

        console.log("_token_1.allowance %s", _token_1.allowance(owner1, address(this)));
        console.log("_token_2.allowance %s", _token_2.allowance(owner2, address(this)));
        // require(_token_1.allowance(owner1, _owner) >= size1, "Not enough funds for settlement for leg1");
        // require(_token_2.allowance(owner2, _owner) >= size2, "Not enough funds for settlement for leg2");

        // _token_1.transferFrom(owner1, owner2, size1);
        // _token_2.transferFrom(owner2, owner1, size2);
    }

    function remove_order(uint256 order_id) private {
        for(uint i = 0; i < _buy_prices.length; i++) {
           uint256 buy_price = _buy_prices[i];
           uint256[] storage buy_list = _buy_list[buy_price];
           bool found = false;
           for(uint j = 0; j < buy_list.length; j++) {
               if (buy_list[j] == order_id) {
                   for(uint k = i; k < buy_list.length - 1; k++) {
                       buy_list[k] = buy_list[k + 1];
                    }
                    buy_list.pop();
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        } // for

        for(uint i = 0; i < _sell_prices.length; i++) {
           uint256 sell_price = _sell_prices[i];
           uint256[] storage sell_list = _sell_list[sell_price];
           bool found = false;
           for(uint j = 0; j < sell_list.length; j++) {
               if (sell_list[j] == order_id) {
                   for(uint k = i; k < sell_list.length - 1; k++) {
                       sell_list[k] = sell_list[k + 1];
                    }
                    sell_list.pop();
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        } // for
    }
}