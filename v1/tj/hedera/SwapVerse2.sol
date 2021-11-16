//SPDX-License-Identifier: Unlicense
pragma solidity ^0.8.4;
pragma experimental ABIEncoderV2;

/**
  _____                __      __                
  / ____|               \ \    / /                
 | (_____      ____ _ _ _\ \  / /__ _ __ ___  ___ 
  \___ \ \ /\ / / _` | '_ \ \/ / _ \ '__/ __|/ _ \
  ____) \ V  V / (_| | |_) \  /  __/ |  \__ \  __/
 |_____/ \_/\_/ \__,_| .__/ \/ \___|_|  |___/\___|
                     | |                          
                     |_|     

            tjdragonhash@gmail.com                     
 */

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "hardhat/console.sol";

struct Order {
    uint256 id_int; // Internal Order Id
    string id_ext; // External Order Id
    address owner; // Owner
    bool is_buy; // Is this a Buy or Sell 
    uint256 size; // Size
    uint256 price; // Price (0 for Market Orders)
}

contract SwapVerse2 {
    // List of events supported
    event OrderPlacedEvent(Order order);
    event OrderUpdatedEvent(Order order);
    event OrderDeletedEvent(Order order);
    event MatchedOrderSameOwnerStoppedEvent(string order, string matched_order);
    event TradeEvent(Order order, Order matched_order);
    event SettlementInstruction(address from, address to, address token1, uint256 amount1, address token2, uint256 amount2);

    // Owner of this smart contract
    address private _owner;
    // List of external order ids
    mapping(string => bool) private _ext_order_ids;
    // Internal order id
    uint256 private _order_id = 1;
    // List of orders placed
    mapping(uint256 => Order) private _orders;
    mapping(uint256 => string) private _order_id_int_ext_mapping;
    mapping(string => uint256) private _order_id_ext_int_mapping;
    // Mapping of Price to Array of order ids
    mapping(uint256 => uint256[]) private _buy_list;
    // Sorted on insert list of prices: 12, 10, 8
    uint256[] private _buy_prices;
     // Mapping of Price to Array of order ids
    mapping(uint256 => uint256[]) private _sell_list;
    // Sorted on insert list of prices: 13, 15, 18
    uint256[] private _sell_prices;

    // Utility function to apply to relevan operations
    modifier onlyOwner {
      require(msg.sender == _owner);
      _;
    }

    address _token_address_1;
    IERC20 _token_1;
    address _token_address_2;
    IERC20 _token_2;

    // Called once at contract creation on-chain
    constructor(address token_address_1, address token_address_2) {
        _owner = msg.sender;

        _token_address_1 = token_address_1;
        _token_1 = IERC20(_token_address_1);
        _token_address_2 = token_address_2;
        _token_2 = IERC20(_token_address_2);

        console.log("Created SwapVerse2 with tokens %s and %s", token_address_1, token_address_2);
    }

    /**
     * Places a limit order in the order book
     */
    function place_limit_order(
        string memory ext_order_id, 
        bool is_buy, 
        uint256 amount, 
        uint256 price) public {
        validate_limit_order(ext_order_id, is_buy, amount, price);

        // Retrieves the list of prices to internal order ids
        mapping(uint256 => uint256[]) storage opp_list = list_by_verb(!is_buy);

        Order memory order = create_order(msg.sender, ext_order_id, is_buy, amount, price);

        if (opp_list[price].length == 0 || best_price(!is_buy) != price) { 
            // No opposite order at that price, we can just place it or
            // There are opposite orders but not at that price
            // Crosses are handled by the validate method
            console.log("[SV2] No opposite order and/or at that pice, placing order");
            store_order(order);
            emit OrderPlacedEvent(order);
            return;
        }

        // We are in a match scenario
        uint256[] storage matched_orders_ids = opp_list[price];
        uint256[] memory moids =  new uint256[](matched_orders_ids.length);
        for(uint i = 0; i < matched_orders_ids.length; i++) {
            moids[i] = matched_orders_ids[i];
        }

        console.log("[SV2] Match scenario. Nb matched orders: %s", moids.length);
        uint256 size_left = amount;
        for(uint i = 0; i < moids.length && size_left > 0; i++) {
            Order storage matched_order = _orders[moids[i]];
            console.log("[SV2] Processing matched order %s", matched_order.id_ext);
            if (msg.sender == matched_order.owner) {
                emit MatchedOrderSameOwnerStoppedEvent(ext_order_id, matched_order.id_ext);
                return;
            }

            emit TradeEvent(order, matched_order);
            console.log("[SV2] Order %s <> Matched Order %s", order.id_ext, matched_order.id_ext);
            if (size_left >= matched_order.size) {
                console.log("[SV2] %s >= %s", size_left,  matched_order.size);
                remove_order(matched_order.id_int, matched_order.is_buy);
                size_left = size_left - matched_order.size;
                console.log("[SV2] new size left %s", size_left);
            } else {
                console.log("[SV2] size_left = 0");
                size_left;
                Order memory updated_order = Order(matched_order.id_int, matched_order.id_ext, matched_order.owner, matched_order.is_buy, matched_order.size - size_left, matched_order.price);
                _orders[matched_order.id_int] = updated_order;
                emit OrderUpdatedEvent(updated_order);
            }
            process_trade(matched_order.owner, order.owner, is_buy, matched_order.size, price);
        } // For all matched orders

        if (size_left > 0) {
            console.log("[SV2] size_left > 0. Creating and storing new order");
            Order memory new_order = create_order(msg.sender, ext_order_id, is_buy, size_left, price);
            store_order(new_order);
            emit OrderPlacedEvent(new_order);
        }

        clean_up();
    } // function place_limit_order

    function process_trade(address matched_owner, address order_owner, bool is_buy, uint256 size, uint256 price) private {
        console.log("[SV2] process_trade %s %s size = %s", matched_owner, order_owner, size);

        uint256 amount_to_transfer = size * price;
        if (is_buy) {
            console.log("[SV2] BO %s transfers %s token_1 to %s", matched_owner, size, order_owner);
            console.log("[SV2] BO %s transfers %s token_2 to %s", order_owner, amount_to_transfer, matched_owner);
            emit SettlementInstruction(matched_owner, order_owner, _token_address_1, size, _token_address_2, amount_to_transfer);
        } else {
            console.log("[SV2] SO %s transfers %s token_2 to %s", matched_owner, amount_to_transfer, order_owner);
            console.log("[SV2] SO %s transfers %s token_1 to %s", order_owner, size, matched_owner);
            emit SettlementInstruction(matched_owner, order_owner, _token_address_2, amount_to_transfer, _token_address_1, size);
        }
    }

    function create_order(address owner, string memory ext_order_id, bool is_buy, uint256 amount, uint256 price) private returns (Order memory) {
        console.log("[SV2] create_order %s %s", _order_id, ext_order_id);
        Order memory order = Order(_order_id, ext_order_id, owner, is_buy, amount, price);
        _order_id = _order_id + 1;
        return order;
    }

    function store_order(Order memory order) private {
        console.log("[SV2] store_order %s %s", order.id_int, order.id_ext);
        _order_id_int_ext_mapping[_order_id] = order.id_ext;
        _order_id_ext_int_mapping[ order.id_ext] = _order_id;
        _orders[order.id_int] = order;

        add_price(order.price, order.is_buy);
        mapping(uint256 => uint256[]) storage list = list_by_verb(order.is_buy);
        list[order.price].push(order.id_int);
    }

    function delete_order(string memory ext_order_id) public {
        require(_ext_order_ids[ext_order_id], "No such order id");
        uint256 int_order_id = _order_id_ext_int_mapping[ext_order_id];
        Order storage order = _orders[int_order_id];
        require(msg.sender == order.owner, "Must own the order to delete it");

        bool found = false;
        for(uint i = 0; i < _buy_prices.length; i++) {
            uint256 buy_price = _buy_prices[i];
            uint256[] storage buy_list = _buy_list[buy_price];
           
            for(uint j = 0; j < buy_list.length; j++) {
                if (buy_list[j] == int_order_id) {
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
            delete _orders[int_order_id];
            emit OrderDeletedEvent(order);
            return;
        }

        found = false;
        for(uint i = 0; i < _sell_prices.length; i++) {
            uint256 sell_price = _sell_prices[i];
            uint256[] storage sell_list = _buy_list[sell_price];
           
            for(uint j = 0; j < sell_list.length; j++) {
                if (sell_list[j] == int_order_id) {
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
            delete _orders[int_order_id];
            emit OrderDeletedEvent(order);
            return;
        }
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

    function list_by_verb(bool is_buy) private view returns (mapping(uint256 => uint256[]) storage) {
        if (is_buy) {
            return _buy_list;
        } else {
            return _sell_list;
        }
    }

    function validate_limit_order(string memory order_id, bool is_buy, uint256 amount, uint256 price) private view {
        require(amount > 0, "Amount must be > 0");
        require(price > 0, "Price must be > 0");
        require(!_ext_order_ids[order_id], "Order id already exists");

        uint256 best_opp_proce = best_price(!is_buy);
        if (best_opp_proce == type(uint256).min) {
            return;
        }
        if (is_buy) {
             require(price <= best_opp_proce, "Crossed buy price > best sell price");
        } else {
            require(price >= best_opp_proce, "Crossed sell price < best buy price");
        }
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

    function prices_by_verb(bool is_buy) private view returns (uint256[] storage) {
        if (is_buy) {
            return _buy_prices;
        } else {
            return _sell_prices;
        }
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

    function remove_order(uint256 order_id, bool is_buy) private {
        console.log("[SV2] remove_order %s %s", order_id, is_buy);
        if (is_buy) {
            remove_buy_order(order_id);
        } else {
            remove_sell_order(order_id);
        }
    }

    function remove_buy_order(uint256 order_id) private {
        console.log("[SV2] remove_buy_order %s", order_id);
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
    }

    function remove_sell_order(uint256 order_id) private {
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
                    console.log("    Order %s %s @ %s", lo.is_buy, lo.size, lo.price);
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
                    console.log("    Order %s %s @ %s", lo.is_buy, lo.size, lo.price);
                }
            }
        }
        console.log("> END ---- <");
    }
}