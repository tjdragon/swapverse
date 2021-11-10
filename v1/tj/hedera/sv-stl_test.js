const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("Settlement Tests", function () {
  let _owner;
  let _addr1;
  let _addr2;

  let _stl;
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

    const Setl = await ethers.getContractFactory("Setl");
    _stl = await Setl.deploy(_link_token.address, _usdt_token.address);
    await _stl.deployed();
    console.log("Setl Contract address:", _stl.address);
  });

  it("Fund & Swap", async function () {
    await _link_token.transfer(_addr1.address, 150);
    expect(await _link_token.balanceOf(_addr1.address)).to.equal(150);

    await _usdt_token.transfer(_addr2.address, 230);
    expect(await _usdt_token.balanceOf(_addr2.address)).to.equal(230);

    await _link_token.connect(_addr1).approve(_stl.address, 100);
    await _usdt_token.connect(_addr2).approve(_stl.address, 200);

    // Sends 20 LINK from _addr1 to _addr2
    // Sends 30 USDT from _addr2 to _addr1
    await _stl.swap(_addr1.address, 20, _addr2.address, 30);

    expect(await _link_token.balanceOf(_addr1.address)).to.equal(130);
    expect(await _link_token.balanceOf(_addr2.address)).to.equal(20);

    expect(await _usdt_token.balanceOf(_addr2.address)).to.equal(200);
    expect(await _usdt_token.balanceOf(_addr1.address)).to.equal(30);
  });
});
