const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("SwapVerse Market Orders Tests A", function () {
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
    _hbar_token = await TJToken.deploy(200000, "Hbar", "HBAR");
    await _hbar_token.deployed();
    console.log("HBAR address:", _hbar_token.address);

    _usdt_token = await TJToken.deploy(300000, "USDt", "USDT");
    await _usdt_token.deployed();
    console.log("USDT address:", _usdt_token.address);

    const SwapVerse = await ethers.getContractFactory("SwapVerse");
    _swap_verse = await SwapVerse.deploy(_hbar_token.address, _usdt_token.address);
    await _swap_verse.deployed();

    await _swap_verse.add_participant(_addr1.address);
    await _swap_verse.add_participant(_addr2.address);

    await _hbar_token.transfer(_addr1.address, 50000);
    expect(await _hbar_token.balanceOf(_addr1.address)).to.equal(50000);
    await _hbar_token.connect(_addr1).approve(_swap_verse.address, 40000);

    await _usdt_token.transfer(_addr2.address, 40000);
    expect(await _usdt_token.balanceOf(_addr2.address)).to.equal(40000);
    await _usdt_token.connect(_addr2).approve(_swap_verse.address, 30000);
  });

  it("Add One Buy Order / One Market Order Swipe All", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 2000, 12)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 1, true, 2000, 12]);
    await _swap_verse.print_clob();

    await expect( _swap_verse.connect(_addr2).place_market_order(false, 2000)).
      to.emit(_swap_verse, 'TradeEvent').
      withArgs(
        [_addr1.address, 1, true, 2000, 12],
        [_addr2.address, 2, false, 2000, 0]);

    await _swap_verse.print_clob();
  });

  it("Add One Buy Order / One Market Order Swipe 2/3rd", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 3000, 17)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 3, true, 3000, 17]);
    await _swap_verse.print_clob();

    await expect( _swap_verse.connect(_addr2).place_market_order(false, 2000)).
      to.emit(_swap_verse, 'TradeEvent').
      withArgs(
        [_addr1.address, 3, true, 2000, 17],
        [_addr2.address, 4, false, 2000, 0]);

    await _swap_verse.print_clob();

    const order = await _swap_verse.get_order(3);
    expect(order.size).to.eq(1000);
  });
});
