const Vehicle = artifacts.require("Vehicle");
const TollToken = artifacts.require("TollToken");

const Web3 = require("web3");

const web3 = new Web3(Web3.providers.HttpProvider(""));

module.exports = function(deployer) {
  deployer.deploy(Vehicle, "Vehicle", "VEH").then(() => {
    return deployer.deploy(
      TollToken,
      "Toll",
      "TOL",
      18,
      Vehicle.address,
      // Ether also uses 18 decimals so this should work
      web3.utils.toWei("1", "ether")
    );
  });
};
