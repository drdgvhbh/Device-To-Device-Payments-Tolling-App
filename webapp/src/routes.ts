// @material-ui/icons
import AttachMoney from '@material-ui/icons/AttachMoney';
import DirectionsCar from '@material-ui/icons/DirectionsCar';
import Toll from '@material-ui/icons/Toll';

import { FaucetDashboard } from '@/views/Faucet';
import { TollingBooth } from '@/views/TollingBooth';
import { Vehicle } from '@/views/Vehicle';

const dashboardRoutes = [
  {
    path: '/vehicles',
    name: 'Vehicles',
    icon: DirectionsCar,
    component: Vehicle,
    layout: '/admin',
  },
  {
    path: '/tolling-booths',
    name: 'Tolling Booths',
    icon: Toll,
    component: TollingBooth,
    layout: '/admin',
  },
  {
    path: '/faucet',
    name: 'Faucet',
    icon: AttachMoney,
    component: FaucetDashboard,
    layout: '/admin',
  },
];

export default dashboardRoutes;
