import { createBrowserHistory } from 'history';
import React from 'react';
import ReactDOM from 'react-dom';
import { Redirect, Route, Router, Switch } from 'react-router-dom';
import Web3 from 'web3';
// core components
import Admin from './layouts/Admin';
import RTL from './layouts/RTL';

import 'assets/css/material-dashboard-react.css?v=1.6.0';

const hist = createBrowserHistory();
const web3Provider =
  (window as any).ethereum || (window as any).web3.currentProvider;

const web3 = new Web3(web3Provider);

(async () => {
  if ((window as any).ethereum) {
    await web3Provider.enable();
  }
})();

export const Web3Context = React.createContext<typeof web3>(web3);

ReactDOM.render(
  <Web3Context.Provider value={web3}>
    <Router history={hist}>
      <Switch>
        <Route path="/admin" component={Admin} />
        <Route path="/rtl" component={RTL} />
        <Redirect from="/" to="/admin/vehicles" />
      </Switch>
    </Router>
  </Web3Context.Provider>,
  document.getElementById('root'),
);
