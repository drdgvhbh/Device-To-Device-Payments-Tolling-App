require("dotenv").config();
const CodegenERC20 = artifacts.require("CodegenERC20");

module.exports = function(deployer) {
  deployer.deploy(CodegenERC20);
};
