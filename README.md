# Device to Device Payments – Tolling Sample

This is a demo application for demostrating device-to-device payments using Ethereum Logic App connectors and NFC.

**It was submitted and chosen as the winner of the Microsoft [Device-To-Device Payments challenge](https://gitcoin.co/issue/Azure-Samples/bc-community-samples/18/2847) for 2000 [DAI](https://makerdao.com/en/dai/).** The PR can be found [here](https://github.com/Azure-Samples/bc-community-samples/issues/18).

It seeks to acheive the following.

- Create an application that will leverage a smart contract based in an Ethereum based blockchain, that will demonstrate how two devices can exchange value (e.g. payments).
- This is common scenario for decentralized payments. This application should simulate a toll payment for an automobile.
- The application will establish a device (car) identity as well as a toll booth sensor identity.
- The toll booth sensor will do a lookup in a registry of tokenized vehicles.
- Finally a smart contract that will demonstrate a car approaching the toll booth, the booth making a request for payment, and the payment from the car to the booth.

Concretely, this application demonstrates a user registering a vehicle and being issued an [ERC-721](https://github.com/ethereum/eips/issues/721) token for it by some entity. A tolling booth is also registered on a device, granting the entity explicit withdrawal rights of TOL token on that address. Using one device as a POS terminal and another as the phone of the vehicle owner/toll payer, the POS terminal will make a request for money (TOL token) to the vehicle owner via an NFC bump and a payment will be performed.

## Quick Video Demo

[![IMAGE ALT TEXT HERE](https://img.youtube.com/vi/cRkzETDQL8Q/0.jpg)](https://www.youtube.com/watch?v=cRkzETDQL8Q)

## Usage

### Prerequisites

- Two Android Phones with API version >= 23
- Metamask

### Steps

1. Download and install the [wallet](/apk/wallet.apk) and [POS Terminal](/apk/pos-terminal.apk) APKs on one phone.
2. Download and install the [wallet](/apk/wallet.apk) and [Vehicle App](/apk/vehicle-app.apk) APKs on the other phone.
3. Open the wallet application on the <i>Vehicle App</i> phone and copy the mnemonic.
4. To find the private key, navigate to this [website] and paste the mnemonic in the <b>BIP39 Mnemonic</b> field and set the <b>Coin</b> field to Ethereum
5. Scroll down to <b>Derived Addresses</b> and take the first private key in the list.
6. ~~Open your metamask extension and connect to the private network via the RPC url: http://40.77.57.190:11111~~ **SERVER NO LONGER MAINTAINED.**
7. Using the derived private key, import it and set the imported account as the default one
8. ~~Navigate to the [Tolling App Dashboard](https://tollingapp.azurewebsites.net) and register your vehicle. You can just input anything.~~ **SERVER NO LONGER MAINTAINED.**
9. Wait a couple of moments and refresh the page, the vehicle you just registered should be in the list.
10. On your Vehicle App phone, copy the public address from your Wallet app and paste it into the Vehicle App. The vehicle you registered should appear. Click on it, it will now setup the device as ready for payment.
11. Go back to the dashboard and click on Faucet in the sidebar. Click "Gimme" to get some funds you can use for payment (TOL) and for gas (ETH).
12. Check your wallet in your Vehicle App phone to make sure the funds have arrived.
13. Open the wallet on your POS terminal phone and copy the Ethereum address.
14. Copy the address and click on "Tolling Booths" on the sidebar in the dashboard. Paste the address to register the tolling booth.
15. Open the POS Terminal app and paste the tolling booth address
16. Perform an NFC bump between the two phones. A request for payment (ERC-20 allowance) should appear on the Vehicle App wallet.
17. Accept the request for payment.
18. Wait.
19. Check the wallets on both phones to see that the funds have been successfully transferred.

### Development

To get your own localized build of this application you will need to peform a grea tnumber of steps! Be prepared!

#### Create an Private Blockchain Network on an Azure VM

1. Create an Azure Virtual Machine. For reference, I used a B2s instance.
2. In the Network Security Group, open up port 22.
3. SSH into the instance
4. Setup the network as described [here](https://github.com/caleteeter/smartcontractdev/blob/master/example1-setup.md)
5. Install Node and extract the private key

```
sudo apt-get install curl python-software-properties
curl -sL https://deb.nodesource.com/setup_11.x | sudo -E bash -
npm init --y
npm install keythereum
nano key.js

### PASTE THE FOLLOWING ###
const keythereum = require('keythereum');

var keyObject = keythereum.importFromFile('YOUR_NODE_1_PUBLIC_ADDRESS', 'node1');
var privateKey = keythereum.recover('YOUR_NODE_1_PASSWORD', keyObject);
console.log(privateKey.toString('hex'));
###########################################

node ./key.js
```

6. Copy the private key and save it for later. This is the master private key you will use on deploying contracts and usage in logic apps
7. Expose the RPC port of node1 in your network security manager

### Deploy contracts using Truffle

1. `cd` into the blockchain directory
2. copy the `.env.example` file as `.env` and input the respective fields. `ETHEREUM_ADDRESS` is your node1 address
3. `npm install`
4. `truffle migrate --network private`
5. Keep note of the deployed addresses display in the terminal. You'll need them later. They're also stored in the jsons produced by truffle

### Setup a file storage

1. Create a `Storage account` in Azure
2. Setup a `Blob` container

#### Setup your Logic Apps

##### Vehicle Registration

1. Create a logic app for vehicle registration, choose the Request-Response template
2. Paste this in the "Request Body for JSON Schema" Field

```json
{
  "properties": {
    "model": {
      "type": "string"
    },
    "ownerAddress": {
      "type": "string"
    },
    "plateNumber": {
      "type": "string"
    },
    "signature": {
      "type": "string"
    }
  },
  "type": "object"
}
```

3. Add a "Create Blob" step
4. Do this

![Blob](https://i.imgur.com/Uwsfbms.png)

5. Add an Ethereum step - Using Gas
6. In `${ROOT_DIR}/blockchain/build/Contracts/Vehicle.json`, take the ABI, minifiy it, and paste it
7. Paste the smart contract address
8. Do this

![Params](https://i.imgur.com/mO6N4oB.png)

9. Save
10. Copy the URL in the first step
11. In the `${ROOT_DIR}/webapp` folder, set this url to be the `REACT_APP_VEHICLE_REGISTRATION_LOGIC_APP_URL` in the `.env` file (copy the `.env.example` file to create it)

##### Tolling Booth Registration

1. Create a logic app for tolling booth registration, choose the Request-Response template
2. Paste this in the "Request Body for JSON Schema" Field

```json
{
  "properties": {
    "ethAddress": {
      "type": "string"
    }
  },
  "type": "object"
}
```

3. Add an Ethereum step - Using Gas
4. In `${ROOT_DIR}/blockchain/build/Contracts/TollToken.json`, take the ABI, minifiy it, and paste it
5. Do this

![Img](https://i.imgur.com/q57oydu.png)

6. Save
7. Copy the URL in the first step
8. In the `${ROOT_DIR}/webapp` folder, set this url to be the `REACT_APP_TOLLING_BOOTH_REGISTRATION_LOGIC_APP_URL` in the `.env` file (copy the `.env.example` file to create it)

##### Faucet Provider

1. Create a logic app for providing funds, choose the Request-Response template
2. Paste this in the "Request Body for JSON Schema" Field

```json
{
  "properties": {
    "ethAddress": {
      "type": "string"
    }
  },
  "type": "object"
}
```

3. Add an Ethereum step - Using Gas
4. In `${ROOT_DIR}/blockchain/build/Contracts/SendMoney.json`, take the ABI, minifiy it, and paste it
5. Do this

![Img](https://i.imgur.com/vvxWIPG.png)

6. Add an Ethereum step - Using Gas
7. In `${ROOT_DIR}/blockchain/build/Contracts/TollToken.json`, take the ABI, minifiy it, and paste it
8. Do this

![img](https://i.imgur.com/PiPikGB.png)

9. Save
10. Copy the URL in the first step
11. In the `${ROOT_DIR}/webapp` folder, set this url to be the `REACT_APP_GIMME_FUNDS_APP_URL` in the `.env` file (copy the `.env.example` file to create it)

##### Payment Handler

1. Create a logic app for handling payments, choose the Request-Response template
2. Paste this in the "Request Body for JSON Schema" Field

```json
{
  "properties": {
    "amount": {
      "type": "string"
    },
    "tollBooth": {
      "type": "string"
    },
    "vehicleId": {
      "type": "string"
    },
    "vehicleOwner": {
      "type": "string"
    }
  },
  "type": "object"
}
```

3. Add an Ethereum step - Using Gas
4. In `${ROOT_DIR}/blockchain/build/Contracts/TollToken.json`, take the ABI, minifiy it, and paste it
5. Do this

![img](https://i.imgur.com/FnacAA6.png)

9. Save
10. Store this URL, it will be used in the Android apps

### Setup Android App

1. Open the `posterminal`, `vehicleapp`, `wallet` directories separately in Android Studio
2. Deal with the 10000 gradle/missing SDK/Other random errors
3. In each project there is a `config_example.properties` file in `res/raw/`. Copy it as `config_example`
4. For `TOL_TOKEN_ADDRESS` input the smart contract address
5. For `PAYMENT_LOGIC_APP_URL` use the `Payment Handler` logic app URL

### Generate Contract Android Files

1. Install the web3j [cli](https://github.com/web3j/web3j)
2. Using a bash terminal, run the `./build_contracts.sh` script
3. Now you can run the Android applications on the phone

### Setup the webapp

1. cd into the webapp
2. Install the Typechain [cli](https://github.com/ethereum-ts/TypeChain) globally
3. `npm install`
4. `npm run start`
