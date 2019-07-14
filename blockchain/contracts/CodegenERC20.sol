pragma solidity >=0.4.21 <0.6.0;
import '../node_modules/openzeppelin-solidity/contracts/token/ERC20/ERC20Detailed.sol';
import '../node_modules/openzeppelin-solidity/contracts/token/ERC20/ERC20.sol';

// Placeholder contract so the json gets created by truffle migrate
contract CodegenERC20 is ERC20, ERC20Detailed {
  constructor() public ERC20Detailed("idc", "idc", 18) {}
}