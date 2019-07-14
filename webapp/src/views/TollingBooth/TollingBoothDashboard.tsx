import { Web3Context } from '@/index';
import {
  Card,
  createStyles,
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

import Web3 from 'web3';

const json = {
  questions: [
    {
      type: 'text',
      name: 'tollingBoothAddress',
      isRequired: true,
      title: 'Tolling Booth Address',
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
  tollingBoothAddress: string;
  isLoading: boolean;
  [key: string]: any;
}

class TollingBoothDashboard extends React.Component<
  Props & ContextProps,
  State
> {
  constructor(props) {
    super(props);

    this.state = {
      tollingBoothAddress: '',
      isLoading: false,
    };
  }

  public render() {
    const { classes, web3 } = this.props;
    const { isLoading } = this.state;

    return (
      <div>
        <p>
          This is the page where you can register tolling booth addresses. The
          purpose for this is to fulfill the following requirement.
        </p>
        <p>
          <i>
            The application will establish a device (car) identity as well as a{' '}
            <b>toll booth sensor identity</b>.
          </i>
        </p>
        <p>
          The idea here is the entity setting up these toll booths would use
          this form specify certain ethereum addresses as tolling booths. By
          doing this, this allows the entity to freely withdraw toll{' '}
          <code>(TOL)</code>
          tokens from the tolling booth back into their own accounts. In
          addition, the payment of the toll can only be sent to a registered
          tolling booth address.
        </p>
        <p>
          When the form is submitted, it will request a signature to verify that
          you are owner of these toll booths. However, for demostration
          purposes, there is no validation check against the signature,
          otherwise you would not be able to execute this demo on your phones
          (you don't have the owner private key).
        </p>
        <div className={classes.container}>
          <LoadingOverlay
            spinner
            active={isLoading}
            text="Registering Tolling Booth..."
          >
            <Card className={classes.card}>
              <Typography variant="h6">Register a Tolling Booth</Typography>

              <Survey.Survey
                model={model}
                onValueChanged={(_v, opts) => {
                  const name: 'tollingBoothAddress' = opts.name;
                  const value: string = opts.value;
                  this.setState({ [name]: value });
                }}
                onComplete={async () => {
                  const accounts = await web3.eth.getAccounts();
                  const { tollingBoothAddress } = this.state;
                  const address = accounts[0];
                  const body = {
                    ethAddress: tollingBoothAddress,
                  };
                  const hash = web3.utils.sha3(JSON.stringify(body));
                  const signature = await web3.eth.sign(hash, address);
                  const requestBody = { ...body, signature };
                  this.setState({ isLoading: true });
                  try {
                    const result = await axios.post<{
                      TransactionHash: string;
                    }>(
                      process.env.REACT_APP_TOLLING_BOOTH_REGISTRATION_LOGIC_APP_URL!!,
                      requestBody,
                    );
                    await Swal.fire({
                      type: 'success',
                      title: 'Toll Booth Registration Successful',
                      html: `Your toll booth registration has been sent for processing. <br/><br/> <b>Transaction Hash</b>: <code>${
                        result.data.TransactionHash
                      }</code>`,
                    });
                  } catch (err) {
                  } finally {
                    this.setState({ isLoading: false });
                  }
                }}
              />
            </Card>
          </LoadingOverlay>
        </div>
      </div>
    );
  }
}

const TollingBoothDashboardContexted = (props: Props) => (
  <Web3Context.Consumer>
    {(web3) => <TollingBoothDashboard {...props} web3={web3} />}
  </Web3Context.Consumer>
);

export default withStyles(styles)(TollingBoothDashboardContexted);
