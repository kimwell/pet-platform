import { CUSTOMER_HOME } from '../../../constants/navigation';
Page({ onLoad() { wx.switchTab({ url: CUSTOMER_HOME, fail: () => wx.reLaunch({ url: CUSTOMER_HOME }) }); } });
