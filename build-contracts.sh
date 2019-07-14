#!/usr/bin/env bash

for file in ./blockchain/build/contracts/*.json; do
  web3j truffle generate $file -o ./vehicleapp/app/build/generated/source/buildConfig/debug -p ca.drdgvhbh.vehicleapp.contracts
  web3j truffle generate $file -o ./posterminal/app/build/generated/source/buildConfig/debug -p ca.drdgvhbh.posterminal.contracts
  web3j truffle generate $file -o ./wallet/app/build/generated/source/buildConfig/debug -p org.drdgvhbh.android.wallet.contracts
done