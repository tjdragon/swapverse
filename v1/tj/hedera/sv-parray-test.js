const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("SwapVerse Price Array Tests", function () {
  let _swap_verse;

  let _link_token;
  let _usdt_token;

  before(async () => {
    const TJToken = await ethers.getContractFactory("TJToken");
    _link_token = await TJToken.deploy(2000, "Link", "LINK");
    await _link_token.deployed();
    console.log("LINK address:", _link_token.address);

    _usdt_token = await TJToken.deploy(1000, "USD", "USDT");
    await _usdt_token.deployed();
    console.log("USDT address:", _usdt_token.address);

    const SwapVerse = await ethers.getContractFactory("SwapVerse");
    _swap_verse = await SwapVerse.deploy(_link_token.address, _usdt_token.address);
    await _swap_verse.deployed();
  });

  it("Add Buy Prices", async function () {
    await _swap_verse.test_add_price(5, true);
    let buy_prices = await _swap_verse.buy_prices();
    expect(buy_prices[0]).to.equal(5);

    await _swap_verse.test_add_price(7, true);
    buy_prices = await _swap_verse.buy_prices();
    expect(buy_prices[0]).to.equal(7);
    expect(buy_prices[1]).to.equal(5);

    await _swap_verse.test_add_price(4, true);
    buy_prices = await _swap_verse.buy_prices();
    expect(buy_prices[0]).to.equal(7);
    expect(buy_prices[1]).to.equal(5);
    expect(buy_prices[2]).to.equal(4);
  });

  it("Add Sell Prices", async function () {
    await _swap_verse.test_add_price(5, false);
    let sell_prices = await _swap_verse.sell_prices();
    expect(sell_prices[0]).to.equal(5);

    await _swap_verse.test_add_price(7, false);
    sell_prices = await _swap_verse.sell_prices();
    expect(sell_prices[0]).to.equal(5);
    expect(sell_prices[1]).to.equal(7);

    await _swap_verse.test_add_price(4, false);
    sell_prices = await _swap_verse.sell_prices();
    expect(sell_prices[0]).to.equal(4);
    expect(sell_prices[1]).to.equal(5);
    expect(sell_prices[2]).to.equal(7);
  });
});
