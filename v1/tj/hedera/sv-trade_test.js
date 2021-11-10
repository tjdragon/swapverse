const { expect } = require("chai");
const { ethers } = require("hardhat");

const hbar_token_supply = 1000000;
const link_token_supply = 1000000;


describe("SwapVerse Trades", function () {
  let _swap_verse;

  before(async () => {
    const TJToken = await ethers.getContractFactory("TJToken");
    const _hbar_token = await TJToken.deploy(hbar_token_supply, "Hbar", "HBAR");
    await _hbar_token.deployed();

    const _link_token = await TJToken.deploy(link_token_supply, "USDt", "USDT");
    await _link_token.deployed();

    const SwapVerse = await ethers.getContractFactory("SwapVerse");
    _swap_verse = await SwapVerse.deploy(_hbar_token.address, _link_token.address);
    await _swap_verse.deployed();
  });

  it("Trades Test", async function () {
    const nb_trades = await _swap_verse.get_trade_size();
    console.log("nb_trades ", nb_trades);
  });
});