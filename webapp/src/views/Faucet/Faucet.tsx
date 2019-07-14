import { Web3Context } from '@/index';
import {
  Button,
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
import Swal from 'sweetalert2';
import Web3 from 'web3';

const styles = (theme: Theme) =>
  createStyles({
    container: {
      width: '100%',
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

interface ContextProps {
  web3: Web3;
}

interface Props extends WithStyles<typeof styles> {}

interface State {
  isLoading: boolean;
  [key: string]: any;
}

class FaucetDashboard extends React.Component<Props & ContextProps, State> {
  constructor(props) {
    super(props);

    this.state = {
      isLoading: false,
    };
  }

  public render() {
    const { classes, web3 } = this.props;
    const { isLoading } = this.state;

    return (
      <div>
        <p>
          This is just a faucet page for you to get funds from the private
          Ethereum network.
        </p>
        <p>
          It will grant you 10 <code>ETH</code> and 10 <code>TOL</code>.
        </p>
        <LoadingOverlay
          className={classes.container}
          spinner
          active={isLoading}
          text="Granting Funds..."
        >
          <Card className={classes.card}>
            <Typography variant="h6">Get Funds</Typography>
            <Button
              variant="contained"
              onClick={async () => {
                this.setState({ isLoading: true });
                try {
                  const accounts = await web3.eth.getAccounts();
                  const requestBody = {
                    ethAddress: accounts[0],
                  };
                  const result = await axios.post<{
                    TransactionHash: string;
                  }>(process.env.REACT_APP_GIMME_FUNDS_APP_URL!!, requestBody);
                  await Swal.fire({
                    type: 'success',
                    title: 'Funds sent to your account',
                    html: `10 ETH and 10 TOL have been deposited into your ethereum address`,
                  });
                } catch (err) {
                } finally {
                  this.setState({ isLoading: false });
                }
              }}
            >
              Gimme
            </Button>
          </Card>
        </LoadingOverlay>
      </div>
    );
  }
}

const FaucetDashboardContexted = (props: Props) => (
  <Web3Context.Consumer>
    {(web3) => <FaucetDashboard {...props} web3={web3} />}
  </Web3Context.Consumer>
);

export default withStyles(styles)(FaucetDashboardContexted);
