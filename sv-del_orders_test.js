const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("SwapVerse Delete Orders Tests", function () {
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

  it("Add & Delete One Order", async function () {
    await expect(_swap_verse.connect(_addr1).place_limit_order(true, 130, 17)).
      to.emit(_swap_verse, 'PlacedOrderEvent').
      withArgs([_addr1.address, 1, true, 130, 17]);

    await _swap_verse.print_clob();

    await expect(_swap_verse.connect(_addr1).delete_order(1)).
      to.emit(_swap_verse, 'DeletedOrderEvent').
      withArgs(1);

    await _swap_verse.print_clob();
  });
});
