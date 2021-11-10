const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("SwapVerse Limit Orders Tests A", function () {
  let _owner;
  let _addr1;
  let _addr2;
  let _swap_verse;
  let _tj_token;

  let _link_token;
  let _usdt_token;

  before(async () => {
    const [owner, addr1, addr2] = await ethers.getSigners();
    _owner = owner;
    _addr1 = addr1;
    _addr2 = addr2;

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
    console.log("Contract address:", _swap_verse.address);

    await _swap_verse.add_participant(_addr1.address);
    await _swap_verse.add_participant(_addr2.address);

    _tj_token = await TJToken.deploy(1000, "TjToken", "TJT");
    await _tj_token.deployed();

    console.log("TJTOK address:", _tj_token.address);
  });

  it("Send ETH To Contract A", async function () {
    console.log("From address:", _addr1.address);
    await expect(_addr1.sendTransaction({
      to: _swap_verse.address,
      value: ethers.utils.parseEther("1.0"), // Sends exactly 1.0 ether
    })).to.emit(_swap_verse, 'AssetReceived').
      withArgs(_addr1.address, ethers.BigNumber.from("1000000000000000000"));
  });

  // https://www.youtube.com/watch?v=9tYkS7YyOjU
  // https://ethereum.org/en/developers/tutorials/transfers-and-approval-of-erc-20-tokens-from-a-solidity-smart-contract/
  // https://medium.com/coinmonks/create-an-erc20-token-payment-splitting-smart-contract-c79436470ccc
  it("Send TJTOK To Contract A", async function () {
    const ownerBalance = await _tj_token.balanceOf(_owner.address);
    expect(await _tj_token.totalSupply()).to.equal(ownerBalance);

    // From TJTOK owner sends 50 to ADDR1
    await _tj_token.transfer(_addr1.address, 50);
    expect(await _tj_token.balanceOf(_addr1.address)).to.equal(50);
    expect(await _tj_token.balanceOf(_owner.address)).to.equal(950);

    // ADDR1 to send 20 tokens to the smart contract
    await _tj_token.connect(_addr1).transfer(_swap_verse.address, 20);
    expect(await _tj_token.balanceOf(_addr1.address)).to.equal(30);
    expect(await _tj_token.balanceOf(_swap_verse.address)).to.equal(20);
  });
});
