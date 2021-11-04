const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("SwapVerse Limit Orders Tests A", function () {
  let _owner;
  let _addr1;
  let _addr2;
  let _swap_verse;

  let _hbar_token;
  let _usdt_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _owner = owner;
    _addr1 = addr1;
    _addr2 = addr2;

    const TJToken = await ethers.getContractFactory("TJToken");
    _hbar_token = await TJToken.deploy(2000, "Hbar", "HBAR");
    await _hbar_token.deployed();
    console.log("HBAR address:", _hbar_token.address);

    _usdt_token = await TJToken.deploy(2000, "USDt", "USDT");
    await _usdt_token.deployed();
    console.log("USDT address:", _usdt_token.address);

    const SwapVerse = await ethers.getContractFactory("SwapVerse");
    _swap_verse = await SwapVerse.deploy(_hbar_token.address, _usdt_token.address);
    await _swap_verse.deployed();

    await _swap_verse.add_participant(_addr1.address);
    await _swap_verse.add_participant(_addr2.address);
  });

  it("Add One Buy Order", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 100, 15)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 1, true, 100, 15]);
      _swap_verse.print_clob();
      best_buy = await _swap_verse.buy_prices();
      expect(best_buy[0]).to.eq(15);
  });

  it("Add Second Buy Order", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 200, 14)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 2, true, 200, 14]);
      _swap_verse.print_clob();
      best_buy = await _swap_verse.buy_prices();
      expect(best_buy[0]).to.eq(15);
      expect(best_buy[1]).to.eq(14);
  });

  it("Add First Sell Order", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(false, 50, 16)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 3, false, 50, 16]);
      _swap_verse.print_clob();
      best_sell = await _swap_verse.sell_prices();
      expect(best_sell[0]).to.eq(16);
  });

  it("Add First Full Match", async function () {
    await expect(_swap_verse.connect(_addr2).place_limit_order(false, 120, 15)).
      to.emit(_swap_verse, 'PlacingOrderEvent').
      withArgs([_addr2.address, 4, false, 120, 15]);
      _swap_verse.print_clob();
  });
});

describe("SwapVerse Limit Orders Tests B", function () {
  let _owner;
  let _addr1;
  let _addr2;
  let _swap_verse;

  let _hbar_token;
  let _usdt_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _owner = owner;
    _addr1 = addr1;
    _addr2 = addr2;

    const TJToken = await ethers.getContractFactory("TJToken");
    _hbar_token = await TJToken.deploy(2000, "Hbar", "HBAR");
    await _hbar_token.deployed();
    console.log("HBAR address:", _hbar_token.address);

    _usdt_token = await TJToken.deploy(2000, "USDt", "USDT");
    await _usdt_token.deployed();
    console.log("USDT address:", _usdt_token.address);

    const SwapVerse = await ethers.getContractFactory("SwapVerse");
    _swap_verse = await SwapVerse.deploy(_hbar_token.address, _usdt_token.address);
    await _swap_verse.deployed();

    await _swap_verse.add_participant(_addr1.address);
    await _swap_verse.add_participant(_addr2.address);
  });

  it("Full Trade Test 1", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 100, 15)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 1, true, 100, 15]);

      await expect(_swap_verse.connect(_addr2).place_limit_order(false, 100, 15)).
      to.emit(_swap_verse, 'TradeEvent').
      withArgs(
        [_addr1.address, 1, true, 100, 15],
        [_addr2.address, 2, false, 100, 15]);

      _swap_verse.print_clob();
  });

  it("Full Trade Swipe All", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 200, 10)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 3, true, 200, 10]);

    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 100, 11)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 4, true, 100, 11]);

    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 50, 12)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 5, true, 50, 12]);

    _swap_verse.print_clob();

    await expect(_swap_verse.connect(_addr2).place_limit_order(false, 350, 12)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr2.address, 6, false, 300, 12]);
    
    _swap_verse.print_clob();
  });
});