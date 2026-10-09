import type { CustomerTab } from '../types/navigation';
export const CUSTOMER_HOME = '/pages/customer/home/index';
export const CUSTOMER_TABS: readonly CustomerTab[] = [
  { key: 'home', text: '首页', pagePath: CUSTOMER_HOME, icon: '/assets/navigation/home-normal.svg', selectedIcon: '/assets/navigation/home-active.svg' },
  { key: 'rooms', text: '找房', pagePath: '/pages/customer/rooms/index', icon: '/assets/navigation/rooms-normal.svg', selectedIcon: '/assets/navigation/rooms-active.svg' },
  { key: 'orders', text: '订单', pagePath: '/pages/customer/orders/index', icon: '/assets/navigation/orders-normal.svg', selectedIcon: '/assets/navigation/orders-active.svg' },
  { key: 'mine', text: '我的', pagePath: '/pages/customer/mine/index', icon: '/assets/navigation/mine-normal.svg', selectedIcon: '/assets/navigation/mine-active.svg' },
];
