pragma solidity >=0.4.21 <0.6.0;

import '../node_modules/openzeppelin-solidity/contracts/token/ERC20/ERC20.sol';
import '../node_modules/openzeppelin-solidity/contracts/token/ERC20/ERC20Detailed.sol';
import '../node_modules/openzeppelin-solidity/contracts/token/ERC20/ERC20Mintable.sol';
import '../node_modules/openzeppelin-solidity/contracts/token/ERC721/IERC721.sol';

contract TollToken is ERC20, ERC20Detailed, ERC20Mintable {
  IERC721 private vehicle;

  address private _owner;

  uint256 private _tollAmount;

  mapping (address => uint) private _tollingBooths;

  constructor(
    string memory name, 
    string memory symbol,
    uint8 decimals,
    address vehicleContractAddr,
    uint256 tollAmount
  ) ERC20Detailed(
    name, 
    symbol, 
    decimals
  ) public {
    vehicle = IERC721(vehicleContractAddr);
    _owner = msg.sender;
    _tollAmount = tollAmount;
  }

  modifier ownerOnly() {
    require(msg.sender == _owner);
    _;
  }

  function withdrawFromBooth(address tollingBooth, uint256 amount) public ownerOnly() {
    require(isTollingBooth(tollingBooth));

    _approve(tollingBooth, _owner, amount);
    transferFrom(tollingBooth, _owner, amount);
  }

  function registerTollingBooth(address tollingBooth) public ownerOnly() {
    _tollingBooths[tollingBooth] = 1;
  }

  function isTollingBooth(address tollingBooth) public view returns (bool) {
    return _tollingBooths[tollingBooth] != 0;
  }

  function tollAmount() public view returns (uint256) {
    return _tollAmount;
  }

  function setTollAmount(uint256 amount) public ownerOnly() {
    _tollAmount = amount;
  }

  function paytoll(
    uint256 amount,
    uint256 vehicleId,
    address vehicleOwner,
    address tollBooth
  ) public ownerOnly() returns (bool) {
    require(isTollingBooth(tollBooth));
    require(vehicle.ownerOf(vehicleId) == vehicleOwner);
    require(allowance(vehicleOwner, tollBooth) >= amount);

    _transfer(vehicleOwner, tollBooth, amount);
    _approve(vehicleOwner, msg.sender, allowance(vehicleOwner, tollBooth).sub(amount));

    return true;
  }
}