const { expect } = require("chai");
const { ethers } = require("hardhat");

const hbar_token_supply = 10000000;
const btc_token_supply  = 10000000;
const token_transfer_amnt = 3000000;
const token_approved_supply = 2500000;

describe("SwapVerse II Limit Orders Tests 1", function () {
  let _owner;
  let _addr1;
  let _addr2;
  let _swap_verse_2;

  let _hbar_token;
  let _btc_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _owner = owner;
    _addr1 = addr1;
    _addr2 = addr2;

    const TJToken = await ethers.getContractFactory("TJToken");
    _hbar_token = await TJToken.deploy(hbar_token_supply, "Hbar", "HBAR");
    await _hbar_token.deployed();
    _btc_token = await TJToken.deploy(btc_token_supply, "Btc", "BTC");
    await _btc_token.deployed();

    const SwapVerse2 = await ethers.getContractFactory("SwapVerse2");
    _swap_verse_2 = await SwapVerse2.deploy(_hbar_token.address, _btc_token.address);
    await _swap_verse_2.deployed();

    await _hbar_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);

    await _hbar_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);
  });

  it("Add One Buy Order", async function () {
    const order_id = "tj-uid-1";
    await expect(_swap_verse_2.connect(_addr1).place_limit_order(order_id, true, 200, 650)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([1, order_id, _addr1.address, true, 200, 650]);
      await _swap_verse_2.print_clob();
  });

  it("Add One Sell Order", async function () {
    const order_id = "tj-uid-2";
    await expect(_swap_verse_2.connect(_addr2).place_limit_order(order_id, false, 300, 800)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([2, order_id, _addr2.address, false, 300, 800]);
      await _swap_verse_2.print_clob();
  });

  it("Add One Trade Full Sell", async function () {
    const order_id = "tj-uid-3";
    await expect(_swap_verse_2.connect(_addr2).place_limit_order(order_id, false, 200, 650)).
      to.emit(_swap_verse_2, 'TradeEvent').
      withArgs(
        [3, "tj-uid-3", _addr2.address, false, 200, 650],
        [1, "tj-uid-1", _addr1.address, true, 200, 650]
      );
      await _swap_verse_2.print_clob();
  });

  it("Add One Trade Partial Buy", async function () {
    const order_id = "tj-uid-4";
    await expect(_swap_verse_2.connect(_addr1).place_limit_order(order_id, true, 200, 800)).
      to.emit(_swap_verse_2, 'TradeEvent').
      withArgs(
        [4, "tj-uid-4", _addr1.address, true, 200, 800],
        [2, "tj-uid-2", _addr2.address, false, 300, 800]
      );
      await _swap_verse_2.print_clob();
  });

  it("Add One Trade Full Buy", async function () {
    await _swap_verse_2.print_clob();
    const order_id = "tj-uid-5";
    await expect(_swap_verse_2.connect(_addr1).place_limit_order(order_id, true, 100, 800)).
      to.emit(_swap_verse_2, 'TradeEvent').
      withArgs(
        [5, "tj-uid-5", _addr1.address, true, 100, 800],
        [2, "tj-uid-2", _addr2.address, false, 100, 800]
      );
      await _swap_verse_2.print_clob();
  });
});

