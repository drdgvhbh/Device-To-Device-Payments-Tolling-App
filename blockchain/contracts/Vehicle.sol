pragma solidity >=0.4.21 <0.6.0;

import '../node_modules/openzeppelin-solidity/contracts/drafts/Counters.sol';
import '../node_modules/openzeppelin-solidity/contracts/token/ERC721/ERC721Full.sol';
import '../node_modules/openzeppelin-solidity/contracts/token/ERC721/ERC721MetadataMintable.sol';

contract Vehicle is ERC721Full, ERC721MetadataMintable {
  using Counters for Counters.Counter;
  Counters.Counter private tokenCounter;

  address public issuer;

  constructor(
    string memory name,
    string memory symbol
  ) ERC721Full(name, symbol) public {
    issuer = msg.sender;
  }

  function registerVehicle(
    address vehicleOwner,
    string memory metadataURL
  ) public {
    require(msg.sender == issuer);
    uint256 tokenID = Counters.current(tokenCounter);

    mintWithTokenURI(vehicleOwner, tokenID, metadataURL);
    Counters.increment(tokenCounter);
  }

  function getRegisteredVehicles() public view returns (uint256[] memory) {
    return _tokensOfOwner(msg.sender);
  }
}