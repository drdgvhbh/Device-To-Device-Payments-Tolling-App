require("dotenv").config();
const SendMoney = artifacts.require("SendMoney");

const Web3 = require("web3");

const web3 = new Web3(Web3.providers.HttpProvider(""));

const etherAddress = process.env.ETHEREUM_ADDRESS;

module.exports = function(deployer) {
  deployer.deploy(SendMoney, {
    from: etherAddress,
    value: web3.utils.toWei("100000", "ether")
  });
};