describe("SwapVerse II Limit Orders Tests 2", function () {
  let _owner;
  let _addr1;
  let _addr2;
  let _swap_verse_2;

  let _hbar_token;
  let _btc_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _owner = owner;
    _addr1 = addr1;
    _addr2 = addr2;

    const TJToken = await ethers.getContractFactory("TJToken");
    _hbar_token = await TJToken.deploy(hbar_token_supply, "Hbar", "HBAR");
    await _hbar_token.deployed();
    _btc_token = await TJToken.deploy(btc_token_supply, "Btc", "BTC");
    await _btc_token.deployed();

    const SwapVerse2 = await ethers.getContractFactory("SwapVerse2");
    _swap_verse_2 = await SwapVerse2.deploy(_hbar_token.address, _btc_token.address);
    await _swap_verse_2.deployed();

    await _hbar_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);

    await _hbar_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);
  });

  it("Full Trade on two orders at same price", async function () {
    await expect(_swap_verse_2.connect(_addr1).place_limit_order("tj-uid-1", true, 200, 650)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([1, "tj-uid-1", _addr1.address, true, 200, 650]);
      await _swap_verse_2.print_clob();

    await expect(_swap_verse_2.connect(_addr1).place_limit_order("tj-uid-2", true, 300, 650)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([2, "tj-uid-2", _addr1.address, true, 300, 650]);
      await _swap_verse_2.print_clob();

    console.log("Creating matching order...");
    await _swap_verse_2.connect(_addr2).place_limit_order("tj-uid-3", false, 500, 650)
    await _swap_verse_2.print_clob();
  });
});

describe("SwapVerse II Limit Orders Tests 3", function () {
  let _owner;
  let _addr1;
  let _addr2;
  let _swap_verse_2;

  let _hbar_token;
  let _btc_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _owner = owner;
    _addr1 = addr1;
    _addr2 = addr2;

    const TJToken = await ethers.getContractFactory("TJToken");
    _hbar_token = await TJToken.deploy(hbar_token_supply, "Hbar", "HBAR");
    await _hbar_token.deployed();
    _btc_token = await TJToken.deploy(btc_token_supply, "Btc", "BTC");
    await _btc_token.deployed();

    const SwapVerse2 = await ethers.getContractFactory("SwapVerse2");
    _swap_verse_2 = await SwapVerse2.deploy(_hbar_token.address, _btc_token.address);
    await _swap_verse_2.deployed();

    await _hbar_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);

    await _hbar_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);
  });

  it("Over trade", async function () {
    await expect(_swap_verse_2.connect(_addr1).place_limit_order("tj-uid-1", true, 200, 650)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([1, "tj-uid-1", _addr1.address, true, 200, 650]);
      await _swap_verse_2.print_clob();

    await expect(_swap_verse_2.connect(_addr2).place_limit_order("tj-uid-2", false, 300, 650)).
      to.emit(_swap_verse_2, 'TradeEvent').
      withArgs(
        [2, "tj-uid-2", _addr2.address, false, 300, 650],
        [1, "tj-uid-1", _addr1.address, true, 200, 650]
      );

    await _swap_verse_2.print_clob();
  });
});

describe("SwapVerse II Trade Tests 4", function () {
  let _addr1;
  let _addr2;
  let _swap_verse_2;

  let _hbar_token;
  let _btc_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _addr1 = addr1;
    _addr2 = addr2;

    const TJToken = await ethers.getContractFactory("TJToken");
    _hbar_token = await TJToken.deploy(hbar_token_supply, "Hbar", "HBAR");
    await _hbar_token.deployed();
    _btc_token = await TJToken.deploy(btc_token_supply, "Btc", "BTC");
    await _btc_token.deployed();

    const SwapVerse2 = await ethers.getContractFactory("SwapVerse2");
    _swap_verse_2 = await SwapVerse2.deploy(_hbar_token.address, _btc_token.address);
    await _swap_verse_2.deployed();

    await _hbar_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);

    await _hbar_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);
  });

  it("Simple Trade 1", async function () {
    await expect(_swap_verse_2.connect(_addr1).place_limit_order("tj-uid-1", true, 200, 650)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([1, "tj-uid-1", _addr1.address, true, 200, 650]);
    await _swap_verse_2.print_clob();

    await expect(_swap_verse_2.connect(_addr2).place_limit_order("tj-uid-2", false, 100, 650)).
      to.emit(_swap_verse_2, 'SettlementInstruction').
      withArgs(
        _addr1.address, _addr2.address, _btc_token.address, 100 * 650, _hbar_token.address, 100
      );

    await _swap_verse_2.print_clob();
  });

  it("Test Get Book", async function () {
    out = await _swap_verse_2.buy_prices();
    console.log(out);
    out = await _swap_verse_2.sell_prices();
    console.log(out);
    out =  await _swap_verse_2.buy_order_ids(650);
    console.log(out);
    out =  await _swap_verse_2.sell_order_ids(650);
    console.log(out);
    out =  await _swap_verse_2.get_order(1);
    console.log(out);
  });
});

