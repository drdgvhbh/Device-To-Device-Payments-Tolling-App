pragma solidity >=0.4.21 <0.6.0;

contract SendMoney {
  address public issuer;

  constructor() public payable {
    issuer = msg.sender;
  }

  function issueFunds(address payable to) public {
    to.transfer((10**18) * 10);
  }
}