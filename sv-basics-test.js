const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("SwapVerse Basic Tests", function () {
  let _owner;
  let _addr1;
  let _swap_verse;

  let _hbar_token;
  let _usdt_token;

  before(async () => {
    const [owner, addr1] = await ethers.getSigners();
    _owner = owner;
    _addr1 = addr1;

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
  });

  it("Start / Stop Trading", async function () {
    await expect(_swap_verse.allow_trading()).to.emit(_swap_verse, 'TradingAllowedEvent');
    await expect(_swap_verse.stop_trading()).to.emit(_swap_verse, 'TradingStoppedEvent');
  });

  it("Test Add / Remove Participant", async function () {
    await expect(_swap_verse.add_participant(_addr1.address)).to.
      emit(_swap_verse, 'ParticipantAddedEvent').
      withArgs(_addr1.address);
    await _swap_verse.connect(_addr1).am_i_allowed();
    await expect(_swap_verse.remove_participant(_addr1.address)).to.
      emit(_swap_verse, 'ParticipantRemovedEvent').
      withArgs(_addr1.address);
      await _swap_verse.add_participant(_addr1.address);
  });

  it("Order Id should be successfully returned", async function () {
    const order_id = await _swap_verse.current_order_id();
    expect(order_id).to.equal(1);
  });

  it("Test Print Clob", async function () {
    _swap_verse.print_clob();
  });
});
