import { Vehicle as VehicleContract } from '@/contracts/Vehicle';
import { Web3Context } from '@/index';
import {
  Card,
  createStyles,
  List,
  ListItem,
  ListItemText,
  Theme,
  Typography,
  withStyles,
  WithStyles,
} from '@material-ui/core';
import axios from 'axios';
import React from 'react';
import LoadingOverlay from 'react-loading-overlay';
import * as Survey from 'survey-react';
import Swal from 'sweetalert2';

import { abi as vehicleABI, networks } from '@/abis/Vehicle.json';
import Web3 from 'web3';
const vehicleAddress = networks[15].address;

const json = {
  questions: [
    {
      type: 'text',
      name: 'licencePlate',
      isRequired: true,
      title: 'Car Licence Plate',
    },
    {
      type: 'checkbox',
      name: 'carManufacturer',
      title: 'Car Manufacturer',
      isRequired: true,
      colCount: 2,
      choices: ['Tesla', 'Tesla', 'Tesla', 'Tesla', 'Tesla', 'Tesla'],
    },
    {
      type: 'checkbox',
      name: 'carModel',
      title: 'Model of the Car',
      isRequired: true,
      colCount: 2,
      choices: ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'],
    },
    {
      type: 'text',
      name: 'carYear',
      isRequired: true,
      title: 'Car Manufacture Year',
    },
  ],
};

const styles = (theme: Theme) =>
  createStyles({
    container: {
      display: 'flex',
      [theme.breakpoints.down('md')]: {
        flexDirection: 'column',
      },
    },
    card: {
      padding: '1rem',
      margin: '1rem',
      flex: 1,
    },
  });

const model = new Survey.ReactSurveyModel(json);

interface ContextProps {
  web3: Web3;
}

interface Props extends WithStyles<typeof styles> {}

interface State {
  licencePlate: string;
  carManufacturer: string;
  carModel: string;
  carYear: string;
  isLoading: boolean;
  vehicleData: VehicleData[];
  [key: string]: any;
}

interface VehicleData {
  id: string;
  plateNumber: string;
  model: string;
}

class VehicleDashboard extends React.Component<Props & ContextProps, State> {
  constructor(props) {
    super(props);

    this.state = {
      licencePlate: '',
      carManufacturer: '',
      carModel: '',
      carYear: '',
      vehicleData: [],
      isLoading: false,
    };
  }

  public componentDidMount() {
    this.fetchRegisteredVehicles();
  }

  public render() {
    const { classes, web3 } = this.props;
    const { isLoading, vehicleData } = this.state;

    return (
      <div>
        <p>
          This page is where you register a vehicle and where you can see all
          your registered vehicles. The purpose for this is to fulfill the
          following requirement.
        </p>
        <p>
          <i>
            The application will <b>establish a device (car) identity</b> as
            well as a toll booth sensor identity.
          </i>
        </p>
        <p>
          When you fill out this form to register a vehicle, it is (in theory)
          sent to a government entity, who reviews the application, and issues
          you an{' '}
          <a href="https://github.com/ethereum/eips/issues/721">ERC-721</a>{' '}
          token for your vehicle, thus proving you own it.
        </p>
        <p>
          The form also requires your signature via Metamask, to ensure the
          registration is legitimately coming from you.
        </p>
        <div className={classes.container}>
          <LoadingOverlay
            spinner
            active={isLoading}
            text="Registering Vehicle..."
          >
            <Card className={classes.card}>
              <Typography variant="h6">Register a Vehicle</Typography>

              <Survey.Survey
                model={model}
                onValueChanged={(_v, opts) => {
                  const name:
                    | 'licencePlate'
                    | 'carManufacturer'
                    | 'carModel'
                    | 'carYear' = opts.name;
                  const value: string = opts.value;
                  this.setState({ [name]: value });
                }}
                onComplete={async () => {
                  const accounts = await web3.eth.getAccounts();
                  const {
                    licencePlate,
                    carManufacturer,
                    carModel,
                    carYear,
                  } = this.state;
                  const address = accounts[0];
                  const body = {
                    ownerAddress: address,
                    plateNumber: licencePlate,
                    model: `${carYear} ${carManufacturer} ${carModel}`,
                  };
                  const hash = web3.utils.sha3(JSON.stringify(body));
                  const signature = await web3.eth.sign(hash, address);
                  const requestBody = { ...body, signature };
                  this.setState({ isLoading: true });
                  try {
                    const result = await axios.post<{
                      TransactionHash: string;
                    }>(
                      process.env
                        .REACT_APP_VEHICLE_REGISTRATION_LOGIC_APP_URL!!,
                      requestBody,
                    );
                    await Swal.fire({
                      type: 'success',
                      title: 'Vehicle Registration Successful',
                      html: `Your vehicle registration has been sent for processing and upon review you will be granted a Vehicle <code>(VEH)</code> token confirming your ownership of the vehicle.<br/><br/> <b>Transaction Hash</b>: <code>${
                        result.data.TransactionHash
                      }</code>`,
                    });
                    this.fetchRegisteredVehicles();
                  } catch (err) {
                  } finally {
                    this.setState({ isLoading: false });
                  }
                }}
              />
            </Card>
          </LoadingOverlay>
          <Card className={classes.card}>
            <Typography variant="h6">Your Registered Vehicles</Typography>
            <List>
              {vehicleData.map((data) => (
                <ListItem key={data.id}>
                  <ListItemText
                    primary={data.id}
                    secondary={
                      <div>
                        Plate Number: {data.plateNumber}
                        <br />
                        Model: {data.model}
                      </div>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </Card>
        </div>
      </div>
    );
  }

  private async fetchRegisteredVehicles() {
    const vehicleContract = await this.getVehicleContract();

    const registeredVehicleIdentifiers = await vehicleContract.methods
      .getRegisteredVehicles()
      .call();

    const vehicleData = registeredVehicleIdentifiers.flatMap(
      async (id, idx, arr) => {
        const dataURL = await vehicleContract.methods.tokenURI(id).call();
        const data = await axios.get<{ plateNumber: string; model: string }>(
          dataURL,
        );

        return { ...data.data, id: `${id}` };
      },
    );

    const promisedVehicledData = await Promise.all(vehicleData);

    this.setState({ vehicleData: promisedVehicledData });
  }

  private async getVehicleContract(): Promise<VehicleContract> {
    const { web3 } = this.props;
    const accounts = await web3.eth.getAccounts();
    const vehicleContract = new web3.eth.Contract(vehicleABI, vehicleAddress, {
      from: accounts[0],
    }) as VehicleContract;

    return vehicleContract;
  }
}

const VehicleDashboardContexted = (props: Props) => (
  <Web3Context.Consumer>
    {(web3) => <VehicleDashboard {...props} web3={web3} />}
  </Web3Context.Consumer>
);

export default withStyles(styles)(VehicleDashboardContexted);