describe("SwapVerse II Trade Tests 5", function () {
  let _addr1;
  let _addr2;
  let _swap_verse_2;

  let _hbar_token;
  let _btc_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _addr1 = addr1;
    _addr2 = addr2;

    const TJToken = await ethers.getContractFactory("TJToken");
    _hbar_token = await TJToken.deploy(hbar_token_supply, "Hbar", "HBAR");
    await _hbar_token.deployed();
    _btc_token = await TJToken.deploy(btc_token_supply, "Btc", "BTC");
    await _btc_token.deployed();

    const SwapVerse2 = await ethers.getContractFactory("SwapVerse2");
    _swap_verse_2 = await SwapVerse2.deploy(_hbar_token.address, _btc_token.address);
    await _swap_verse_2.deployed();

    await _hbar_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);

    await _hbar_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _hbar_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _hbar_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr2.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr2.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr2).approve(_swap_verse_2.address, token_approved_supply);

    await _btc_token.transfer(_addr1.address, token_transfer_amnt);
    expect(await _btc_token.balanceOf(_addr1.address)).to.equal(token_transfer_amnt);
    await _btc_token.connect(_addr1).approve(_swap_verse_2.address, token_approved_supply);
  });

  it("Simple Trade X", async function () {
    await expect(_swap_verse_2.connect(_addr1).place_limit_order("tj-uid-b", true, 1500, 650)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([1, "tj-uid-b", _addr1.address, true, 1500, 650]);
    await _swap_verse_2.print_clob();

    await expect(_swap_verse_2.connect(_addr2).place_limit_order("tj-uid-s", false, 1500, 650)).
      to.emit(_swap_verse_2, 'SettlementInstruction').
      withArgs(
        _addr1.address, _addr2.address, _btc_token.address, 1500 * 650, _hbar_token.address, 1500
      );

    await _swap_verse_2.print_clob();
  });

  it("Simple Trade Y", async function () {
    adr1_hbar = await _hbar_token.balanceOf(_addr1.address);
    adr2_hbar = await _hbar_token.balanceOf(_addr2.address);
    adr1_btc = await _btc_token.balanceOf(_addr1.address);
    adr2_btc = await _btc_token.balanceOf(_addr2.address);
    console.log("adr1_hbar = ", adr1_hbar.toString())
    console.log("adr2_hbar = ", adr2_hbar.toString())
    console.log("adr1_btc = ", adr1_btc.toString())
    console.log("adr2_btc = ", adr2_btc.toString())

    await expect(_swap_verse_2.connect(_addr1).place_limit_order("tj-uid-c", true, 1500, 650)).
      to.emit(_swap_verse_2, 'OrderPlacedEvent').
      withArgs([3, "tj-uid-c", _addr1.address, true, 1500, 650]);
    await _swap_verse_2.print_clob();

    await expect(_swap_verse_2.connect(_addr2).place_limit_order("tj-uid-d", false, 500, 650)).
      to.emit(_swap_verse_2, 'OrderUpdatedEvent').
      withArgs([3, "tj-uid-c", _addr1.address, true, 1000, 650]);

    await _swap_verse_2.print_clob();

    adr1_hbar = await _hbar_token.balanceOf(_addr1.address);
    adr2_hbar = await _hbar_token.balanceOf(_addr2.address);
    adr1_btc = await _btc_token.balanceOf(_addr1.address);
    adr2_btc = await _btc_token.balanceOf(_addr2.address);
    console.log("adr1_hbar = ", adr1_hbar.toString())
    console.log("adr2_hbar = ", adr2_hbar.toString())
    console.log("adr1_btc = ", adr1_btc.toString())
    console.log("adr2_btc = ", adr2_btc.toString())
  });
});